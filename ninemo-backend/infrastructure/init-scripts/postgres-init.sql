-- Create databases. One per PostgreSQL-owning service — services must never share a
-- database, because each runs its own Flyway migrations into flyway_schema_history and
-- two services in one database collide on version numbers (IS-039).
CREATE DATABASE reejuven8_identity;      -- identity-abha-service
CREATE DATABASE reejuven8_ninemo;        -- ninemo-clinical-service
CREATE DATABASE reejuven8_notification;  -- notification-service

-- Grant privileges (reejuven8 user created by POSTGRES_USER env var)
GRANT ALL PRIVILEGES ON DATABASE reejuven8_identity TO reejuven8;
GRANT ALL PRIVILEGES ON DATABASE reejuven8_ninemo TO reejuven8;
GRANT ALL PRIVILEGES ON DATABASE reejuven8_notification TO reejuven8;

-- Enable pg_trgm extension in both databases (needed for fuzzy food search)
\c reejuven8_identity
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c reejuven8_ninemo
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c reejuven8_notification
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
