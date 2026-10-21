# Privacy — the account and your data

**Status:** Implemented 2026-10-21 (issue #17). **Owns:** the plain-language
inventory of what an optional account stores, how long it is kept, what
deletion removes, and what export contains. **Defers:** the exact synced
payload and conflict rule → `SYNC.md`; backend identifiers, permissions, and
function deployment → `APPWRITE.md`; the product rule that this document
explains → `PRODUCT_REQUIREMENTS.md` §10.

## You do not need an account

Every feature works without signing in: prayer times, qibla, the Hijri date,
tasbih, and reminders. Their data stays on your device whether or not you have
an account. An account only carries your settings and tasbih total to another
device.

## What an account stores

The account is held on a self-hosted Appwrite service (`APPWRITE.md`). It stores:

- **Your sign-in identity.** The user id Appwrite assigns, and the name and
  email address your Google or Apple sign-in shares with the app.
- **Your settings.** Calculation method, Asr juristic method, high-latitude
  rule, Hijri day shift, per-prayer offsets, and language — the same choices the
  settings screen controls.
- **Your tasbih total.** The current count only.

The full shape is the versioned payload in `SYNC.md`.

## What an account never stores

- Your city, coordinates, or any location data.
- Calculated prayer times, the Hijri date, or qibla direction.
- Reminder choices, schedules, or notifications.
- The tasbih label or target.
- Any timestamp of your activity or worship.
- Analytics, advertising identifiers, or diagnostic logs containing the above.

These stay on the device. Losing the account does not lose them, and sync cannot
move them.

## How long it is kept

- The synced row and identity stay while the account exists.
- Signing out leaves them in place, so your choices return when you sign in
  again from the same account.
- Clearing the app's data or uninstalling removes only the device's local copy
  and its record of the last sync. The account's copy remains until you delete
  the account.
- Deleting the account removes the synced row and the identity from the service.
  The app has no other server-side copy.

## Deleting your account

In the app, open **Account → Delete account** and confirm. The app asks the
trusted server function to delete:

1. the synced settings and tasbih total, then
2. the sign-in identity, and
3. anything a sync still in flight recreated after step 1.

The deleted account cannot be signed into again. Signing in with the same
Google or Apple account afterwards starts a new account with no synced data.
The confirmation explains this before anything is deleted.

Deletion touches only the account. **Your device keeps its data:** the city,
prayer times, reminders, tasbih counter, and settings remain, and every local
feature keeps working. You can continue without an account or sign in with a
different one.

If the service cannot be reached or an attempt fails, the screen says so. Some
of the account's data may already have been deleted; retrying finishes the
removal. A failed attempt never removes local data. If the account was in fact
removed, the next sign-in check shows you as signed out.

## Exporting your data

In the app, open **Account → Export my data**. The app reads the account's row
and offers a JSON file named `being-muslim-account.json` through the system file
picker. The file is self-describing:

```json
{
  "format": "being-muslim-account",
  "version": 1,
  "account": { "id": "…", "name": "…", "email": "…" },
  "stored": {
    "settings": { "v": 1, "calculationMethod": "KARACHI", "…": "…" },
    "tasbihTotal": 0
  }
}
```

- `stored` is `null` when the account holds no synced data.
- `settings` is `null` when a row exists but no settings have been saved yet;
  `tasbihTotal` is still included.
- Export is read-only: it changes nothing on the service or the device.
- It contains only the account's data. The device's local data is not included
  because it never left the device; it is available on the device itself.

Export needs the service. If it is unreachable, no file is produced and the
screen says so.

## Choices and control

- Signing in is optional and can be undone at any time by signing out or
  deleting the account.
- The app sends only the documented payload. It never sends location,
  devotional timestamps, or notification data (`SYNC.md`).
- There is no analytics or advertising collection to opt out of.
