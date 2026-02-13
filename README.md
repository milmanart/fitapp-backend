# FitApp Backend

Spring Boot API for FitApp: auth, meals, foods, and offline data packs.

## Stack

- Java 21
- Spring Boot 3 (Web, Security, Data JPA, Validation)
- PostgreSQL + Flyway
- JWT (jjwt)
- Maven

## Run

~~~sh
docker compose up -d postgres
./mvnw spring-boot:run
~~~

Env (see .env.example):
- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- SERVER_PORT, SPRING_PROFILES_ACTIVE
- JWT_SECRET

## Endpoints

- GET /health
- POST /auth/register, POST /auth/login, POST /auth/refresh
- GET /profile, PUT /profile, PUT /profile/password, DELETE /profile
- GET /meals, GET /meals/nutrition, POST /meals, PUT /meals/{id}, DELETE /meals/{id}
- GET /api/foods/search, GET /api/foods/resolve/{foodKey}, POST /api/foods/resolve-batch
- GET /api/packs/versions, GET /api/packs/dish-templates, GET /api/packs/dictionary/{lang}, GET /api/packs/mini-usda
