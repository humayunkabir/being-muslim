# Appwrite backend configuration

**Status:** Provisioned 2026-10-20 against the self-hosted Appwrite instance that
is now the production backend (`DECISIONS/0008`).
**Owns:** the non-secret backend identifiers, platform registrations, OAuth
provider state, database/table schema and permission model, the verification
evidence for issue #22, and the operational steps for the administrative key.
**Defers:** what may be synced and retained → `PRODUCT_REQUIREMENTS.md` §10;
account and sync behavior → `DECISIONS/0003`, issues #15–#17, and the sync
contract in `SYNC.md`; secret storage and rotation policy → `ARCHITECTURE.md`
§7.

## Non-secret identifiers

| Setting | Value |
|---|---|
| Endpoint | `https://appwrite.alveo.dev/v1` |
| Project ID | `being-muslim` |
| Server API version | `2.5.0` (`GET /v1/health/version`) |
| Database ID | `being_muslim` |
| Table IDs | `user_sync` (account settings and tasbih total); `problem_reports` (user-initiated problem reports) |
| Android SDK | `io.appwrite:sdk-for-android:31.0.0` — the SDK line that targets server 2.5.x |
| Verification SDK | `appwrite` (web client) `29.1.0`, used only for the call below |

These are not secrets. The Android app reads the endpoint and project ID from
Gradle `BuildConfig` fields (`app/build.gradle.kts`), overridable with
`-PappwriteEndpoint=...` and `-PappwriteProjectId=...`; no key or OAuth secret
is in the APK. The sync row-creation function id is `APPWRITE_SYNC_FUNCTION_ID`
(default `user-sync`), overridable with `-PappwriteSyncFunctionId=...`; the
account-deletion function id is `APPWRITE_ACCOUNT_DELETE_FUNCTION_ID` (default
`account-delete`), overridable with `-PappwriteAccountDeleteFunctionId=...`; the
problem-report function id is `APPWRITE_REPORT_FUNCTION_ID` (default
`report-problem`), overridable with `-PappwriteReportFunctionId=...`.

**Licensing:** the `io.appwrite:sdk-for-android` Maven POM declares GPL-3.0, but
the upstream repository's `LICENSE.md` is **BSD-3-Clause** (verified
2026-10-21). Treat it as BSD-3-Clause and include its notice in the app's
third-party notices before release; the POM metadata is wrong.

## Database and permissions

`being_muslim.user_sync` holds one row per signed-in user:

| Column | Type | Notes |
|---|---|---|
| `settings` | varchar(8192) | Opaque JSON object of synced settings; nullable until first sync. Its shape, versioning, and conflict rule are the sync contract (`SYNC.md`). |
| `tasbihTotal` | integer, min 0, default 0 | The tasbih total only, not the label or target (`SYNC.md`). |

- Row security is **on**; the table grants **no** table-level permissions.
- The row ID is the Appwrite user ID. The creating server path sets
  `read("user:<userId>")`, `update("user:<userId>")`,
  `delete("user:<userId>")`; the owner can then read and update that row, and
  nobody else can.
- Clients **cannot create rows**. This is deliberate: with client create
  granted, a user who knew another user's ID could pre-create that row with
  public read. That was verified — the victim read the attacker's payload and
  could not overwrite it. Rows must be created by a trusted server path using
  the administrative key.
- No client API key is embedded in either app.

**Verified 2026-10-20** with two throwaway users, deleted afterwards:

| Action | Result |
|---|---|
| Server creates A's row with A's permissions | `201` |
| User A reads own row | `200` |
| User A updates own row | `200` |
| User A attempts to create a row | `401`, no create permission |
| User B reads A's row | `404 row_not_found` |
| User B lists rows | `200`, total `0` |
| User B updates or deletes A's row | `401 user_unauthorized` |

Rows are created by the `user-sync` function described below, never by a
client. That gap is intentional and safer than shipping client create.

## Sync row creation function (#16)

`user-sync` creates the signed-in caller's row through the trusted server path.
Clients have no create permission, so this is the only way a row comes into
existence.

