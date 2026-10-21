import test from "node:test";
import assert from "node:assert/strict";
import handler from "./index.js";

const ENDPOINT = "https://example.test/v1";
process.env.APPWRITE_FUNCTION_API_ENDPOINT = ENDPOINT;
process.env.APPWRITE_FUNCTION_PROJECT_ID = "project";

const report = {
  version: 1,
  appVersion: "0.1.0",
  library: "adhan 1.2.1",
  date: "2026-10-21",
  city: {
    name: "Dhaka",
    countryCode: "BD",
    latitude: 23.7104,
    longitude: 90.40744,
    timezone: "Asia/Dhaka",
  },
  method: "KARACHI",
  parameters: {
    fajrAngle: 18,
    ishaAngle: 18,
    ishaInterval: 0,
    adjustments: { fajr: 0, sunrise: 0, dhuhr: 1, asr: 0, maghrib: 0, isha: 0 },
  },
  asrJuristicMethod: "STANDARD",
  highLatitudeRule: "SEVENTH_OF_THE_NIGHT",
  hijriDayShift: 0,
  offsets: { FAJR: 0, DHUHR: 0, ASR: 0, MAGHRIB: 0, ISHA: 0 },
  days: [
    {
      date: "2026-10-21",
      times: {
        FAJR: "2026-10-20T23:38:00Z",
        DHUHR: "2026-10-21T05:50:00Z",
        ASR: "2026-10-21T09:11:00Z",
        MAGHRIB: "2026-10-21T11:31:00Z",
        ISHA: "2026-10-21T12:47:00Z",
      },
      highLatitudeApplied: [],
    },
  ],
  note: "Fajr looks ten minutes early.",
};

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

async function invoke(body, fetchImpl) {
  const original = globalThis.fetch;
  globalThis.fetch = fetchImpl ?? (async () => ({ ok: true, status: 201 }));
  try {
    const res = response();
    await handler({
      req: { headers: { "x-appwrite-key": "ephemeral" }, body: JSON.stringify(body) },
      res,
      error: () => {},
    });
    return res;
  } finally {
    globalThis.fetch = original;
  }
}

test("stores a report as one row with no one allowed to read it back", async () => {
  const calls = [];
  const res = await invoke(report, async (url, options) => {
    calls.push({ url, options });
    return { ok: true, status: 201 };
  });

  assert.equal(res.code, 201);
  assert.deepEqual(res.body, { sent: true });
  assert.equal(calls.length, 1);
  assert.equal(
    calls[0].url,
    `${ENDPOINT}/tablesdb/being_muslim/tables/problem_reports/rows`,
  );
  assert.equal(calls[0].options.method, "POST");
  const sent = JSON.parse(calls[0].options.body);
  assert.equal(sent.rowId, "unique()");
  assert.deepEqual(sent.permissions, []);
  assert.deepEqual(JSON.parse(sent.data.payload), report);
});

test("accepts a report from a guest, because reporting needs no account", async () => {
  const res = await invoke(report);
  assert.equal(res.code, 201);
});

test("rejects a field the calculation inputs do not include", async () => {
  let called = false;
  const res = await invoke(
    { ...report, email: "person@example.test" },
    async () => {
      called = true;
      return { ok: true, status: 201 };
    },
  );

  assert.equal(res.code, 400);
  assert.equal(called, false);
});

test("rejects a report whose date is not a civil date", async () => {
  const res = await invoke({ ...report, date: "21/10/2026" });
  assert.equal(res.code, 400);
});

test("rejects an unknown field nested in the city", async () => {
  const res = await invoke({
    ...report,
    city: { ...report.city, deviceId: "abc" },
  });
  assert.equal(res.code, 400);
});

test("rejects a note longer than the limit", async () => {
  const res = await invoke({ ...report, note: "x".repeat(2001) });
  assert.equal(res.code, 400);
});

test("rejects a body that is not JSON", async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: true, status: 201 });
  try {
    const res = response();
    await handler({ req: { headers: {}, body: "not json" }, res, error: () => {} });
    assert.equal(res.code, 400);
  } finally {
    globalThis.fetch = original;
  }
});

test("rejects a report too large to be the recorded inputs", async () => {
  const res = await invoke({ ...report, note: "x".repeat(20_000) });
  assert.equal(res.code, 413);
});

test("reports a storage failure without claiming the report was sent", async () => {
  const res = await invoke(report, async () => ({ ok: false, status: 500 }));
  assert.equal(res.code, 502);
  assert.deepEqual(res.body, { message: "Report not sent" });
});

test("never echoes the report in the error log", async () => {
  const logged = [];
  const original = globalThis.fetch;
  globalThis.fetch = async () => ({ ok: false, status: 500 });
  try {
    const res = response();
    await handler({
      req: { headers: {}, body: JSON.stringify(report) },
      res,
      error: (message) => logged.push(message),
    });
    assert.equal(res.code, 502);
    assert.ok(logged.every((message) => !message.includes("Fajr looks")));
  } finally {
    globalThis.fetch = original;
  }
});
