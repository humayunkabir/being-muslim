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
| Android SDK | `io.appwrite:sdk-for-android:30.0.0` — the SDK line that targets server 2.5.x |
| Verification SDK | `appwrite` (web client) `29.1.0`, used only for the call below |

These are not secrets. They are safe to put in build configuration when the
account feature (#15) adds it.

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

**Google — configured and credential-validated on 2026-10-21.**

- Google Cloud has an **Android** OAuth client for package
  `app.beingmuslim.android` and the SHA-1 above, plus a **Web application**
  client.
- Appwrite has `nativeEnabled=true` and `nativeClientIds` set to the web client
  ID `784912150069-loi4m221leaph7hn9oqjk0ogmtg6lav9.apps.googleusercontent.com`.
  The Android app must request its ID token with that web client ID as the
  server client ID; the Android client ID is not the token audience. Appwrite's
  own guidance says Android and web tokens name the same web client
  (`add-google-one-tap-to-your-web-app`).
- The client ID/secret pair was validated against Google while the browser
  redirect flow was briefly enabled. That flow is now **disabled** and the
  secret cleared: the project has no Web platform, and the secret was pasted
  into chat on 2026-10-21. Rotate it in Google Cloud. Native sign-in does not
  use the secret; enable the browser flow later only with a fresh secret and a
  registered Web platform.
- If the browser flow is enabled later, add
  `https://appwrite.alveo.dev/v1/account/sessions/oauth2/callback/google/being-muslim`
  as an **authorized redirect URI** on the Web application client. Authorized
  JavaScript origins are only needed for Google Identity Services used directly
  in a browser; the Android native flow uses neither field.
- **End-to-end sign-in with a test account is still pending** the Android client
  flow in #15. The provider configuration is verified; the user journey is not.

**Apple — not configured.** Deferred to #32 and the iOS work.

Do not put the OAuth client secret, `.p8` key, or Apple key material in this
repository. The Appwrite API accepts them over TLS and stores them server-side.

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
same 2.5.0 API; an Android instrumented call still needs a device/emulator and
is covered when #15 adds the client.

## Deployment and staging

The self-hosted endpoint is the **production backend** (`DECISIONS/0008`);
Appwrite Cloud is not used. Environment separation follows `DECISIONS/0007`:
the current `being-muslim` project is the development project, a staging project
is created before the first external account, and a production project before
the public release. All three may run on this instance.

Self-hosting also makes TLS renewal, upgrades, backups/restore, monitoring, and
security patching our responsibility. Those are release prerequisites, not
assumptions; see `DECISIONS/0008` for the minimum bar.