| Setting | Value |
|---|---|
| Function ID | `user-sync` |
| Runtime | `node-22` |
| Entrypoint | `index.js` |
| Build command | `npm install` (no dependencies) |
| Scopes | `rows.read`, `rows.write` |
| Execute permission | `any` — see the finding below |
| Timeout | 15 s |
| Source | `backend/functions/user-sync/` |

The function:

1. Reads `x-appwrite-user-id` from the execution. A call without a session
   returns `401 {"message":"Sign in required"}` and creates nothing.
2. Reads the caller's row. If it exists, returns `200 {"created":false}`.
3. Otherwise creates row id = the caller's user id with
   `read("user:<id>")`, `update("user:<id>")`, `delete("user:<id>")`, and
   returns `201`/`200 {"created":true}`. A concurrent `409` is treated as
   created.

The app calls it only when a row read returns `404`, then retries the write
(`SYNC.md`, *Errors and retries*). It never sends settings or the tasbih total
to the function.

**Finding — `execute: ["users"]` is rejected by this server.** Creating the
function with that role returns
`401 Missing "execute" permission for role "users". Only ["any","guests"] scopes are allowed`.
The execute permission is therefore `["any"]`; authentication is enforced
inside the function from the session user id, and no row can be created for
another user because the id is never read from the request body. Rate limiting
is the platform's per-project limit.

**Deployment** (from the repository root; requires the administrative key from
`.env.appwrite`, never committed):

```sh
set -a; . ./.env.appwrite; set +a
base="${APPWRITE_ENDPOINT%/}"

# Once, if the function does not exist yet.
curl -X POST "$base/functions" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"functionId":"user-sync","name":"User sync row","runtime":"node-22",
       "execute":["any"],"scopes":["rows.read","rows.write"],
       "entrypoint":"index.js","commands":"npm install","enabled":true,
       "logging":true,"timeout":15}'

# For each code change.
tar -czf /tmp/user-sync.tar.gz -C backend/functions/user-sync .
curl -X POST "$base/functions/user-sync/deployments" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -F "entrypoint=index.js" -F "commands=npm install" -F "activate=true" \
  -F "code=@/tmp/user-sync.tar.gz;type=application/gzip"
```

Poll `GET /v1/functions/user-sync/deployments/<id>` until `status` is `ready`;
the function is live once the activated deployment is ready.

**Verified 2026-10-21** with a throwaway email/password user, then deleted:

| Action | Result |
|---|---|
| User executes the function with a session | `200 {"created":true}` |
| Row as admin | id = user id; `tasbihTotal 0`, `settings null`; permissions are the three `user:<id>` grants |
| User executes it again | `200 {"created":false}` |
| User updates settings + total on the row | `200`, values stored |
| User reads the row | `200`, values returned |
| Execution without a session | `401 {"message":"Sign in required"}`, no row |
| Execution without a session but with a forged `x-appwrite-user-id` header | `401`, no row; the executor drops the client-supplied header |
| User and row deleted afterwards | `204`, `204`; table back to 0 rows |

**Two-session handover, same user** (two independent authenticated sessions, so
two server clients), verified 2026-10-21 and cleaned up:

| Action | Result |
|---|---|
| A writes method KARACHI, total 10; B reads | `10`, KARACHI |
| B writes method UMM_AL_QURA, total 20; A reads | `20`, UMM_AL_QURA |
| A writes total 30, then B writes total 40; A reads | `40` — the later write wins |

The Android client path is compiled and its payload/conflict tests pass. The
handover has not been exercised on two physical devices; the pair of sessions
above verifies the shared row and the last-write-wins ordering the app relies
on.

## Account deletion function (#17)

`account-delete` removes the signed-in caller's synced row and then the identity.
The Android SDK has no account-deletion call, and the client must not hold the
administrative key, so this runs on the trusted server path.

| Setting | Value |
|---|---|
| Function ID | `account-delete` |
| Runtime | `node-22` |
| Entrypoint | `index.js` |
| Build command | `npm install` (no dependencies) |
| Scopes | `rows.read`, `rows.write`, `users.read`, `users.write` |
| Execute permission | `any` — authentication is enforced from the session, as below |
| Timeout | 15 s |
| Source | `backend/functions/account-delete/` |

