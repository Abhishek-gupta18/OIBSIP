# RailReserve — Online Train Reservation System

RailReserve is a desktop application for the **Oasis Infobyte Java Development Internship (Java Development, Task 1)**.
It lets a user log in, book a train ticket, check status of a booking by PNR, and cancel a booking —
with all data persisted in PostgreSQL.

## Features

- **Login** — credentials validated against the `users` table; passwords stored as PBKDF2-HMAC-SHA256
  hashes (never plaintext). Wrong credentials show a clear error message.
- **Book a ticket** — passenger name, train number (numeric only, auto-fills the train name),
  class type, journey date (no past dates), source and destination (must differ). On success a
  unique 10-character PNR is generated and all details are shown in a confirmation dialog.
- **PNR status** — enter a PNR and see the full passenger/journey details. Unknown or invalid
  PNRs produce a friendly message, never a crash.
- **Cancellation** — fetch the booking by PNR, review it, confirm, and it is marked `CANCELLED`
  in the database (repeated cancellations are rejected).
- **Reliability** — all SQL uses `PreparedStatement`; any `SQLException` is caught and surfaced
  through a friendly Swing dialog; on duplicate PNR (SQLState `23505`) a fresh PNR is generated
  automatically and the insert is retried.

## Tech stack

| Component    | Choice                                        |
|--------------|-----------------------------------------------|
| Language     | Java 17+ (built and tested on Java 17/26)     |
| Build        | Maven                                         |
| GUI          | Java Swing                                    |
| Database     | PostgreSQL (local install or Supabase cloud)  |
| JDBC driver  | `org.postgresql:postgresql:42.7.4`            |
| Testing      | JUnit 5                                       |

## Prerequisites

- **JDK 17 or newer** (`java -version`)
- **Maven 3.9+** (`mvn -version`)
- **PostgreSQL 12+** — either a local server or a cloud instance such as Supabase
  (only the session-pooler host is needed, port 5432)

## Database setup

1. Create the database (local server only; Supabase provides one automatically):

   ```sql
   CREATE DATABASE railreserve;
   ```

2. Apply schema and seed data (adjust psql connection flags to your server):

```bash
psql -U postgres -d railreserve -f backend/src/main/resources/schema.sql
psql -U postgres -d railreserve -f backend/src/main/resources/data.sql
```

   `schema.sql` creates `users`, `trains` and `reservations` (idempotent `CREATE TABLE IF NOT EXISTS`).
   `data.sql` inserts 10 sample trains and one demo user — `demo` with password `demo123`
   (stored hashed).

## Environment variables

The app reads (in this order: real environment variables → `.env` file → local defaults):

| Variable      | Meaning                                    | Local default                                    |
|---------------|--------------------------------------------|--------------------------------------------------|
| `DB_URL`      | JDBC URL                                   | `jdbc:postgresql://localhost:5432/railreserve`   |
| `DB_USER`     | database role                              | `postgres`                                       |
| `DB_PASSWORD` | password                                   | *(empty)*                                        |

Copy `middleware/.env.example` to `middleware/.env` and fill in your values for Supabase or another
remote database. `.env` is gitignored — **credentials are never committed**. Start the app from the
`middleware/` folder (its working directory) so the `.env` file is picked up.

## How to run

