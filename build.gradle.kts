plugins {
    kotlin("jvm") version "2.3.21"
}

group = "ru.ai_advent_app"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-jackson:3.1.3")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")

    implementation("io.modelcontextprotocol:kotlin-sdk-client:0.13.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}