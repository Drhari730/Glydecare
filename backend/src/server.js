import "dotenv/config";
import cors from "cors";
import express from "express";
import helmet from "helmet";
import morgan from "morgan";
import { query, withClient } from "./db.js";

const app = express();
const port = Number(process.env.PORT || 8080);
const defaultDoctorId = process.env.DEFAULT_DOCTOR_ID || "MCH GM 001";

const allowedOrigins = (process.env.ALLOWED_ORIGINS || "*")
  .split(",")
  .map((origin) => origin.trim())
  .filter(Boolean);

app.use(helmet({
  crossOriginResourcePolicy: false,
  contentSecurityPolicy: false
}));
app.use(express.json({ limit: "1mb" }));
app.use(morgan("tiny"));
app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.includes("*") || allowedOrigins.includes(origin)) {
      callback(null, true);
      return;
    }
    callback(new Error("Origin not allowed"));
  }
}));

app.get(["/", "/admin"], (_req, res) => {
  res.type("html").send(adminPage(defaultDoctorId));
});

app.get("/health", async (_req, res) => {
  try {
    await query("SELECT 1");
    res.json({ ok: true, service: "glydecare-backend", database: "ok" });
  } catch (error) {
    res.status(503).json({ ok: false, service: "glydecare-backend", database: "error" });
  }
});

app.post("/api/patients", async (req, res, next) => {
  try {
    const patient = normalizePatient(req.body);
    await upsertPatient(patient);
    res.status(201).json({ ok: true, patient });
  } catch (error) {
    next(error);
  }
});

app.get("/api/patients", async (req, res, next) => {
  try {
    const doctorId = req.query.doctorId || defaultDoctorId;
    const { rows } = await query(
      `SELECT id, doctor_id AS "doctorId", name, phone, age, gender,
              diabetes_type AS "diabetesType", diagnosis_year AS "diagnosisYear",
              doctor_name AS "doctorName", created_at AS "createdAt", updated_at AS "updatedAt"
         FROM patients
        WHERE doctor_id = $1
        ORDER BY updated_at DESC`,
      [doctorId]
    );
    res.json({ ok: true, doctorId, patients: rows });
  } catch (error) {
    next(error);
  }
});

app.post("/api/patients/:patientId/glucose", async (req, res, next) => {
  try {
    const patientId = req.params.patientId;
    const measuredAt = asDate(req.body.measuredAt || req.body.timestamp);
    const { rows } = await query(
      `INSERT INTO glucose_readings(patient_id, value, reading_type, notes, measured_at)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id`,
      [patientId, Number(req.body.value), req.body.readingType || "", req.body.notes || "", measuredAt]
    );
    res.status(201).json({ ok: true, id: rows[0].id });
  } catch (error) {
    next(error);
  }
});

app.post("/api/patients/:patientId/hba1c", async (req, res, next) => {
  try {
    const patientId = req.params.patientId;
    const measuredAt = asDate(req.body.measuredAt || req.body.timestamp);
    const { rows } = await query(
      `INSERT INTO hba1c_records(patient_id, value, notes, measured_at)
       VALUES ($1, $2, $3, $4)
       RETURNING id`,
      [patientId, Number(req.body.value), req.body.notes || "", measuredAt]
    );
    res.status(201).json({ ok: true, id: rows[0].id });
  } catch (error) {
    next(error);
  }
});

app.post("/api/patients/:patientId/vitals", async (req, res, next) => {
  try {
    const patientId = req.params.patientId;
    const measuredAt = asDate(req.body.measuredAt || req.body.timestamp);
    const { rows } = await query(
      `INSERT INTO vitals_records(patient_id, pulse, systolic, diastolic, spo2, respiratory_rate, bmi, measured_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING id`,
      [
        patientId,
        nullableNumber(req.body.pulse),
        nullableNumber(req.body.systolic),
        nullableNumber(req.body.diastolic),
        nullableNumber(req.body.spo2),
        nullableNumber(req.body.respiratoryRate),
        nullableNumber(req.body.bmi),
        measuredAt
      ]
    );
    res.status(201).json({ ok: true, id: rows[0].id });
  } catch (error) {
    next(error);
  }
});

