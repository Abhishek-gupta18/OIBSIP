# RailReserve — TODO

Online Train Reservation System — Oasis Infobyte Java Development Internship, Task 1.

- Tech: Java 17+, Maven, Swing, PostgreSQL + JDBC (42.7.4), JUnit 5

## Milestones 1–5

- [x] **M1 — Setup + DB connection:** pom.xml, .gitignore, TODO.md, DatabaseConfig (env vars + .env), schema.sql, data.sql, Main skeleton
- [x] **M2 — Login + dashboard:** models, UserDAO/TrainDAO, AuthService (PBKDF2 hashing), first Swing frames
- [x] **M3 — Booking flow:** ReservationDAO, ReservationService (validation + PNR generation + 23505 retry), train-name auto-fill
- [x] **M4 — PNR search + cancellation:** PNR lookup, confirm-before-cancel, already-cancelled guard
- [x] **M5 — Tests, polish, docs:** 36 JUnit tests, README.md, mvn clean test + mvn package green

## Milestone 6 — finishing touches & submission readiness

- [x] **S1 — Security audit:** full `git log -p --all` scan; no real credentials/hosts in any commit; `.env` never tracked; findings recorded in `middleware/SECURITY_AUDIT.md` (no history rewrite, per owner instruction)
- [x] **S2 — My Bookings tab:** `HistoryFilter` (SQL-side, PreparedStatement-only) + `ReservationDAO.findByUserFiltered` + bookings table (SwingWorker, Refresh, empty state)
- [x] **S3 — UI polish:** status colors, sensible Tab order + Enter submit, friendly dialogs, SQLState codes removed from all messages
- [x] **S4 — Tests:** HistoryFilterTest (7) + ReservationServiceHistoryTest (3); suite 46/46 green, original 36 untouched
- [x] **S5 — Docs:** README Database setup (local OR own Supabase, sslmode=require noted), demo-only credentials marked, screenshots checklist
- [x] **S6 — Final check:** `mvn -f middleware/pom.xml clean test` (46/46) + `package` fresh run, exit 0 on both, jar built

## F-series — modern frontend (Swing)

- [x] **F1 — Theme + components + shell:** `Theme` (navy/saffron palette, light/dark, fonts, spacing, radius), `ui/components` (Primary/Secondary buttons, rounded fields, password field with eye toggle, Card, NavBar, Toast, FormLabel, spinner), `AppFrame` one-window CardLayout router, resizable ≥1100×700
- [x] **F2 — Landing page:** navbar, hero search widget (From/To + swap + autocomplete, date, class), benefit cards, how-it-works, quick PNR check with ticket card, popular routes (pre-fill), collapsible FAQ incl. "not IRCTC" disclaimer, footer + About/Help/Privacy pages
- [x] **F3 — Sign Up + DB migration:** `migration_01_users.sql` (full_name, email unique, phone, created_at) applied and verified; live inline validation, strength meter, duplicate handling via SQLState 23505; 13 tests
- [x] **F4 — Login + session + lockout:** username-or-e-mail login, Enter submit, busy state, 5-failure/30s lockout with countdown, `UserSession`, session-aware navbar; 8 tests
- [x] **F5 — Dashboard:** sidebar (Overview, Book, My Bookings, PNR, Cancel, Settings, Logout); stats + next journey + recent bookings; booking form with auto-fill and ticket-card confirmation; filtered bookings table with details/cancel; PNR/cancel ticket cards
- [x] **F6 — Settings:** profile edit (validation + duplicate e-mail), change password (current verified), live light/dark theme + default class/source saved locally, delete account (password + typed DELETE, bookings removed transactionally); 12 tests
- [x] **F7 — Polish:** legacy frames removed, Enter submits booking form, docs (Screens section, migration notes, 79-test counts), package + GUI smoke run green

## Verification

- `mvn -f middleware/pom.xml clean test` → **79/79 tests**, exit 0
- `mvn -f middleware/pom.xml package` → jar built (`middleware/target/railreserve-1.0.0.jar`)
- GUI smoke: app launches to the landing page against PostgreSQL with no exceptions

## Outstanding items needing the owner

- [ ] Capture the screenshots listed in the root README (`docs/screenshots/`) — need a GUI run with real data
- [ ] Decide: keep or rotate the Supabase password (near-lookalike test string in old M5 history, removed in `9f11380`; audit doc has details)
- [ ] Optional: GitHub remote push when ready (not done — owner said no pushing yet)