The function:

1. Reads `x-appwrite-user-id` from the execution. A call without a session
   returns `401 {"message":"Sign in required"}` and deletes nothing.
2. Deletes `being_muslim.user_sync` row `<userId>`. A `404` means it is already
   gone and is treated as done. Any other failure returns `502` and stops, so
   the identity stays usable and the request can be retried.
3. Deletes Appwrite user `<userId>`. A `404` is treated as done. Any other
   failure returns `502`; the row is already gone, and a retry finishes the
   deletion.
4. Deletes the row once more, narrowing the window where a sync write already in
   flight could recreate it between steps 2 and 3. Now that the session is
   dead, no new write can succeed, but a request authorized before step 3 can
   still land afterwards. A failure here returns `200
   {"deleted":true,"cleanedUp":false}` and logs the leftover row for operator
   cleanup; the app treats the account as deleted because the identity is gone
   and cannot be retried.

The order is deliberate: a failure before the identity is deleted leaves the
account usable and the request retryable, while the final row delete narrows
the window in which a write can race the deletion. Deleting the identity ends
all of that user's sessions. The app clears only its signed-in marker; local
device data is untouched (`SYNC.md`, `PRIVACY.md`).

**Deployment** (from the repository root; same administrative key as above):

```sh
set -a; . ./.env.appwrite; set +a
base="${APPWRITE_ENDPOINT%/}"

# Once, if the function does not exist yet.
curl -X POST "$base/functions" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"functionId":"account-delete","name":"Account deletion","runtime":"node-22",
       "execute":["any"],"scopes":["rows.read","rows.write","users.read","users.write"],
       "entrypoint":"index.js","commands":"npm install","enabled":true,
       "logging":true,"timeout":15}'

# For each code change.
tar -czf /tmp/account-delete.tar.gz -C backend/functions/account-delete index.js package.json
curl -X POST "$base/functions/account-delete/deployments" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -F "entrypoint=index.js" -F "commands=npm install" -F "activate=true" \
  -F "code=@/tmp/account-delete.tar.gz;type=application/gzip"
```

The function's contract, including the row-then-identity order and `404`
handling, is covered by `node --test` in
`backend/functions/account-delete/`.

**Verified 2026-10-21** with a throwaway email/password user, then deleted:

| Action | Result |
|---|---|
| Create user and sign in | `201`, `201` |
| Create the row through `user-sync`, write `tasbihTotal` 17 | `200`, `200` |
| User executes `account-delete` | execution `completed`, response `200 {"deleted":true}` |
| Row as admin | `404 row_not_found` |
| User as admin | `404 user_not_found` |
| Execution without a session | response `401 {"message":"Sign in required"}` |
| The deleted user's old session (`GET /account`) | `401 general_unauthorized_scope` |

## Problem report function (#18)

A problem report records the inputs that reproduce a wrong time so it can be
investigated (`docs/PRODUCT_REQUIREMENTS.md` §2.1, §5). Reporting needs no
account — every feature is usable signed out — so the function executes for
any caller and stores the report with no read permission for anyone.

`being_muslim.problem_reports` holds one row per report:

| Column | Type | Notes |
|---|---|---|
| `payload` | varchar(16384) | The validated report as JSON: calculation inputs, dates and times, and the user's note. No account, device, or location history. |

- Row security is **on**; the table grants **no** table-level permissions.
- Created rows carry `permissions: []`, so only the administrative key can read
  them. There is no client read path, and no report is tied to a user id — a
  guest and a signed-in caller are stored alike.
- The function validates an allowlist before storing anything. Unknown keys at
  the top level or inside the city, parameters, adjustments, offsets or day
  objects are rejected, as are out-of-range values, a note over 2000 characters,
  and a request body over 16 KiB. The input schema is the `isReport` contract in
  `backend/functions/report-problem/index.js`, mirrored by
  `ProblemReportCodec` in the app and covered by tests on both sides.
- The function never logs the report or the note; a storage failure logs only
  the HTTP status.

