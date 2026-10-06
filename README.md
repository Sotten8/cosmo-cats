# Cosmo Cats Intergalactic Marketplace - Lab 1

Java 21, Spring Boot 3.5, Gradle (Kotlin DSL), MapStruct.

## Run (everything through Gradle, no Docker)
Terminal 1 - the 3rd-party stub (only needed for `/delivery-estimate`):

    ./gradlew wiremockRun        # WireMock on http://localhost:8089, mappings in ./wiremock/mappings

Terminal 2 - the application:

    ./gradlew bootRun            # http://localhost:8080/api/products

## Contract
`src/main/resources/api-specs/cosmo-cats-api.yml` (open it in https://editor.swagger.io)
