# Appwrite backend configuration

**Status:** Provisioned 2026-10-20 against a **self-hosted** Appwrite instance.
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

Both providers are **disabled** as of 2026-10-20. The console/API is ready, but
the provider credentials and the end-to-end test account are still human work.

**Google**

1. In Google Cloud, create an **Android** OAuth client for package
   `app.beingmuslim.android` and the SHA-1 above. This authorizes the app to
   obtain Google ID tokens.
2. Also create a **Web application** client in Google Cloud. Appwrite checks the
   ID token's audience against web client IDs, so the trusted value is the **web**
   client ID — not the Android client ID.
3. In Appwrite, set the Google provider's `nativeClientIds` to the web client ID
   and `nativeEnabled` to true. Native sign-in verifies the token signature, so
   it needs no client secret. These fields are the 30.0.1 Project service's
   `updateOAuth2Google` parameters.
4. Browser-based sign-in additionally needs the web client ID and secret with
   `enabled` true. The app's Android flow should use the native path.
5. Sign in on a device with a test account.

Appwrite's native-signin guidance says a Google ID token from Android and one
from the web name the same **web** client ID ("One Tap tokens name the same web
client as Android tokens", Appwrite blog, `add-google-one-tap-to-your-web-app`).
Using the Android client ID here would break sign-in.

**Apple**

1. Create a Services ID, a Sign in with Apple key, and record the Key ID and
   Team ID.
2. In Appwrite, set `serviceId`, `keyId`, `teamId`, and the `.p8` contents;
   optionally add `nativeClientIds` = `app.beingmuslim.ios` and
   `nativeEnabled` = true for the native iOS flow.
3. Sign in on a device with a test account.

Do not put the OAuth client secret, `.p8` key, or Apple key material in this
repository. The Appwrite API accepts them over TLS and stores them server-side.

## API key

A project API key named **Android** exists with broad scopes. It is an
administrative key, not a client key: it must never be embedded in either app or
written to a log.

- It was pasted into a chat on 2026-10-20 and must be **rotated** in the console.
- The local copy lives in `.env.appwrite` (gitignored, mode 600), never in a
  tracked file.
- Appwrite 2.5's Project service exposes `createEphemeralKey` but no
  standard-key creation method; standard keys are created in the console. The
  key ID observed here is `6ad4377b001ed384a05c`, name **Android**.
- Create a replacement in the console, then delete the old key.

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

The endpoint currently points at a **self-hosted** instance, while
`DECISIONS/0003` names Appwrite Cloud as the MVP backend and defers
self-hosting. That is an **unresolved conflict**, not a decision: do not treat
this instance as the production backend until 0003 is amended or a new ADR says
self-hosting replaces it. For now it is the development backend.

Staging decision: `DECISIONS/0007`.
