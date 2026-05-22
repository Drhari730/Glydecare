import pg from "pg";

const { Pool } = pg;

const connectionString = process.env.DATABASE_URL;

if (!connectionString) {
  console.warn("DATABASE_URL is not set. API will start, but database calls will fail.");
}

export const pool = new Pool({
  connectionString,
  ssl: process.env.PGSSLMODE === "disable" ? false : { rejectUnauthorized: false }
});

export async function query(text, params = []) {
  const result = await pool.query(text, params);
  return result;
}

export async function withClient(fn) {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const result = await fn(client);
    await client.query("COMMIT");
    return result;
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}
