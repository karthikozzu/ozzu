# Ozzu

## Ozzu Run In Local

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
