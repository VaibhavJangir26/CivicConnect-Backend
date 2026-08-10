# CivicConnect Backend API

Welcome to the backend system of **CivicConnect**, built using **Spring Boot 3.x / Java 17**. This system exposes REST endpoints to manage citizen complaints, categories, OTPs, roles, and profiles, utilizing PostgreSQL, Redis caching, Elasticsearch, and Cloudinary.

---

## 🚀 Technologies & Features

- **Framework**: Spring Boot 4.1.0 (with Java 17 support)
- **Database**: PostgreSQL (managed via Spring Data JPA & Hibernate)
- **Caching & OTP Validation**: Redis
- **Search Engine**: Elasticsearch (for fast full-text search indexing on complaints)
- **Security**: Spring Security + OAuth2 clients (Google & GitHub) + Stateless JWT authentication
- **Notifications**: SMTP Mail integration for signing up and verifying accounts via OTP
- **Media Upload**: Cloudinary integration for uploading images of complaints and proof of resolution
- **Local environment orchestration**: Spring Boot Docker Compose integration

---

## 📂 Project Structure

```
finedgebank/
├── pom.xml                        # Maven dependency and build config
├── .env                           # Local environment configuration file
├── compose.yaml                   # Docker Compose services (Postgres, Redis, Elasticsearch)
├── src/main/java/com/bluewave/civicconnect/
│   ├── CivicConnectApplication.java  # Bootstraps the app and injects dotenv variables
│   ├── auth/                      # Controllers & services for credentials, JWT generation, and OAuth
│   ├── category/                  # Category management (CRUD APIs)
│   ├── complains/                 # Complaint ticketing, assignments, status upgrades, and searching
│   ├── config/                    # Security filters, CORS configuration, Redis, and Cloudinary templates
│   ├── otp/                       # Email OTP delivery, Redis caching, and validation logic
│   ├── profile/                   # Current user info updates
│   ├── profilemanagement/         # Admin views to list directories, assign roles, and toggle users
│   ├── users/                     # Database User entities, roles (CITIZEN, OFFICER, MANAGER, SUPERADMIN)
│   └── utils/                     # JWT parser, custom ApiResponse builders, and standard templates
└── src/main/resources/
    └── application.properties     # Central Spring Configuration maps variables from dotenv
```

---

## 🛠️ Getting Started

### 1. Setup Infrastructure Services
Ensure Docker is running, then start the services via Docker Compose:
```bash
docker compose up -d
```
This spins up:
- **PostgreSQL** on port `5432`
- **Redis** on port `6379`
- **Elasticsearch** on port `9200`

### 2. Configure Environment Variables
Create or customize the [.env](file:///Users/vaibhavjangir/Desktop/Projects/Java%20Project/finedgebank/.env) file in the root directory:
```properties
PORT=8000

POSTGRES_URL=jdbc:postgresql://localhost:5432/mydatabase
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=123456

REDIS_HOST=localhost
REDIS_PORT=6379

ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=123456
ELASTICSEARCH_URI=http://localhost:9200

JWT_SECRET=adkfjijeiois4352uoiewurertyncmazoijdafsd453

GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

GMAIL_HOST=smtp.gmail.com
GMAIL_PORT=587
MY_GMAIL=your-email@gmail.com
GMAIL_APP_ID=your-gmail-app-password

CLOUDINARY_CLOUD_NAME=your-cloudinary-cloud-name
CLOUDINARY_API_KEY=your-cloudinary-api-key
CLOUDINARY_API_SECRET=your-cloudinary-api-secret
```

### 3. Run the Backend App
Run the Spring Boot application using the Maven wrapper:
```bash
./mvnw spring-boot:run
```
The server will start on port `8000` (or whatever `PORT` is defined in `.env`).

---

## 🔒 Roles and Security Flow

- **CITIZEN**: Can create complaints, view their tracker, and request OTP updates.
- **OFFICER**: Assigned specific complaints. Can update statuses and upload completion proof.
- **MANAGER / SUPERADMIN**: Manage categories, promote users to officers, and view directories.
