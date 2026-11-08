# Docker and Render deployment

The application uses two Spring profiles:

- `h2` is the default for a normal local Spring Boot run.
- `mysql` is the default inside the Docker image and requires database credentials at runtime.

The backend image does not contain MySQL. Docker Compose or the cloud platform runs MySQL as a separate service so its data can live on persistent storage.

## Run locally with H2

```bash
./mvnw spring-boot:run
```

The API and H2 console are available at:

- `http://localhost:50000`
- `http://localhost:50000/h2-console`

The H2 JDBC URL is `jdbc:h2:mem:testdb`. Its data is recreated when the application restarts.

## Run locally with Docker and MySQL

Optionally copy `.env.example` to `.env` and replace the development passwords. The `.env` file is ignored by Git.

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. MySQL data is stored in the named `mysql-data` Docker volume and survives container recreation.

To stop the services without deleting the database:

```bash
docker compose down
```

Running `docker compose down --volumes` deletes the local MySQL volume and all of its data.

## Publish the backend image to Docker Hub

Run these commands from this directory and replace the example name with your Docker Hub username and repository:

```bash
docker login
docker buildx build --platform linux/amd64 \
  -t YOUR_DOCKERHUB_USERNAME/restful-housekeeping-api:v1 \
  --push .
```

Using a versioned tag makes deployments reproducible. The `linux/amd64` platform is compatible with Render even when the image is built on an Apple Silicon Mac.

## Deploy MySQL on Render

Render does not provide managed MySQL. To keep MySQL on Render, deploy it as a separate private service using Render's MySQL template and attach a persistent disk at `/var/lib/mysql`.

Configure these MySQL service environment variables with strong, unique values:

```text
MYSQL_DATABASE=restfulhousekeeping
MYSQL_USER=<generated-username>
MYSQL_PASSWORD=<generated-password>
MYSQL_ROOT_PASSWORD=<different-generated-root-password>
```

Place the MySQL private service and backend web service in the same Render region. A persistent disk and private service require paid Render resources. An externally managed MySQL provider can be used instead if desired.

## Deploy the Docker Hub image on Render

1. Create a Render Web Service.
2. Choose **Existing Image** and enter `docker.io/YOUR_DOCKERHUB_USERNAME/restful-housekeeping-api:v1`.
3. Use the same region as the MySQL service.
4. Add the following environment variables:

```text
PORT=10000
SPRING_PROFILES_ACTIVE=mysql
SPRING_DATASOURCE_URL=jdbc:mysql://<mysql-private-host>:3306/restfulhousekeeping?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=<same-as-MYSQL_USER>
SPRING_DATASOURCE_PASSWORD=<same-as-MYSQL_PASSWORD>
APP_BOOTSTRAP_ENABLED=false
```

Use the MySQL service's private hostname, not `localhost`. Do not attach the MySQL data disk to the backend service.

The MySQL profile currently uses Hibernate `update` so a new capstone database can initialize itself. Before treating the application as production-grade, replace this with versioned migrations such as Flyway and set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.

Render does not automatically redeploy a service that uses a prebuilt registry image. For each release, push a new versioned tag and update the image URL in the Render service.

## Demo data

Local H2 and local Docker Compose load sample data. Cloud deployment defaults to no sample users because those accounts use known development passwords. To create an intentional temporary demo deployment, set `APP_BOOTSTRAP_ENABLED=true` before the first start, then set it back to `false`.
