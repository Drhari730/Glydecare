CREATE TABLE IF NOT EXISTS patients (
  id TEXT PRIMARY KEY,
  doctor_id TEXT NOT NULL,
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  age INTEGER,
  gender TEXT,
  diabetes_type TEXT,
  diagnosis_year INTEGER,
  doctor_name TEXT,
  low_glucose_threshold INTEGER DEFAULT 70,
  high_glucose_threshold INTEGER DEFAULT 180,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_patients_doctor_id ON patients(doctor_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_patients_doctor_phone ON patients(doctor_id, phone);

CREATE TABLE IF NOT EXISTS glucose_readings (
  id BIGSERIAL PRIMARY KEY,
  patient_id TEXT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  value INTEGER NOT NULL,
  reading_type TEXT,
  notes TEXT,
  measured_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_glucose_patient_time ON glucose_readings(patient_id, measured_at DESC);

CREATE TABLE IF NOT EXISTS hba1c_records (
  id BIGSERIAL PRIMARY KEY,
  patient_id TEXT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  value NUMERIC(4, 1) NOT NULL,
  measured_at TIMESTAMPTZ NOT NULL,
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_hba1c_patient_time ON hba1c_records(patient_id, measured_at DESC);

CREATE TABLE IF NOT EXISTS medications (
  id BIGSERIAL PRIMARY KEY,
  patient_id TEXT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  dose TEXT,
  medication_type TEXT,
  frequency TEXT,
  reminder_times TEXT[],
  start_date DATE,
  active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_medications_patient ON medications(patient_id);

CREATE TABLE IF NOT EXISTS medication_logs (
  id BIGSERIAL PRIMARY KEY,
  patient_id TEXT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  medication_id BIGINT REFERENCES medications(id) ON DELETE SET NULL,
  status TEXT NOT NULL,
  logged_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_med_logs_patient_time ON medication_logs(patient_id, logged_at DESC);

CREATE TABLE IF NOT EXISTS vitals_records (
  id BIGSERIAL PRIMARY KEY,
  patient_id TEXT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  pulse INTEGER,
  systolic INTEGER,
  diastolic INTEGER,
  spo2 INTEGER,
  respiratory_rate INTEGER,
  bmi NUMERIC(4, 1),
  measured_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_vitals_patient_time ON vitals_records(patient_id, measured_at DESC);

CREATE TABLE IF NOT EXISTS hospital_visits (
  id BIGSERIAL PRIMARY KEY,
  patient_id TEXT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  visit_date DATE NOT NULL,
  clinic_name TEXT,
  doctor_name TEXT,
  notes TEXT,
  completed BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_visits_patient_date ON hospital_visits(patient_id, visit_date);
