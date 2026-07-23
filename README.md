# TechPulse — Backend

Spring Boot backend for **TechPulse**, an electronics e-commerce store. Handles the product catalog, orders, payments, coupons, admin auth, reviews, and real-time order notifications for the [TechPulse frontend](https://github.com/sohanfernando/mobile-store-fe).

## Tech Stack

- **Java 21** + **Spring Boot 4.1** (Spring Framework 7)
- **MySQL** with **Flyway** migrations (schema is version-controlled, not auto-generated)
- **Spring Security** + **JWT** (JJWT) for admin authentication
- **Spring Data JPA** / Hibernate
- **RabbitMQ** for async order-event messaging
- **WebSocket (STOMP)** for live order notifications on the admin dashboard
- **Stripe** for payment processing
- **OpenPDF** for invoice generation
- **JUnit 5 / Mockito / AssertJ** for testing

## Features

- Product catalog with color variants, per-variant stock, and image uploads
- Order placement with Stripe payment verification and pessimistic-locking stock checks (prevents overselling on concurrent orders)
- Coupon system: percentage/flat discounts, usage limits, minimum order amount, per-customer redemption limits
- Admin authentication via JWT, with login rate limiting and token revocation (logout actually invalidates the token, not just client-side)
- Local file storage for product images behind a swappable `FileStorageService` interface (drop-in ready for S3/CDN later)
- Real-time order notifications pushed to the admin dashboard over WebSocket
- Async order-confirmation messaging via RabbitMQ
- PDF invoice generation for orders
- Product reviews and admin moderation
- Admin analytics (revenue, orders, category breakdown, sales trend)

## Prerequisites

- Java 21+
- MySQL 8+
- RabbitMQ (optional at startup, but required for order-event messaging to work)
- A Stripe account (test-mode keys are fine for local development)

## Configuration

All secrets and environment-specific values are read from environment variables, with dev-only defaults in `application.properties`. Set these before running against anything other than a local throwaway database:

| Variable | Purpose | Dev default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection | `localhost:3306/mobile_store_db`, `root`, `1234` |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | RabbitMQ connection | `localhost`, `5672`, `guest`, `guest` |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe API access | *(empty — required for payments to work)* |
| `JWT_SECRET` | Signing key for admin JWTs (min 32 chars) | insecure placeholder — **override in any real environment** |
| `ADMIN_EMAIL`, `ADMIN_SEED_PASSWORD` | Seeded once into an empty `admins` table on first startup | `change-me@example.com` / `change-me` |
| `STORAGE_LOCATION`, `STORAGE_PUBLIC_BASE_URL` | Where uploaded product images are stored/served from | `./uploads`, `/uploads` |

## Getting Started

```bash
# 1. Start MySQL and RabbitMQ (or point the env vars above at existing instances)

# 2. Run the app (Flyway migrates the schema automatically on startup)
./mvnw spring-boot:run
```

The API is served on `http://localhost:8080`. On first startup with an empty database, Flyway creates the schema and a single admin account is seeded using `ADMIN_EMAIL` / `ADMIN_SEED_PASSWORD`.

### Running tests

```bash
./mvnw test
```

## API Overview

| Area | Base path | Notes |
|---|---|---|
| Products | `/api/products` | Public reads; create/update/delete require admin JWT |
| Orders | `/api/orders` | Order creation, history, PDF invoice download |
| Payments | `/api/payments` | Stripe payment-intent creation and webhook |
| Coupons | `/api/coupons`, `/api/admin/coupons` | Public validation, admin management |
| Reviews | `/api/products/{productId}/reviews` | Public submission, admin moderation via `/api/admin/reviews` |
| Auth | `/api/auth` | Admin login/logout |
| Admin | `/api/admin` | Analytics, customer registry, review moderation |
| Uploads | `/api/admin/uploads` | Admin-only image upload (multipart) |
| WebSocket | `/ws` (STOMP, topic `/topic`) | Live order notifications for the admin dashboard |

Admin-only routes require `Authorization: Bearer <token>` from `/api/auth/login`.

## Project Structure

```
src/main/java/com/mobilestore/mobile_store/
├── controller/     REST controllers
├── service/        Business logic (interfaces + impl/)
├── repository/      Spring Data JPA repositories
├── entity/          JPA entities
├── dto/              Request/response DTOs
├── mapper/          Entity <-> DTO mapping
├── security/        JWT auth filter, rate limiting, token revocation
├── storage/          File storage abstraction (local disk, swappable for S3)
├── messaging/       RabbitMQ event publishing/listening
├── exception/        Typed exceptions + global exception handler
└── config/           Security, CORS, WebSocket, web config

src/main/resources/db/migration/   Flyway SQL migrations
```
