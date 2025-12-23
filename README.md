# Ozzu
Ozzu is a Spring Boot–based backend API platform designed for domain-driven interactions such as users, domains, events, wagers, lounges, and token management.  
The API is generated and validated using **OpenAPI**, built with **Java 21**, and designed to be **Docker & AWS deployable**.

## Tech Stack

- **Java**: 21
- **Spring Boot**: 3.3.x
- **Spring Data JPA**
- **PostgreSQL**
- **Hibernate ORM**
- **OpenAPI Generator**
- **Docker**
- **Maven**

##️ Prerequisites

- Java 21
- Maven 3.9+
- Docker (optional, recommended)
- PostgreSQL (local or AWS RDS)

## Run Locally (Without Docker)

### Start PostgreSQL
Ensure Postgres is running and accessible.

Example:
```bash
psql -h localhost -p 5432 -U postgres
```
### Start the App 
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Ozzu Run In Docker

```
docker build --no-cache -t ozzu-api .

docker run --rm -p 3001:3001 -e SPRING_PROFILES_ACTIVE=dev -e SERVER_PORT=300
karthik@Karthikeyans-MacBook-Pro ozzu % docker run --rm -p 3001:3001 \
-e SPRING_PROFILES_ACTIVE=dev \
-e SERVER_PORT=3001 \
-e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/ozzu_dev" \
-e SPRING_DATASOURCE_USERNAME="postgres" \
-e SPRING_DATASOURCE_PASSWORD="postgres" \
ozzu-api
```
## Swagger UI
http://localhost:8080/ozzu/swagger-ui.html