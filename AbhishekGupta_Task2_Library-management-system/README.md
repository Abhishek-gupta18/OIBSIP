# RailLibrary — Library Management System 📚

Desktop application for **Oasis Infobyte Java Development Internship — Java Development, Task 2**.
A librarian signs in, manages the book catalogue, issues and receives returned books, and tracks
fines — all data persisted in PostgreSQL.

> Academic demo project. Real-world rules are simplified for the assignment scope.

## Tech stack

Java 17+ · Maven · Java Swing (same ixigo-style theme + component set as Task 1) · PostgreSQL + JDBC
(`org.postgresql:postgresql:42.7.4`) · JUnit 5

## Planned flow

`frontend` (Swing) → `middleware` (business rules) → `backend` (DAOs + JDBC) → PostgreSQL.
Same three-tier layout, logging discipline and `PreparedStatement`-only SQL as the Task 1
online reservation system in this repository.

## Features

- **Librarian login** — username/password backed by a `users` table with PBKDF2 password
  hashing; failed attempts locked for 30 seconds after five tries.
- **Book catalogue** — add, edit and remove books: book id, title, author, category and
  quantity (available vs. total copies).
- **Issue a book** — a student selects a book and receives an issue slip with a return-by date;
  issuing fails gracefully when no copies remain.
- **Receive / return** — mark a book returned, update the catalogue and free the copy.
- **Fines** — overdue days × fine rate, shown at return time and in a history table.

## Status

🚧 Work in progress — code lands in this folder shortly. Once pushed it follows the same
structure as Task 1:

| Folder        | Layer                  | Contents                                                            |
|---------------|------------------------|---------------------------------------------------------------------|
| `backend/`    | Data access            | JDBC-only DAOs, models, `config/DatabaseConfig`, `schema.sql`       |
| `middleware/` | Business logic + build | services + validators, **`pom.xml`**, JUnit tests                   |
| `frontend/`   | Presentation           | Swing UI screens, reusable components, `Theme`                      |

## Screenshots

Coming after the code milestone — same convention as Task 1 (`docs/screenshots/*.png`).
