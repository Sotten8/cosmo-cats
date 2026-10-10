# Cosmo Cats Intergalactic Marketplace - Lab 1

Java 21, Spring Boot 3.5, Gradle (Kotlin DSL), MapStruct.

## Run
Terminal 1 - the 3rd-party stub (only needed for `/api/v1/delivery-estimates`):

    ./gradlew wiremockRun        # WireMock on http://localhost:8089, mappings in ./wiremock/mappings

Terminal 2 - the application:

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
    |  |- exception      ProductNotFoundException, DuplicateProductNameException, CategoryNotFoundException
    |  |- repository     ports: ProductRepository, CategoryRepository
    |  `- common         PageResult
    |- application       use cases
    |  |- product        ProductService
    |  |- delivery       DeliveryEstimateService, DeliveryEstimator (port), DeliveryEstimate
    |  `- exception      DeliveryServiceException, DeliveryTimeoutException
    |- infrastructure    adapters to the outside world
    |  |- persistence    in-memory repositories (until the database is connected)
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
| Product name already taken (any letter case) | 409    |
| Delivery service answered with an error      | 502    |
| Delivery service timed out                   | 504    |
