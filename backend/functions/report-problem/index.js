// Stores a user-initiated prayer-time problem report for the operator.
//
// A report must be reproducible from what it carries: the city and its
// coordinates, the calculation method's parameters, the juristic and
// high-latitude choices, the user's offsets, the dates and the calculated
// times. The allowlist below is that list, so a future client change cannot
// quietly start sending an account id, device details, or location history.
//
// Reports need no account (the app is fully usable signed out), so the function
// executes for any caller. Rows are created with no permissions at all: only the
// server key can read them. Rate limiting is the platform's per-project limit.

const DATABASE_ID = "being_muslim";
const TABLE_ID = "problem_reports";
const MAX_BODY_BYTES = 16 * 1024;
const MAX_NOTE = 2000;
const PRAYERS = ["FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"];
const ADJUSTMENTS = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"];

function isObject(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function hasOnlyKeys(value, keys) {
  return isObject(value) && Object.keys(value).every((key) => keys.includes(key));
}

function text(value, max) {
  return typeof value === "string" && value.length > 0 && value.length <= max;
}

function numberIn(value, min, max) {
  return typeof value === "number" && Number.isFinite(value) && value >= min && value <= max;
}

function intIn(value, min, max) {
  return Number.isInteger(value) && value >= min && value <= max;
}

function isReport(value) {
  if (!hasOnlyKeys(value, [
    "version",
    "appVersion",
    "city",
    "method",
    "parameters",
    "asrJuristicMethod",
    "highLatitudeRule",
    "hijriDayShift",
    "offsets",
    "days",
    "note",
  ])) {
    return false;
  }
  if (value.version !== 1 || !text(value.appVersion, 40)) return false;

  const city = value.city;
  if (!hasOnlyKeys(city, ["name", "countryCode", "latitude", "longitude", "timezone"])) {
    return false;
  }
  if (
    !text(city.name, 120) ||
    !text(city.countryCode, 2) ||
    !numberIn(city.latitude, -90, 90) ||
    !numberIn(city.longitude, -180, 180) ||
    !text(city.timezone, 64)
  ) {
    return false;
  }

  if (
    !text(value.method, 64) ||
    !text(value.asrJuristicMethod, 64) ||
    !text(value.highLatitudeRule, 64) ||
    !intIn(value.hijriDayShift, -1, 1)
  ) {
    return false;
  }

  const parameters = value.parameters;
  if (!hasOnlyKeys(parameters, ["fajrAngle", "ishaAngle", "ishaInterval", "adjustments"])) {
    return false;
  }
  if (
    !numberIn(parameters.fajrAngle, 0, 90) ||
    !numberIn(parameters.ishaAngle, 0, 90) ||
    !intIn(parameters.ishaInterval, 0, 1440)
  ) {
    return false;
  }
  const adjustments = parameters.adjustments;
  if (
    !hasOnlyKeys(adjustments, ADJUSTMENTS) ||
    !ADJUSTMENTS.every((key) => intIn(adjustments[key], -60, 60))
  ) {
    return false;
  }

  const offsets = value.offsets;
  if (
    !hasOnlyKeys(offsets, PRAYERS) ||
    !PRAYERS.every((key) => intIn(offsets[key], -60, 60))
  ) {
    return false;
  }

  if (!Array.isArray(value.days) || value.days.length > 2) {
    return false;
  }
  for (const day of value.days) {
    if (!hasOnlyKeys(day, ["date", "times", "highLatitudeApplied"])) return false;
    if (typeof day.date !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(day.date)) return false;
    if (
      !hasOnlyKeys(day.times, PRAYERS) ||
      !PRAYERS.every((key) => text(day.times[key], 40))
    ) {
      return false;
    }
    if (
      !Array.isArray(day.highLatitudeApplied) ||
      !day.highLatitudeApplied.every((key) => PRAYERS.includes(key))
    ) {
      return false;
    }
  }

  return typeof value.note === "string" && value.note.length <= MAX_NOTE;
}

function apiHeaders(req) {
  return {
    "X-Appwrite-Project": process.env.APPWRITE_FUNCTION_PROJECT_ID,
    "X-Appwrite-Key": req.headers["x-appwrite-key"],
    "Content-Type": "application/json",
  };
}

export default async ({ req, res, error }) => {
  const raw = typeof req.body === "string" ? req.body : JSON.stringify(req.body ?? "");
  // Bytes, not characters: the limit guards storage, and UTF-8 Bengali or
  // Arabic in a note costs more than one byte per character.
  if (Buffer.byteLength(raw, "utf8") > MAX_BODY_BYTES) {
    return res.json({ message: "Invalid report" }, 413);
  }

  let report;
  try {
    report = JSON.parse(raw);
  } catch {
    return res.json({ message: "Invalid report" }, 400);
  }
  if (!isReport(report)) {
    return res.json({ message: "Invalid report" }, 400);
  }

  const endpoint = process.env.APPWRITE_FUNCTION_API_ENDPOINT;
  const created = await fetch(`${endpoint}/tablesdb/${DATABASE_ID}/tables/${TABLE_ID}/rows`, {
    method: "POST",
    headers: apiHeaders(req),
    body: JSON.stringify({
      rowId: "unique()",
      data: { payload: JSON.stringify(report) },
      // No grants: only the server key can read the row back.
      permissions: [],
    }),
  });

  if (!created.ok) {
    // Never log the report or the note; only the failure.
    error(`Could not store the report: ${created.status}`);
    return res.json({ message: "Report not sent" }, 502);
  }

  return res.json({ sent: true }, 201);
};
