// Creates the signed-in user's sync row if it does not exist yet.
//
// The user_sync table grants no create permission to clients on purpose: a
// client that could create rows could pre-create another user's row with a
// payload the victim would then read. This function runs with an ephemeral key
// scoped to rows.read/rows.write and derives the row id from the session user
// id Appwrite passes in, so a client can only ever create its own row.
//
// The app invokes it only when a read finds no row. It never receives payload
// data: settings and the tasbih total are written by the client with the
// row-level permissions set here.

const DATABASE_ID = "being_muslim";
const TABLE_ID = "user_sync";

function rowUrl(endpoint, userId) {
  return `${endpoint}/tablesdb/${DATABASE_ID}/tables/${TABLE_ID}/rows/${encodeURIComponent(userId)}`;
}

function apiHeaders(req) {
  return {
    "X-Appwrite-Project": process.env.APPWRITE_FUNCTION_PROJECT_ID,
    "X-Appwrite-Key": req.headers["x-appwrite-key"],
    "Content-Type": "application/json",
  };
}

export default async ({ req, res, error }) => {
  const userId = req.headers["x-appwrite-user-id"];
  if (!userId) {
    return res.json({ message: "Sign in required" }, 401);
  }

  const endpoint = process.env.APPWRITE_FUNCTION_API_ENDPOINT;
  const headers = apiHeaders(req);

  const existing = await fetch(rowUrl(endpoint, userId), { headers });
  if (existing.ok) {
    return res.json({ created: false });
  }
  if (existing.status !== 404) {
    error(`Could not read the sync row: ${existing.status}`);
    return res.json({ message: "Sync row unavailable" }, 502);
  }

  const created = await fetch(`${endpoint}/tablesdb/${DATABASE_ID}/tables/${TABLE_ID}/rows`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      rowId: userId,
      data: { settings: null, tasbihTotal: 0 },
      permissions: [
        `read("user:${userId}")`,
        `update("user:${userId}")`,
        `delete("user:${userId}")`,
      ],
    }),
  });

  // 409: a concurrent execution created it first, which is the same outcome.
  if (created.ok || created.status === 409) {
    return res.json({ created: created.ok });
  }

  error(`Could not create the sync row: ${created.status}`);
  return res.json({ message: "Sync row unavailable" }, 502);
};
