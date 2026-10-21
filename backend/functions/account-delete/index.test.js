import test from "node:test";
import assert from "node:assert/strict";
import handler from "./index.js";

const ENDPOINT = "https://example.test/v1";
process.env.APPWRITE_FUNCTION_API_ENDPOINT = ENDPOINT;
process.env.APPWRITE_FUNCTION_PROJECT_ID = "project";

function response() {
  return {
    code: 0,
    body: null,
    json(body, code = 200) {
      this.body = body;
      this.code = code;
      return this;
    },
  };
}

function request(userId = "user-1") {
  return {
    headers: userId
      ? { "x-appwrite-user-id": userId, "x-appwrite-key": "ephemeral" }
      : { "x-appwrite-key": "ephemeral" },
  };
}

async function invoke(fetchImpl, userId) {
  const original = globalThis.fetch;
  globalThis.fetch = fetchImpl;
  try {
    const res = response();
    await handler({ req: request(userId), res, error: () => {} });
    return res;
  } finally {
    globalThis.fetch = original;
  }
}

const noContent = async () => ({ ok: true, status: 204 });
const notFound = async () => ({ ok: false, status: 404 });

test("deletes the sync row, the identity, then the row again", async () => {
  const calls = [];
  const res = await invoke(async (url, options) => {
    calls.push({ url, method: options.method });
    return noContent();
  });

  assert.deepEqual(calls, [
    { url: `${ENDPOINT}/tablesdb/being_muslim/tables/user_sync/rows/user-1`, method: "DELETE" },
    { url: `${ENDPOINT}/users/user-1`, method: "DELETE" },
    { url: `${ENDPOINT}/tablesdb/being_muslim/tables/user_sync/rows/user-1`, method: "DELETE" },
  ]);
  assert.deepEqual(res.body, { deleted: true });
  assert.equal(res.code, 200);
});

test("deletes the identity when the row is already gone", async () => {
  const calls = [];
  const res = await invoke(async (url, options) => {
    calls.push(url);
    return options.method === "DELETE" && url.includes("/tablesdb/") ? notFound() : noContent();
  });

  assert.equal(calls.length, 3);
  assert.deepEqual(res.body, { deleted: true });
});

test("treats an already deleted identity as deleted", async () => {
  let rowDeletes = 0;
  const res = await invoke(async (url) => {
    if (url.includes("/users/")) return notFound();
    rowDeletes += 1;
    return noContent();
  });

  assert.equal(rowDeletes, 2);
  assert.deepEqual(res.body, { deleted: true });
  assert.equal(res.code, 200);
});

test("a missing session deletes nothing", async () => {
  let calls = 0;
  const res = await invoke(async () => {
    calls += 1;
    return noContent();
  }, null);

  assert.equal(calls, 0);
  assert.deepEqual(res.body, { message: "Sign in required" });
  assert.equal(res.code, 401);
});

test("a failed row deletion leaves the identity alone", async () => {
  const calls = [];
  const res = await invoke(async (url) => {
    calls.push(url);
    return { ok: false, status: 500 };
  });

  assert.equal(calls.length, 1);
  assert.equal(res.code, 502);
  assert.deepEqual(res.body, { message: "Account not deleted" });
});

test("a failed identity deletion is reported, with the row already gone", async () => {
  const calls = [];
  const res = await invoke(async (url) => {
    calls.push(url);
    return url.includes("/users/") ? { ok: false, status: 500 } : noContent();
  });

  assert.equal(calls.length, 2);
  assert.equal(res.code, 502);
});

test("a failed final cleanup still reports the account as deleted", async () => {
  let rowDeletes = 0;
  const res = await invoke(async (url) => {
    if (url.includes("/users/")) return noContent();
    rowDeletes += 1;
    return rowDeletes === 1 ? noContent() : { ok: false, status: 500 };
  });

  assert.equal(rowDeletes, 2);
  assert.equal(res.code, 200);
  assert.deepEqual(res.body, { deleted: true, cleanedUp: false });
});
