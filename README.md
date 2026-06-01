# FitApp Backend

Backend for FitApp — mobile meal tracking app.

## What it does

- User registration and login with JWT tokens
- Meal logging — add, edit, delete meals with nutrition info
- Food search — local database, USDA FoodData Central, OpenFoodFacts
- Barcode lookup — find product by EAN/UPC barcode
- Dish templates — pre-built dishes with default ingredients and portions
- Food photo recognition — sends image to Groq or GitHub Copilot API, returns dish name and ingredients
- Offline data packs — versioned bundles of dish templates and food dictionary, downloaded by the app on first run

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL 16, Flyway
- Docker

## Deployment

```sh
~/fitapp-backend/redeploy.sh
```

Builds the jar, creates a Docker image and restarts the container.
