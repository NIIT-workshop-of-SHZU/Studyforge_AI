# StudyForge AI

StudyForge AI is structured around this architecture:

```text
Vue frontend
    -> Axios / Fetch / Ajax
Spring MVC Controller returning JSON
    -> Service
    -> MyBatis Mapper
    -> MySQL
```

## Repository Layout

```text
StudyForge_AI/
├── docs/
├── sql/
├── scripts/
├── studyforge-frontend/
├── studyforge-server/
└── README.md
```

## Backend Modules

```text
studyforge-server/
├── studyforge-common
├── studyforge-framework
├── studyforge-system
├── studyforge-content
├── studyforge-interaction
├── studyforge-ai
├── studyforge-voice
├── studyforge-help
├── studyforge-admin
└── studyforge-webapi
```

## Build

Maven has been installed under the current Linux user:

```bash
~/.local/bin/mvn -v
```

Build normally:

```bash
cd studyforge-server
mvn -DskipTests package
```

Build through the local proxy on port `7897`:

```bash
./scripts/build_with_proxy.sh
```

The backend entry artifact is:

```text
studyforge-server/studyforge-webapi/target/studyforge-webapi-1.0.0-SNAPSHOT.war
```

## Run Locally

Start the backend API through Maven:

```bash
./scripts/start_api_maven.sh
```

Stop it:

```bash
./scripts/stop_api_maven.sh
```

Health check:

```bash
curl http://localhost:8080/api/v1/health
```

Start the user knowledge platform:

```bash
./scripts/start_knowledge_web.sh
```

User-side Vue URL:

```text
http://localhost:5174
```

## Database

Run the schema and seed scripts in order:

```bash
mysql -u root -p < sql/001_schema.sql
mysql -u root -p < sql/002_seed_data.sql
```

On this Linux machine, the available local database account is `lynn` without a password, and it can create `test_*` databases. The local development database has been initialized as:

```text
test_studyforge_ai_v2
```

Reimport the local database:

```bash
./scripts/import_local_db.sh
```

The local seed data is reset to a production-like study dataset on each import. Current seeded accounts:

```text
User account:  chen_jiayi / StudyForge@2026
Admin account: ops_admin  / AdminForge@2026
```

The schema uses a main table plus i18n table model for content:

- `posts`
- `post_i18n`
- `categories`
- `category_i18n`

This keeps the content model extensible beyond fixed `zh/en` columns.
