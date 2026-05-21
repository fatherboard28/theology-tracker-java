# Theology Study Tracker

A personal, single-user web application for organizing and tracking self-directed theological study. Runs entirely on your local machine — no cloud services, no accounts, no internet required after setup.

---

## Quick Start (Docker)

**Prerequisites:** Docker Desktop (Community Edition)

```bash
# 1. Clone or download the project
# 2. From the project root:

docker compose up --build
```

Open **http://localhost:3000** in your browser.

The SQLite database is created automatically at `./data/theology.db` on first startup.

---

## Data & Backups

All application data lives in a single file:

```
./data/theology.db
```

**To back up everything:** copy this file.  
**To restore:** replace this file and restart the container.

The `./data/` directory is bind-mounted into the container. The container can be deleted and recreated freely — your data is on the host machine, not inside Docker.

---

## Development (without Docker)

**Prerequisites:** JDK 21, Maven 3.9+

```bash
# Run locally against ./data/theology.db
THEOLOGY_DB_PATH=./data/theology.db mvn spring-boot:run
```

Application starts at **http://localhost:8080**.

Spring Boot DevTools is included. Changes to Thymeleaf templates and static files are picked up immediately (browser refresh required). Java class changes trigger an application restart automatically.

---

## Project Structure

```
theology-tracker/
├── src/
│   └── main/
│       ├── java/com/theology/tracker/
│       │   ├── TheologyTrackerApplication.java   # Entry point
│       │   ├── config/
│       │   │   ├── DatabaseConfig.java           # SQLite PRAGMA setup (WAL, FK)
│       │   │   └── WebConfig.java                # MVC config, static resources
│       │   ├── controller/                       # Spring MVC controllers (Phase 3+)
│       │   ├── model/                            # JPA entities (Phase 3+)
│       │   ├── repository/                       # Spring Data repositories (Phase 3+)
│       │   └── service/                          # Business logic (Phase 3+)
│       └── resources/
│           ├── application.properties            # Configuration
│           ├── db/migration/                     # Flyway SQL migrations
│           │   ├── V0__baseline.sql
│           │   └── V1__initial_schema.sql        # Full schema (all tables)
│           ├── templates/                        # Thymeleaf templates
│           │   ├── layout/base.html              # Base layout (nav, footer)
│           │   ├── dashboard/index.html          # Dashboard stub
│           │   └── error/error.html              # Error page
│           └── static/
│               ├── css/main.css                  # Application stylesheet
│               └── js/main.js                    # HTMX config, Sortable, Markdown
├── data/                                         # SQLite DB (bind-mounted, gitignored)
├── Dockerfile                                    # Multi-stage build
├── docker-compose.yml                            # Compose with bind mount
└── pom.xml                                       # Maven dependencies
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3 |
| Web | Spring MVC + Thymeleaf |
| Database | SQLite (via xerial/sqlite-jdbc) |
| ORM | Spring Data JPA + Hibernate (SQLiteDialect) |
| Migrations | Flyway |
| Frontend sprinkles | HTMX 1.9, Alpine.js 3, Sortable.js 1.15, Marked.js |
| Layout dialect | Thymeleaf Layout Dialect (nz.net.ultraq) |
| Container | Docker + Docker Compose |

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `THEOLOGY_DB_PATH` | `/app/data/theology.db` | Path to the SQLite database file |
| `server.port` | `8080` (container) / `3000` (host) | HTTP port |
| `spring.thymeleaf.cache` | `false` | Set to `true` in production for performance |

---

## Implementation Phases

See `tasks.md` for the full implementation task list. Phase 1 (this scaffold) is complete. Phases 2–15 implement the full feature set.

| Phase | Status | Description |
|---|---|---|
| 1 | ✅ Complete | Project setup, infrastructure, base layout, Docker |
| 2 | ✅ Complete (V1 migration) | Full database schema via Flyway |
| 3–15 | 🔲 Pending | Feature implementation per tasks.md |
