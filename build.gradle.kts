plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

group = "com.cosmocats"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val mapstructVersion = "1.6.3"

val wiremockVersion = "3.9.1"
val wiremock by configurations.creating

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    wiremock("org.wiremock:wiremock-standalone:$wiremockVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

extra["filesExcludedFromCoverage"] = listOf(
    "**/CosmoCatsApplication*",
    "**/*MapperImpl*"
)
extra["minimumCoveragePerFile"] = "0.8".toBigDecimal()

apply(from = "gradle/jacoco.gradle")
apply(from = "gradle/test.gradle")

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}

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