| Setting | Value |
|---|---|
| Function ID | `report-problem` |
| Runtime | `node-22` |
| Entrypoint | `index.js` |
| Build command | `npm install` (no dependencies) |
| Scopes | `rows.write` |
| Execute permission | `any` — reporting must work without an account |
| Timeout | 15 s |
| Source | `backend/functions/report-problem/` |

Responses: `201 {"sent":true}` when stored; `400 {"message":"Invalid report"}`
for a body outside the allowlist; `413` for a body over the limit; `502
{"message":"Report not sent"}` when the row cannot be created, so the app keeps
the report on screen and offers a retry. The app sends the report only when the
user taps **Send report**; being offline is not an error it hides — the screen
says a connection is needed and keeps the report for a retry.

Because reporting works signed out, this is an unauthenticated write path by
necessity. Abuse is bounded by the platform's per-project rate limit and the
16 KiB body and 2,000-character note caps; the rows hold no identifier to block
or delete by, so the operator prunes test or junk rows as they arrive.

**Deployment** (from the repository root; same administrative key as above):

```sh
set -a; . ./.env.appwrite; set +a
base="${APPWRITE_ENDPOINT%/}"

# Once: the table, its one column, and the function.
curl -X POST "$base/tablesdb/being_muslim/tables" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"tableId":"problem_reports","name":"Problem reports",
       "permissions":[],"rowSecurity":true,"enabled":true}'
curl -X POST "$base/tablesdb/being_muslim/tables/problem_reports/columns/string" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"key":"payload","required":true,"size":16384,"array":false}'
curl -X POST "$base/functions" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"functionId":"report-problem","name":"Problem report","runtime":"node-22",
       "execute":["any"],"scopes":["rows.write"],
       "entrypoint":"index.js","commands":"npm install","enabled":true,
       "logging":true,"timeout":15}'

# For each code change.
tar -czf /tmp/report-problem.tar.gz -C backend/functions/report-problem index.js package.json
curl -X POST "$base/functions/report-problem/deployments" \
  -H "X-Appwrite-Project: being-muslim" -H "X-Appwrite-Key: $APPWRITE_API_KEY" \
  -F "entrypoint=index.js" -F "commands=npm install" -F "activate=true" \
  -F "code=@/tmp/report-problem.tar.gz;type=application/gzip"
```

Wait for the column to report `available` and the deployment `ready`. The
function's contract is covered by `node --test` in
`backend/functions/report-problem/`.

**Verified 2026-10-21** on the development project, then the test row deleted:

| Action | Result |
|---|---|
| Guest executes `report-problem` with a valid report | execution `completed`, `201 {"sent":true}` |
| Row as admin | `payload` is the report; permissions `[]` |
| Guest lists `problem_reports` | `200`, total `0` — no read path |
| Guest sends a body that is not JSON | `400 {"message":"Invalid report"}`, no row |
| Guest sends a report with an extra `email` field | `400 {"message":"Invalid report"}`, no row |
| Test row deleted as admin | `204`; table back to 0 rows |

## Platforms

| Platform | Identifier | Fingerprints |
|---|---|---|
| Android | `app.beingmuslim.android` | Debug SHA-256 `A6:99:53:96:E7:A3:AC:AC:EE:F8:86:3A:9B:9C:85:F4:73:B0:5D:39:C8:C4:60:63:9A:BD:1F:14:B9:C2:E4:9B` |
| Apple | `app.beingmuslim.ios` | none (bundle ID only) |

The debug certificate's SHA-1, needed by Google Cloud for the Android OAuth
client, is `16:B6:2F:59:1B:53:8C:91:CD:B3:DD:D3:72:79:DB:26:96:F6:76:77`.
A release fingerprint is deliberately not registered because the repository has
no release signing configuration (`DECISIONS/0001`); add the Play App Signing
SHA-256 before a release build signs in.