From the project root:

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.railreserve.Main   # or run Main from your IDE
```

(Use `mvn -f middleware/pom.xml ...` when running from the repository root.)

Alternatively build a runnable jar: `mvn -f middleware/pom.xml package`, then from `middleware/` run

```bash
java -cp "target/classes;target/lib/*" com.railreserve.Main   # Windows (use : on Linux/macOS)
# or, without target/lib on the classpath:
java -jar target/railreserve-1.0.0.jar
```

GUI flow: **Landing page** (ixigo-style hero with pill tabs, **quick PNR check is public by design**, FAQ)
→ **Sign Up** or **Login** (`demo` / `demo123`) → **Dashboard**: Overview, Book Ticket, My Bookings,
PNR Status, Cancel Ticket, sidebar **dark-mode quick toggle**, **Settings** (profile, password,
instant light/dark theme, delete account).

## Folder structure

The repository root contains **exactly three folders** — one per tier:

```
railreserve/
├── frontend/                          presentation layer (Swing UI + entry point)
│   └── src/main/java/com/railreserve/
│       ├── Main.java                  entry point (L&F + DB check + UI launch)
│       └── ui/                        AppFrame + App router, LandingPanel, SignUpScreen,
│                                      LoginScreen, SettingsScreen, UserSession, dashboard/
│                                      (Overview, Book, My Bookings, PNR, Cancel),
│                                      components/ (buttons, fields, cards, toast, ticket card),
│                                      theme/Theme, data/StationCatalog
├── middleware/                        business logic + build + docs
│   ├── pom.xml                        Maven build (assembles all three layers)
│   ├── README.md / TODO.md / .env.example
│   └── src/{main,test}/java/com/railreserve/
│       ├── service/                   AuthService, AccountService, ReservationService,
│       │                              SignupValidator, LoginAttemptTracker,
│       │                              PreferencesService (+ tests)
│       └── util/                      PasswordUtil (PBKDF2), PnrGenerator (+ tests)
└── backend/                           data-access layer and database scripts
    └── src/main/...
        ├── java/com/railreserve/
        │   ├── config/                DatabaseConfig (env vars + .env)
        │   ├── model/                 User, Train, Reservation
        │   └── dao/                   UserDAO, TrainDAO, ReservationDAO,
        │                              AuthServiceStore, HistoryFilter (JDBC only)
        └── resources/                 schema.sql, data.sql, migration_01_users.sql
```

The three top-level folders mark the classic three-tier layers: `frontend` (Swing GUI) talks only to
`middleware` (service business rules), which talks only to `backend` (DAOs + `schema.sql`/`data.sql`).
Java package names (`com.railreserve.ui`, `.service`, `.dao`, ...) are unchanged, so the split is a
file-organization concern; `middleware/pom.xml` wires the cross-folder sources into one build via
`build-helper-maven-plugin` extra source roots.

⚠️ The Maven build file lives at `middleware/pom.xml` — every command below is run either from
inside `middleware/` or with `-f middleware/pom.xml`.

## Running the tests

```bash
mvn -f middleware/pom.xml clean test   # 82 JUnit tests: hashing, PNR generation uniqueness,
                                       # booking/cancellation logic, signup + login rules,
                                       # lockout timing, account settings, preferences,
                                       # theme-shell rebuild
mvn -f middleware/pom.xml package       # full build + tests
```

The service tests run against in-memory fakes of the DAO seam, so no database is required for
`mvn test`.

## Screenshots

Fresh captures of the current UI are committed in `docs/screenshots/` at the repository root
(the root README embeds them). Regenerate with the offscreen render helpers if the UI changes:

- [x] `docs/screenshots/landing.png` — landing page with hero search
- [x] `docs/screenshots/signup.png` — sign-up with strength meter
- [x] `docs/screenshots/login.png` — login screen
- [x] `docs/screenshots/dashboard.png` — dashboard overview
- [x] `docs/screenshots/booking.png` — booking form with train-name auto-fill
- [x] `docs/screenshots/confirmation.png` — ticket-card confirmation
- [x] `docs/screenshots/my-bookings.png` — My Bookings with filters/status chips
- [x] `docs/screenshots/pnr.png` — PNR status ticket card
- [x] `docs/screenshots/cancel.png` — cancellation confirmation
- [x] `docs/screenshots/settings.png` — settings sections
- [x] `docs/screenshots/dark-mode.png` — landing page in dark mode

## Notes / limitations

- My Bookings filters (status/journey-date/train-number/PNR) run on the SQL side and load on a
  SwingWorker; the same data drives the Overview stats.
- Deleting an account removes the account and its bookings in one transaction (documented demo
  behaviour — the FK on `reservations.user_id` makes user-first deletion impossible).
- Theme and default class/source preferences live in `~/.railreserve/preferences.properties`.
  The theme is applied live the moment it is picked in Settings, and the dashboard sidebar
  carries a quick dark-mode toggle (moon/sun) that flips and persists it in one click.
- Demo credentials are for evaluation only; sign up in-app, or insert users with hashes from
  `com.railreserve.util.PasswordUtil#hash`.
