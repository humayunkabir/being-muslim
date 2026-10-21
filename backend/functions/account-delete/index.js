// Deletes the signed-in user's synced row and the account itself.
//
// Both steps run here because the client cannot delete an account through the
// Appwrite Android SDK: the account service exposes sessions, not the identity.
// The ephemeral key is scoped to rows.read/rows.write/users.read/users.write and
// the target id is the session user id Appwrite passes in, never a request
// field, so a caller can only delete their own data.
//
// Row first, identity second, row again: a retry after a partial failure finds
// the row already gone (404) and still deletes the identity. The final delete
// closes the window where a sync write already in flight could recreate the row
// between the first delete and the identity deletion. Neither step touches the
// device's local data.

const DATABASE_ID = "being_muslim";
const TABLE_ID = "user_sync";

function rowUrl(endpoint, userId) {
  return `${endpoint}/tablesdb/${DATABASE_ID}/tables/${TABLE_ID}/rows/${encodeURIComponent(userId)}`;
}

function userUrl(endpoint, userId) {
  return `${endpoint}/users/${encodeURIComponent(userId)}`;
}

function apiHeaders(req) {
  return {
    "X-Appwrite-Project": process.env.APPWRITE_FUNCTION_PROJECT_ID,
    "X-Appwrite-Key": req.headers["x-appwrite-key"],
    "Content-Type": "application/json",
  };
}

async function deleteRow(endpoint, headers, userId) {
  const row = await fetch(rowUrl(endpoint, userId), { method: "DELETE", headers });
  // 404: already deleted, or the user never synced. The outcome is the same.
  return row.ok || row.status === 404 ? null : row.status;
}

export default async ({ req, res, error }) => {
  const userId = req.headers["x-appwrite-user-id"];
  if (!userId) {
    return res.json({ message: "Sign in required" }, 401);
  }

  const endpoint = process.env.APPWRITE_FUNCTION_API_ENDPOINT;
  const headers = apiHeaders(req);

  const firstDelete = await deleteRow(endpoint, headers, userId);
  if (firstDelete !== null) {
    error(`Could not delete the sync row: ${firstDelete}`);
    return res.json({ message: "Account not deleted" }, 502);
  }

  const user = await fetch(userUrl(endpoint, userId), { method: "DELETE", headers });
  // 404: a retry after the identity was already removed.
  if (!user.ok && user.status !== 404) {
    error(`Could not delete the account: ${user.status}`);
    return res.json({ message: "Account not deleted" }, 502);
  }

  // The session is dead now, so no further sync can succeed; remove anything a
  // write already in flight recreated after the first delete.
  const finalDelete = await deleteRow(endpoint, headers, userId);
  if (finalDelete !== null) {
    error(`Could not clean up the sync row: ${finalDelete}`);
    return res.json({ message: "Account deleted; sync row cleanup failed" }, 502);
  }

  return res.json({ deleted: true });
};