app.post("/api/patients/:patientId/visits", async (req, res, next) => {
  try {
    const patientId = req.params.patientId;
    const { rows } = await query(
      `INSERT INTO hospital_visits(patient_id, visit_date, clinic_name, doctor_name, notes, completed)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id`,
      [
        patientId,
        req.body.visitDate,
        req.body.clinicName || "",
        req.body.doctorName || "",
        req.body.notes || "",
        Boolean(req.body.completed)
      ]
    );
    res.status(201).json({ ok: true, id: rows[0].id });
  } catch (error) {
    next(error);
  }
});

app.get("/api/doctor/dashboard", async (req, res, next) => {
  try {
    const doctorId = req.query.doctorId || defaultDoctorId;
    const { rows } = await query(
      `WITH latest_glucose AS (
          SELECT DISTINCT ON (patient_id) patient_id, value, measured_at
          FROM glucose_readings
          ORDER BY patient_id, measured_at DESC
        ),
        glucose_trend AS (
          SELECT patient_id, array_agg(value ORDER BY measured_at DESC) AS trend
          FROM (
            SELECT patient_id, value, measured_at,
                   row_number() OVER (PARTITION BY patient_id ORDER BY measured_at DESC) AS rn
            FROM glucose_readings
          ) g
          WHERE rn <= 7
          GROUP BY patient_id
        ),
        latest_a1c AS (
          SELECT DISTINCT ON (patient_id) patient_id, value, measured_at
          FROM hba1c_records
          ORDER BY patient_id, measured_at DESC
        ),
        latest_vitals AS (
          SELECT DISTINCT ON (patient_id) patient_id, pulse, systolic, diastolic, bmi
          FROM vitals_records
          ORDER BY patient_id, measured_at DESC
        ),
        meds AS (
          SELECT patient_id, array_agg(name || COALESCE(' ' || NULLIF(dose, ''), '') ORDER BY active DESC, name) AS medicines
          FROM medications
          WHERE active = true
          GROUP BY patient_id
        ),
        adherence AS (
          SELECT patient_id,
                 ROUND(100.0 * SUM(CASE WHEN status = 'TAKEN' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0))::int AS score
          FROM medication_logs
          WHERE logged_at >= now() - interval '7 days'
          GROUP BY patient_id
        ),
        next_visit AS (
          SELECT DISTINCT ON (patient_id) patient_id, visit_date, doctor_name
          FROM hospital_visits
          WHERE completed = false
          ORDER BY patient_id, visit_date ASC
        )
        SELECT p.id,
               p.doctor_id AS "doctorId",
               p.name,
               p.phone,
               p.age,
               p.gender,
               p.diabetes_type AS "diabetesType",
               p.diagnosis_year AS "diagnosisYear",
               p.doctor_name AS "doctorName",
               COALESCE(lg.value, 0) AS glucose,
               lg.measured_at AS "glucoseDate",
               COALESCE(la.value, 0) AS hba1c,
               COALESCE(ad.score, 0) AS adherence,
               nv.visit_date AS "nextVisit",
               COALESCE(meds.medicines, ARRAY[]::text[]) AS medicines,
               COALESCE(gt.trend, ARRAY[]::int[]) AS trend,
               json_build_object(
                 'pulse', lv.pulse,
                 'bp', CASE WHEN lv.systolic IS NULL OR lv.diastolic IS NULL THEN NULL ELSE lv.systolic || '/' || lv.diastolic END,
                 'bmi', lv.bmi
               ) AS vitals
          FROM patients p
          LEFT JOIN latest_glucose lg ON lg.patient_id = p.id
          LEFT JOIN glucose_trend gt ON gt.patient_id = p.id
          LEFT JOIN latest_a1c la ON la.patient_id = p.id
          LEFT JOIN latest_vitals lv ON lv.patient_id = p.id
          LEFT JOIN meds ON meds.patient_id = p.id
          LEFT JOIN adherence ad ON ad.patient_id = p.id
          LEFT JOIN next_visit nv ON nv.patient_id = p.id
         WHERE p.doctor_id = $1
         ORDER BY p.updated_at DESC`,
      [doctorId]
    );
    res.json({ ok: true, doctorId, patients: rows.map(mapDashboardPatient) });
  } catch (error) {
    next(error);
  }
});

