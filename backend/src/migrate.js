import "dotenv/config";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { pool } from "./db.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const schema = fs.readFileSync(path.join(__dirname, "schema.sql"), "utf8");

try {
  await pool.query(schema);
  console.log("Database migration completed.");
} finally {
  await pool.end();
}
