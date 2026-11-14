# Cosmo Cats Intergalactic Marketplace

Java 21, Spring Boot 3.5, Gradle (Kotlin DSL), MapStruct.

## Run
Terminal 1 - the 3rd-party stub (only needed for `/api/v1/delivery-estimates`):

    ./gradlew wiremockRun        # WireMock on http://localhost:8089, mappings in ./wiremock/mappings

Terminal 2 - the database (PostgreSQL 16, see `docker-compose.yml`):

    docker compose up -d db

Terminal 3 - the application:

    ./gradlew bootRun            # http://localhost:8080/api/v1/products

## Contract
`src/main/resources/api-specs/cosmo-cats-api.yml` (open it in https://editor.swagger.io)

All endpoints are versioned: `/api/v1/...`.

## Project structure
Dependencies point only inwards: `web -> application -> domain`; `infrastructure` implements the interfaces
(ports) declared in `domain` and `application`.

    com.cosmocats.marketplace
    |- domain            business model and rules, no framework code
    |  |- model          Product, Money, Category, Cart, Order, ...
    |  |- exception      ProductNotFoundException, OrderNotFoundException, InsufficientStockException, ...
    |  |- repository     ports: ProductRepository, CategoryRepository, OrderRepository
    |  `- common         PageResult
    |- application       use cases
    |  |- product        ProductService
    |  |- category       CategoryService
    |  |- order          OrderService (placing an order)
    |  |- report         ReportService (most bought products)
    |  |- delivery       DeliveryEstimateService, DeliveryEstimator (port), DeliveryEstimate
    |  `- exception      DeliveryServiceException, DeliveryTimeoutException
    |- infrastructure    adapters to the outside world
    |  |- persistence    JPA adapters (Jpa*Repository), entities, Spring Data interfaces, projections
    |  `- delivery       DeliveryClient (RestClient) + its settings and wire format
    `- web               REST API
       |- controller     ProductController, DeliveryController
       |- dto            request / response
       |- mapper         MapStruct mappers
       |- validation     @CosmicWordCheck
       `- error          GlobalExceptionHandler (RFC 9457), FieldViolation

## Demo data
Categories (ids are fixed so they can be used in requests):

| Category        | id                                     |
|-----------------|----------------------------------------|
| Toys            | `11111111-1111-1111-1111-111111111111` |
| Food and drinks | `22222222-2222-2222-2222-222222222222` |
| Treats          | `33333333-3333-3333-3333-333333333333` |

Prices are `price` + `currency` (3-letter code, the marketplace uses `GCR` - Galactic Credit).

## Errors
Every error is `application/problem+json`. Validation errors carry `errors[]` with `field`, a stable `code`
(`NotBlank`, `Size`, `DecimalMax`, `CosmicWordCheck`, `TypeMismatch`, `UnknownField`, `CategoryNotFound`, ...)
and a human-readable `message`.

| Situation                                    | Status |
|----------------------------------------------|--------|
| Invalid body / parameters, unknown category  | 400    |
| Product not found                            | 404    |
| Product name already taken in its category   | 409    |
| Delivery service answered with an error      | 502    |
| Delivery service timed out                   | 504    |

## Testing
All test running and coverage check:
    
    ./gradlew clean build

Integration tests running:

    ./gradlew integrationTest

## Database & tests
The app uses PostgreSQL. Liquibase creates the tables when the app starts (`src/main/resources/db/changelog`).
Start the database and the app as described in **Run** (database/user/password: `cosmocats`/`cosmo`/`cosmo`).
If port 5432 is already used by a local PostgreSQL, stop it or change the port in `docker-compose.yml`
and `application.properties`.

Tests use a real PostgreSQL in Docker (Testcontainers), so Docker must be running. The commands are in
**Testing**; the coverage report is in `build/reports/coverage/index.html`.

### Tables
    category 1 --- N product N --- 1 order_item N --- 1 orders

- `category` - the name is unique.
- `product` - belongs to a category; the name is unique inside its category; there is an index on the name.
- `orders` - `order_number` is unique and is used to find an order (Natural ID).
- `order_item` - one product in one order, with `quantity` and `unit_price`.

Why 3NF: each table holds one kind of thing and nothing is stored twice. For example, the category name
lives only in `category`. `unit_price` is the price at the moment of purchase, so a later price change does not
change old orders. The order total is not stored, it is calculated from the items.

- Ids come from database sequences. The API uses UUIDs, so internal ids are not exposed.
- `OrderService.placeOrder` is one transaction: if one product is out of stock, nothing is saved.
- The "most bought products" report is a JPQL query with a projection. It counts only paid, shipped and delivered orders.