app.post("/api/demo/seed", async (req, res, next) => {
  try {
    const doctorId = req.body.doctorId || defaultDoctorId;
    const patients = req.body.patients || [];
    await withClient(async (client) => {
      for (const raw of patients) {
        const patient = normalizePatient({ ...raw, doctorId });
        await client.query(
          `INSERT INTO patients(id, doctor_id, name, phone, age, gender, diabetes_type, diagnosis_year, doctor_name)
           VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)
           ON CONFLICT (id) DO UPDATE SET
             doctor_id = EXCLUDED.doctor_id,
             name = EXCLUDED.name,
             phone = EXCLUDED.phone,
             age = EXCLUDED.age,
             gender = EXCLUDED.gender,
             diabetes_type = EXCLUDED.diabetes_type,
             diagnosis_year = EXCLUDED.diagnosis_year,
             doctor_name = EXCLUDED.doctor_name,
             updated_at = now()`,
          [patient.id, patient.doctorId, patient.name, patient.phone, patient.age, patient.gender, patient.diabetesType, patient.diagnosisYear, patient.doctorName]
        );
      }
    });
    res.status(201).json({ ok: true, count: patients.length });
  } catch (error) {
    next(error);
  }
});

app.use((error, _req, res, _next) => {
  console.error(error);
  res.status(error.status || 500).json({
    ok: false,
    error: error.message || "Internal server error"
  });
});

app.listen(port, () => {
  console.log(`Glydecare backend listening on ${port}`);
});

