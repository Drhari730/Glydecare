# Glydecare Railway Backend

Backend API for the Glydecare Android app and doctor dashboard.

Default clinic/doctor ID:

```text
MCH GM 001
```

## Railway Setup

1. Create a Railway project.
2. Add a PostgreSQL service.
3. Add this `backend` folder as the deployed service.
4. Set variables:

```text
DATABASE_URL=<Railway Postgres DATABASE_URL>
DEFAULT_DOCTOR_ID=MCH GM 001
ALLOWED_ORIGINS=*
```

5. Railway start command is already in `railway.json`:

```text
npm run migrate && npm start
```

## Important Endpoints

```text
GET  /health
POST /api/patients
GET  /api/patients?doctorId=MCH%20GM%20001
GET  /api/doctor/dashboard?doctorId=MCH%20GM%20001
POST /api/patients/:patientId/glucose
POST /api/patients/:patientId/hba1c
POST /api/patients/:patientId/vitals
POST /api/patients/:patientId/visits
```

## Android App

Set this in `local.properties` after Railway deployment:

```text
GLYDECARE_API_BASE_URL=https://your-railway-service.up.railway.app/
```

Then rebuild the APK.
