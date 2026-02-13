# FitApp Backend (Spring Boot)

Backend API for FitApp (auth, meals, foods, and data packs for offline sync).

## Tech Stack

- Java 21
- Spring Boot 3 (Web, Security, Validation, Data JPA)
- PostgreSQL + Flyway
- JWT auth (jjwt)
- Maven

## Run

PostgreSQL (Docker):

`ash
docker compose up -d postgres
`

App:

`ash
./mvnw spring-boot:run
`

Config via environment variables (see .env.example):
- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- SERVER_PORT
- SPRING_PROFILES_ACTIVE (default: dev)
- JWT_SECRET, JWT_EXPIRATION, JWT_REFRESH_EXPIRATION

## API Endpoints

Health:
- GET /health

Auth:
- POST /auth/register
- POST /auth/login
- POST /auth/refresh

Profile (auth required):
- GET /profile
- PUT /profile
- PUT /profile/password
- DELETE /profile

Meals (auth required):
- GET /meals (optional date=YYYY-MM-DD)
- GET /meals/nutrition (optional date=YYYY-MM-DD)
- POST /meals
- PUT /meals/{id}
- DELETE /meals/{id}

Foods:
- GET /api/foods/search?query=...&locale=en&limit=20
- GET /api/foods/search-full?query=...&limit=20
- GET /api/foods/resolve/{foodKey}
- POST /api/foods/resolve-batch
- GET /api/foods/{id}
- GET /api/foods/barcode/{barcode}
- GET /api/foods/stats

Data packs (offline sync):
- GET /api/packs/versions
- GET /api/packs/dish-templates?since=0
- GET /api/packs/dictionary/{lang}?since=0
- GET /api/packs/mini-usda?since=0

Admin:
- POST /admin/import-usda?path=... (requires ADMIN role)