function adminPage(doctorId) {
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Glydecare Doctor Dashboard</title>
  <style>
    :root { --bg:#f4f7fb; --card:#fff; --ink:#10202b; --muted:#647887; --line:#dfe7ee; --teal:#006c6b; --blue:#1f74d2; --red:#d92d20; --amber:#f79009; --green:#12b76a; }
    * { box-sizing:border-box; }
    body { margin:0; font-family:Inter, Arial, sans-serif; color:var(--ink); background:var(--bg); }
    header { padding:24px; background:linear-gradient(135deg,#005f60,#0b82d8); color:white; }
    .top { max-width:1180px; margin:auto; display:flex; justify-content:space-between; gap:16px; align-items:center; }
    h1 { margin:0; font-size:30px; }
    .sub { opacity:.86; margin-top:6px; }
    main { max-width:1180px; margin:0 auto; padding:20px; }
    .grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:14px; }
    .card { background:var(--card); border:1px solid var(--line); border-radius:18px; box-shadow:0 8px 24px rgba(16,32,43,.06); padding:18px; }
    .kpi b { display:block; font-size:32px; margin-top:8px; }
    .kpi span, .muted { color:var(--muted); }
    .toolbar { margin:18px 0; display:flex; gap:10px; flex-wrap:wrap; align-items:center; }
    input { flex:1; min-width:240px; border:1px solid var(--line); border-radius:12px; padding:12px 14px; font-size:15px; }
    button { border:0; border-radius:12px; padding:12px 16px; background:var(--teal); color:white; font-weight:800; cursor:pointer; }
    table { width:100%; border-collapse:collapse; overflow:hidden; }
    th, td { padding:13px 12px; border-bottom:1px solid var(--line); text-align:left; vertical-align:top; }
    th { color:var(--muted); font-size:12px; text-transform:uppercase; letter-spacing:.06em; }
    .patient { font-weight:900; }
    .badge { display:inline-block; padding:5px 9px; border-radius:999px; font-size:12px; font-weight:900; }
    .high { background:#fee4e2; color:var(--red); }
    .review { background:#fef0c7; color:#b54708; }
    .stable { background:#dcfae6; color:#067647; }
    .status { font-weight:800; color:var(--teal); }
    .empty { padding:30px; text-align:center; color:var(--muted); }
    @media (max-width:800px) { .grid { grid-template-columns:repeat(2,1fr); } table { font-size:13px; } th:nth-child(6),td:nth-child(6),th:nth-child(7),td:nth-child(7){display:none;} }
  </style>
</head>
<body>
  <header>
    <div class="top">
      <div>
        <h1>Glydecare Doctor Dashboard</h1>
        <div class="sub">Live clinic panel for Doctor ID ${doctorId}</div>
      </div>
      <div class="status" id="status">Connecting...</div>
    </div>
  </header>
  <main>
    <section class="grid">
      <div class="card kpi"><span>Total patients</span><b id="kTotal">0</b></div>
      <div class="card kpi"><span>High risk</span><b id="kHigh">0</b></div>
      <div class="card kpi"><span>Average glucose</span><b id="kGlucose">0</b></div>
      <div class="card kpi"><span>Average HbA1c</span><b id="kA1c">0%</b></div>
    </section>
    <section class="toolbar">
      <input id="search" placeholder="Search patient name, phone, type, medicine">
      <button id="refresh">Refresh now</button>
    </section>
    <section class="card">
      <table>
        <thead>
          <tr>
            <th>Patient</th><th>Phone</th><th>Diabetes</th><th>Glucose</th><th>HbA1c</th><th>Medicines</th><th>Next visit</th><th>Risk</th>
          </tr>
        </thead>
        <tbody id="rows"></tbody>
      </table>
      <div class="empty" id="empty" hidden>No patients found for this doctor ID yet.</div>
    </section>
  </main>
  <script>
    var DOCTOR_ID = ${JSON.stringify(doctorId)};
    var patients = [];
    function esc(value) {
      return String(value == null ? '' : value).replace(/[&<>"']/g, function(ch) {
        return ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#039;' })[ch];
      });
    }
    function avg(values) {
      var nums = values.map(Number).filter(Number.isFinite);
      if (!nums.length) return 0;
      return nums.reduce(function(sum, n) { return sum + n; }, 0) / nums.length;
    }
    function risk(p) {
      if ((p.glucose || 0) >= 200 || (p.hba1c || 0) >= 9 || (p.adherence || 0) < 65) return 'high';
      if ((p.glucose || 0) >= 140 || (p.hba1c || 0) >= 7 || (p.adherence || 0) < 85) return 'review';
      return 'stable';
    }
    function riskText(p) {
      var r = risk(p);
      return r === 'high' ? 'High risk' : r === 'review' ? 'Needs review' : 'Stable';
    }
    function render() {
      var q = document.getElementById('search').value.trim().toLowerCase();
      var rows = patients.filter(function(p) {
        return [p.name, p.phone, p.diabetesType, p.doctorName].concat(p.medicines || []).join(' ').toLowerCase().indexOf(q) >= 0;
      });
      document.getElementById('kTotal').textContent = patients.length;
      document.getElementById('kHigh').textContent = patients.filter(function(p) { return risk(p) === 'high'; }).length;
      document.getElementById('kGlucose').textContent = Math.round(avg(patients.map(function(p) { return p.glucose; })));
      document.getElementById('kA1c').textContent = avg(patients.map(function(p) { return p.hba1c; })).toFixed(1) + '%';
      document.getElementById('empty').hidden = rows.length > 0;
      document.getElementById('rows').innerHTML = rows.map(function(p) {
        return '<tr>' +
          '<td><div class="patient">' + esc(p.name) + '</div><div class="muted">' + esc(p.age) + ' yrs, ' + esc(p.gender) + ' | ' + esc(p.doctorId) + '</div></td>' +
          '<td>' + esc(p.phone) + '</td>' +
          '<td>' + esc(p.diabetesType) + '</td>' +
          '<td><b>' + esc(p.glucose || '--') + '</b> mg/dL<div class="muted">' + esc(p.glucoseDate || 'No reading') + '</div></td>' +
          '<td><b>' + esc(p.hba1c || '--') + '</b></td>' +
          '<td>' + esc((p.medicines || []).join(', ') || '--') + '</td>' +
          '<td>' + esc(p.nextVisit || '--') + '</td>' +
          '<td><span class="badge ' + risk(p) + '">' + riskText(p) + '</span></td>' +
        '</tr>';
      }).join('');
    }
    async function load() {
      document.getElementById('status').textContent = 'Loading live data...';
      try {
        var response = await fetch('/api/doctor/dashboard?doctorId=' + encodeURIComponent(DOCTOR_ID), { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        var data = await response.json();
        patients = data.patients || [];
        document.getElementById('status').textContent = 'Live: ' + patients.length + ' patient(s). Auto-refresh 30 sec';
        render();
      } catch (error) {
        document.getElementById('status').textContent = 'Backend not reachable: ' + error.message;
      }
    }
    document.getElementById('search').addEventListener('input', render);
    document.getElementById('refresh').addEventListener('click', load);
    load();
    setInterval(load, 30000);
  </script>
</body>
</html>`;
}

function normalizePatient(body) {
  const phone = String(body.phone || "").trim();
  const id = body.id || `${body.doctorId || defaultDoctorId}-${phone || Date.now()}`.replace(/\s+/g, "-");
  if (!body.name) throw Object.assign(new Error("Patient name is required"), { status: 400 });
  if (!phone) throw Object.assign(new Error("Patient phone is required"), { status: 400 });
  return {
    id,
    doctorId: body.doctorId || defaultDoctorId,
    name: String(body.name).trim(),
    phone,
    age: nullableNumber(body.age),
    gender: body.gender || "",
    diabetesType: body.diabetesType || "TYPE2",
    diagnosisYear: nullableNumber(body.diagnosisYear),
    doctorName: body.doctorName || "",
    lowGlucoseThreshold: nullableNumber(body.lowGlucoseThreshold) || 70,
    highGlucoseThreshold: nullableNumber(body.highGlucoseThreshold) || 180
  };
}

async function upsertPatient(patient) {
  await query(
    `INSERT INTO patients(id, doctor_id, name, phone, age, gender, diabetes_type, diagnosis_year, doctor_name, low_glucose_threshold, high_glucose_threshold)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
     ON CONFLICT (id) DO UPDATE SET
       doctor_id = EXCLUDED.doctor_id,
       name = EXCLUDED.name,
       phone = EXCLUDED.phone,
       age = EXCLUDED.age,
       gender = EXCLUDED.gender,
       diabetes_type = EXCLUDED.diabetes_type,
       diagnosis_year = EXCLUDED.diagnosis_year,
       doctor_name = EXCLUDED.doctor_name,
       low_glucose_threshold = EXCLUDED.low_glucose_threshold,
       high_glucose_threshold = EXCLUDED.high_glucose_threshold,
       updated_at = now()`,
    [
      patient.id,
      patient.doctorId,
      patient.name,
      patient.phone,
      patient.age,
      patient.gender,
      patient.diabetesType,
      patient.diagnosisYear,
      patient.doctorName,
      patient.lowGlucoseThreshold,
      patient.highGlucoseThreshold
    ]
  );
}

function nullableNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function asDate(value) {
  if (typeof value === "number") return new Date(value).toISOString();
  if (value) return new Date(value).toISOString();
  return new Date().toISOString();
}

function mapDashboardPatient(row) {
  return {
    ...row,
    glucose: Number(row.glucose || 0),
    hba1c: Number(row.hba1c || 0),
    adherence: Number(row.adherence || 0),
    glucoseDate: row.glucoseDate ? new Date(row.glucoseDate).toLocaleString("en-IN") : "",
    nextVisit: row.nextVisit ? new Date(row.nextVisit).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "",
    trend: Array.isArray(row.trend) ? row.trend.slice().reverse().map(Number) : [],
    vitals: row.vitals || {}
  };
}
