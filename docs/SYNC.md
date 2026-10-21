# Sync contract — settings and tasbih total

**Status:** Implemented 2026-10-21 (issue #16). **Owns:** the synced payload's
shape and versioning, the conflict rule, sync error behaviour, and the retention
of the server-side copy. **Defers:** what data may be collected or retained →
`PRODUCT_REQUIREMENTS.md` §10; the backend's identifiers, permissions, and
deployment → `APPWRITE.md`; local persistence and offline guarantees →
`ARCHITECTURE.md` §4, §7 and `DECISIONS/0003`.

Sync is **optional** and **best-effort**. The local store is always the source
of truth for the app; sync projects it to the account's row when the service is
reachable. Nothing in the app waits for sync, and no feature changes behaviour
when it fails.

## What is synced

One row per account, `being_muslim.user_sync`, keyed by the Appwrite user id:

| Field | Where | Content |
|---|---|---|
| `settings` | varchar(8192), JSON, nullable | The settings object below |
| `tasbihTotal` | integer ≥ 0 | The tasbih counter's current total only |

The `settings` object, version 1:

```json
{
  "v": 1,
  "calculationMethod": "KARACHI",
  "asrJuristicMethod": "HANAFI",
  "highLatitudeRule": "SEVENTH_OF_THE_NIGHT",
  "hijriDayShift": 1,
  "offsets": { "FAJR": 5, "DHUHR": 0, "ASR": 0, "MAGHRIB": 0, "ISHA": -3 },
  "language": "bn"
}
```

**Never transmitted:** the chosen city or any coordinates/location, computed
prayer times, the tasbih label or target, reminder choices and schedules, and
any timestamp of user activity. The row has no time or location columns. A
sync update time, if the backend records one, is server metadata and is not
part of this payload.

The city is intentionally device-local: it is location data
(`PRODUCT_REQUIREMENTS.md` §10). A new device keeps its own city and adopts the
account's calculation settings, Hijri shift, offsets, language, and tasbih
total.

## Versioning

- `v` is the payload version. This client writes and reads version `1`.
- A payload with any other version is **not applied and not overwritten**. The
  device keeps its local state and leaves the server copy alone; the next
  version of the app can migrate it.
- Missing or unreadable **required** fields (`v`, `calculationMethod`,
  `asrJuristicMethod`, `highLatitudeRule`), or an enum value this build does not
  know, are treated the same as an unknown version: the payload is not applied.
  Missing optional fields (`hijriDayShift`, `offsets`, `language`) default the
  way the local store does: no shift, no offsets, and the device keeps its
  language.
- New optional fields may be added within version 1 only if an older client can
  ignore them; the current decoder ignores unknown object keys, so the version
  must be bumped when a change cannot be ignored.

## Conflict rule

Every device stores the payload it last exchanged with the server. That local
marker — **not a transmitted timestamp** — decides what happened since:

1. **First sync with an account:** if the account already has settings, the
   device adopts them. If the row is empty, the device seeds it with its local
   payload.
2. **After the first sync:** a local payload that differs from the last
   exchanged one has unsynced changes; it is uploaded and wins. If the local
   payload is unchanged, a different server payload is downloaded and applied.
3. **Whole payload, last successful upload wins.** If two devices upload
   concurrent changes, the write that reaches the server last is the state the
   other device adopts on its next sync. There is no field-level merge and no
   clock comparison.
4. **Unavailable server:** nothing is applied; a local change that differs from
   the last exchanged payload still attempts an upload on the next attempt, so
   work done offline is not stranded. **Newer server payload:** neither applied
   nor overwritten; the device keeps its local state until the app understands
   it.

A pull is discarded if the user changes something while it is in flight; the
local change is then uploaded instead, so unsaved work is never silently
replaced. Settings are not pulled while the settings screen has unsaved edits.

## Errors and retries

The backend uses Appwrite's standard error object:

```json
{ "message": "…", "code": 404, "type": "row_not_found", "version": "2.5.0" }
```

The row-creation function's own `401`/`502` responses carry `{ "message": … }`
only; the app inspects the status, not the body. Auth and table endpoints use
the object above.

Sync treats every transport error, non-2xx response, and unreadable body as
**service unavailable**. It does not show an error, does not block a local
feature, and does not retry in a loop. The attempt is repeated when the app
returns to the foreground, when the user changes synced data, or on the next
launch. A failed write leaves the local payload marked as unsynced, so the
next attempt uploads it.

A `404` on read means "no row yet". A `404` on write triggers the row-creation
function synchronously once and retries the write; if that fails, the attempt is
treated as unavailable.

## Idempotency

Each upload is the full settings object and total, keyed by user id, so
re-sending the same payload after a timeout or a retry produces the same row. A
retried push cannot double-count the tasbih total: the total is a value, not an
increment.

## Pagination and filtering

Not applicable: there is exactly one row per user, addressed by user id. The
client never lists rows.

## Times, time zones, and locales

No date, time, or time zone is transmitted, so there is no time-zone conversion
to reconcile: the row carries the user's choices, not a computed schedule. The
language is a short tag (`en` or `bn`); the device maps it to its own locale,
and an unknown tag is ignored rather than applied.

## Retention, deletion, and export

- The row lives server-side while the account exists. Signing out leaves it in
  place so the user's choices return on the next sign-in.
- Account deletion removes the row and the identity
  (`PRODUCT_REQUIREMENTS.md` §10; issue #17). Data export includes this row.
- Clearing app data or uninstalling removes the device's local copy and its
  last-synced marker; the next sign-in pulls the server copy.
- No analytics or diagnostic logs receive the payload. The label is never sent.

## Row creation

Clients have **no create permission** on `user_sync`. A row is created by the
`user-sync` Appwrite Function through the trusted server path, using the user
id Appwrite passes for the caller's session; permissions are then
`read`/`update`/`delete` for that user only. See `APPWRITE.md` for the function,
its scopes, and the verification evidence.

## Tests

`SyncCodecTest` covers the payload's exact shape, the absence of city/location
and timestamps, version rejection, unknown values, offset clamping, and the
language field. `SyncPolicyTest` covers every conflict-rule decision above,
including an unreachable service and a newer payload. `SyncCoordinatorTest`
covers failure swallowing and marker bookkeeping. `SyncMergeTest` covers the
pull merge: city preserved, settings adopted, and the tasbih label/target kept.
All are ordinary JVM tests in
`apps/android/app/src/test/kotlin/app/beingmuslim/android/sync/`.

A two-session server verification of the shared row and last-write-wins
ordering is recorded in `APPWRITE.md`.
