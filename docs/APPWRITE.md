# Appwrite backend configuration

**Status:** Provisioned 2026-10-20 against the self-hosted Appwrite instance that
is now the production backend (`DECISIONS/0008`).
**Owns:** the non-secret backend identifiers, platform registrations, OAuth
provider state, database/table schema and permission model, the verification
evidence for issue #22, and the operational steps for the administrative key.
**Defers:** what may be synced and retained → `PRODUCT_REQUIREMENTS.md` §10;
account and sync behavior → `DECISIONS/0003` and issues #15–#17; secret storage
and rotation policy → `ARCHITECTURE.md` §7.

## Non-secret identifiers

| Setting | Value |
|---|---|
| Endpoint | `https://appwrite.alveo.dev/v1` |
| Project ID | `being-muslim` |
| Server API version | `2.5.0` (`GET /v1/health/version`) |
| Database ID | `being_muslim` |
| Table ID | `user_sync` |
| Android SDK | `io.appwrite:sdk-for-android:31.0.0` — the SDK line that targets server 2.5.x |
| Verification SDK | `appwrite` (web client) `29.1.0`, used only for the call below |

These are not secrets. The Android app reads the endpoint and project ID from
Gradle `BuildConfig` fields (`app/build.gradle.kts`), overridable with
`-PappwriteEndpoint=...` and `-PappwriteProjectId=...`; no key or OAuth secret
is in the APK.

**Licensing:** the `io.appwrite:sdk-for-android` Maven POM declares GPL-3.0, but
the upstream repository's `LICENSE.md` is **BSD-3-Clause** (verified
2026-10-21). Treat it as BSD-3-Clause and include its notice in the app's
third-party notices before release; the POM metadata is wrong.

## Database and permissions

`being_muslim.user_sync` holds one row per signed-in user:

| Column | Type | Notes |
|---|---|---|
| `settings` | varchar(8192) | Opaque JSON object of synced settings; nullable until first sync. Its shape and versioning belong to the sync implementation (#16), not this table. |
| `tasbihTotal` | integer, min 0, default 0 | The tasbih total only, not the label or count |

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

**Before #15/#16:** implement the server-side row creation path (for example an
Appwrite Function or another trusted endpoint). Until it exists, no client can
create a sync row; that gap is intentional and safer than shipping client
create.

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

**Apple — not configured.** Deferred to #32 and the iOS work.

Do not put the OAuth client secret, `.p8` key, or Apple key material in this
repository. The Appwrite API accepts them over TLS and stores them server-side.

## Android client flow (#15)

- The app opens on an account gate. `AccountViewModel.resolve()` calls
  `account.get()` before the dashboard may render: a live session opens the
  dashboard, no session (or a failed check) opens the login screen, and
  **Continue without an account** enters the existing offline app. The same
  guard runs whenever the account screen is opened from the times screen.
- Sign-in opens
  `{endpoint}/account/tokens/oauth2/google?project=being-muslim&success=…&failure=…`.
  The success URL is
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
  on every account-screen entry. No token or secret is logged. The cookie lives
  in the SDK's `myCookie` preferences, which are excluded from platform backup
  and device transfer (`res/xml/backup_rules.xml`,
  `res/xml/data_extraction_rules.xml`). The SDK has no Keystore-backed cookie
  store, so the file is protected by the app sandbox rather than Keystore.
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
  session creation, and sign-out. The JVM suite, lint, and debug/release builds
  pass.

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
