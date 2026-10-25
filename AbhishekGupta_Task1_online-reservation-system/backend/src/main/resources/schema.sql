-- RailReserve schema (PostgreSQL)

CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name     VARCHAR(100),
    email         VARCHAR(120),
    phone         VARCHAR(20),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Email must be unique across accounts; legacy rows keep NULL email (allowed).
CREATE UNIQUE INDEX IF NOT EXISTS uniq_users_email ON users (email);

CREATE TABLE IF NOT EXISTS trains (
    train_number INT PRIMARY KEY,
    train_name   VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    pnr                 CHAR(10) PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(id),
    passenger_name      VARCHAR(100) NOT NULL,
    train_number        INT NOT NULL REFERENCES trains(train_number),
    class_type          VARCHAR(20) NOT NULL,
    journey_date        DATE NOT NULL,
    source_station      VARCHAR(60) NOT NULL,
    destination_station VARCHAR(60) NOT NULL,
    status              VARCHAR(15) NOT NULL DEFAULT 'CONFIRMED'
            CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reservations_user      ON reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_train     ON reservations(train_number);
CREATE INDEX IF NOT EXISTS idx_reservations_journey   ON reservations(journey_date);
