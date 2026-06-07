plugins {
    kotlin("jvm") version "2.1.20"
}

group = "ru.ai_advent_app"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-jackson:3.1.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}