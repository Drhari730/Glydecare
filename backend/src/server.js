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

app.use(helmet({ crossOriginResourcePolicy: false }));
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

app.get("/", (_req, res) => {
  res.json({
    ok: true,
    service: "glydecare-backend",
    message: "Glydecare API is running. Use /health or /api/doctor/dashboard?doctorId=MCH%20GM%20001."
  });
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
