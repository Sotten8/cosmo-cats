plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.cosmocats"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val mapstructVersion = "1.6.3"

// Classpath of the standalone WireMock server (3rd-party delivery service stub), used only by `wiremockRun`
val wiremock by configurations.creating

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    wiremock("org.wiremock:wiremock-standalone:3.9.1")
}

tasks.withType<JavaCompile> {
    // Compile against the Java 21 API even when Gradle runs on a newer JDK (sourceCompatibility alone does not)
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}

// ./gradlew wiremockRun  ->  stub server on http://localhost:8089 (mappings in ./wiremock/mappings)
tasks.register<JavaExec>("wiremockRun") {
    group = "application"
    description = "Starts WireMock (delivery service stub) on port 8089"
    classpath = wiremock
    mainClass.set("wiremock.Run")
    args(
        "--port", "8089",
        "--root-dir", layout.projectDirectory.dir("wiremock").asFile.absolutePath,
        "--verbose"
    )
}