**Finding — the ticket's platform restriction premise does not hold.** Appwrite
2.5 exposes an Android `fingerprints` field (observed in the `PlatformAndroid`
model and the 30.0.1 Project service's `updateAndroidPlatform`), but its own API
reference says the fingerprints are used to accept **passkeys** created by the
app. They do not stop another app that knows the project ID from calling the
API; the project ID is a public client identifier by design. Platform
registration is therefore not an access-control boundary. Data protection comes
from the row permissions above. Preventing another app from using the project
would need a different control (for example, disabling unused auth methods and
rate-limiting abuse), which is not part of this ticket.

## OAuth providers

**Google — configured on 2026-10-21.**

- Google Cloud has an **Android** OAuth client for package
  `app.beingmuslim.android` and the SHA-1 above, plus a **Web application**
  client.
- Appwrite has the Google provider enabled with the web client ID
  `784912150069-loi4m221leaph7hn9oqjk0ogmtg6lav9.apps.googleusercontent.com` and
  its client secret. The secret is stored server-side in Appwrite, never in the
  APK. It was pasted into chat on 2026-10-21: rotate it in Google Cloud and
  update Appwrite's provider settings (the app does not need the new value).
- `nativeEnabled=true` and `nativeClientIds` are also set to the web client ID
  for a future native ID-token flow. The app built for #15 uses the OAuth2
  token flow, which needs the web client's ID and secret on the server.
- The Web application client must authorize the Appwrite callback URI. See
  **Android client flow** below for the exact URL.
- The callback URI is now **https** and Google accepts it. Verified end to end on
  the Android emulator on 2026-10-21: Google sign-in returned to the app, the
  dashboard showed the account name and email, and the session survived a cold
  start.

**Apple — client ready, provider not configured.** The Android app offers Apple
wherever Google is and opens the same token endpoint with `apple`. #32 must
configure the Appwrite Apple provider (Services ID, Key ID, Team ID, `.p8`) and
enable the **browser** flow: the iOS `nativeEnabled` flow does not serve the
Android browser. Until then the Apple button reaches an Appwrite error page, and
Google remains the only provider that completes sign-in.

Do not put the OAuth client secret, `.p8` key, or Apple key material in this
repository. The Appwrite API accepts them over TLS and stores them server-side.

## Android client flow (#15)

- The app opens on an account gate. `AccountViewModel.resolve()` calls
  `account.get()` before the dashboard may render: a live session opens the
  dashboard, no session (or a failed check) opens the login screen, and
  **Continue without an account** enters the existing offline app. The same
  guard runs whenever the account screen is opened from the times screen.
- Sign-in opens
  `{endpoint}/account/tokens/oauth2/{provider}?project=being-muslim&success=…&failure=…`
  where `{provider}` is `google` or `apple`. The success URL is
  `appwrite-callback-being-muslim://auth/success?appstate=<state>`; Appwrite
  adds `userId` and `secret` to it. The failure URL carries the same
  `appstate`, so a forged failure callback is ignored too.
- `AuthCallbackActivity` receives that custom scheme and forwards it to
  `MainActivity`. `AccountViewModel` checks the `appstate` it generated, calls
  `Account.createSession(userId, secret)`, and only then shows the dashboard.
  The state is stored in `SavedStateHandle`, so a callback still works after
  Android recreates the process while the browser is open.
- **Why not the SDK's `createOAuth2Token` method:** its Android implementation
  intercepts the callback and expects `key`/`secret` from the
  `createOAuth2Session` flow, while the 2.5 token endpoint returns
  `userId`/`secret`. The app opens the same token endpoint and exchanges the
  token with `Account.createSession`, as the token-flow contract requires.
- The SDK's session cookie jar keeps the session; `account.get()` is the guard
  on every account-screen entry. No token or secret is logged. The SDK builds
  its cookie store from `getSharedPreferences("myCookie")` and has no store hook,
  so `SecureCookieContext` hands it an `EncryptedSharedPreferences` file
  (`myCookie_secure`) instead: AES256-GCM values under an AES256-GCM key in the
  Android Keystore. A legacy `myCookie` file is copied once and cleared. Both
  files are excluded from platform backup and device transfer
  (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).
- The Appwrite client is built lazily inside `Dispatchers.IO`, so the Keystore
  read never blocks the UI. A broken or invalidated key deletes both the secure
  file and the key alias and requires a fresh sign-in; the failure reaches the
  screen as `CHECK_FAILED` or `SIGN_IN_FAILED` rather than crashing the offline
  app.
- Cancelling while the browser is open returns to the login screen with a
  cancelled state and discards the pending `appstate`, so a later callback
  cannot revive that attempt. Once the callback arrives, the one-time token is
  exchanged and cancellation is no longer offered. A failure callback, an
  unreachable Appwrite check (10-second timeout), a stalled exchange
  (10-second timeout), and a browser that cannot open each have their own
  understandable state.
- **Residual risk — custom scheme:** any app can register
  `appwrite-callback-being-muslim`. The `appstate` check stops a forged callback
  from creating a session or cancelling a real one, but it cannot stop a hostile
  app from receiving the token if it wins the scheme. Before release, move to an
  Android App Link on a domain we control (with a Web platform in Appwrite), or
  accept the risk explicitly for the MVP.
- The Appwrite SDK also ships push; this feature uses neither push nor its
  foreground service, so the unused push components and
  `FOREGROUND_SERVICE_REMOTE_MESSAGING` permission are removed from the merged
  manifest. The remaining `WAKE_LOCK`/`ACCESS_NETWORK_STATE`/
  `RECEIVE_BOOT_COMPLETED`/`FOREGROUND_SERVICE` permissions come from
  WorkManager, which the reminders feature already uses.
- Tests: `AuthRedirectsTest` covers the success/failure deep links;
  `AccountViewModelTest` covers the signed-in/out guards, the state check,
  Google and Apple token URLs, session creation, cancellation, an unopenable
  browser, and sign-out. The JVM suite, lint, and debug/release builds pass.

**Callback URI — resolved 2026-10-21.** Appwrite now emits
`https://appwrite.alveo.dev/v1/account/sessions/oauth2/callback/google/being-muslim`,
and Google accepts it. The token endpoint returns `301` to Google and Google
shows its sign-in page instead of `redirect_uri_mismatch`.

This depends on the **server** environment variable
`_APP_OPTIONS_FORCE_HTTPS=enabled` (or an equivalent proxy setup), not the
project-level variables under `projects/being-muslim/settings/variables`. Keep
the URI registered on the Web application client in Google Cloud. The app's
custom scheme does not need a Google Cloud entry; Google only sees the Appwrite
callback.

## API key

A project API key named **Android** exists with 94 scopes. It is an
administrative key, not a client key: it must never be embedded in either app or
written to a log.

- The key exposed in chat on 2026-10-20 was rotated on 2026-10-21. The
  replacement (key ID `6ad849f3003b92e812b6`) is the only project key and lives
  in `.env.appwrite` (gitignored, mode 600), never in a tracked file.
- Its scope set is broader than least privilege. Narrow it once the
  administrative surface (database, platforms, OAuth, account deletion) is
  known; do not use it as a client key.
- Appwrite 2.5's Project service exposes `createEphemeralKey` but no
  standard-key creation method; standard keys are created in the console.
- The name **Android** is misleading for a server key; rename it to `admin` when
  convenient.

## Verification of the endpoint and project

A real client SDK call (web client `29.1.0`, no API key) resolved the project and
returned data:

```text
new Locale(client).get() -> 200, countryCode "--"
new Account(client).get() -> 401 (guest, expected without a session)
```

A bad project ID returns `404 project_not_found`, so the `401` above confirms
the project was found rather than silently failing. The Android SDK targets the
same 2.5.0 API, and the account state machine is covered by JVM tests. The
callback handshake and a real Google sign-in were verified on the Android
emulator on 2026-10-21; the dashboard showed the account name and the session
survived a cold start.

## Deployment and staging

The self-hosted endpoint is the **production backend** (`DECISIONS/0008`);
Appwrite Cloud is not used. Environment separation follows `DECISIONS/0007`:
the current `being-muslim` project is the development project, a staging project
is created before the first external account, and a production project before
the public release. All three may run on this instance.

Self-hosting also makes TLS renewal, upgrades, backups/restore, monitoring, and
security patching our responsibility. Those are release prerequisites, not
assumptions; see `DECISIONS/0008` for the minimum bar.
