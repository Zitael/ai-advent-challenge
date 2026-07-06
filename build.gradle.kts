plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "ru.maleks"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    implementation("io.ktor:ktor-client-core:3.3.0")
    implementation("io.ktor:ktor-client-cio:3.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
    implementation("io.ktor:ktor-serialization-jackson:3.3.0")
    implementation("io.ktor:ktor-server-cio:3.3.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")

    implementation("io.modelcontextprotocol:kotlin-sdk-client:0.13.0")
    implementation("io.modelcontextprotocol:kotlin-sdk-server:0.13.0")
}

application {
    mainClass.set("ru.maleks.ai_advent_challenge_app.MainKt")
}

tasks.register<JavaExec>("runRagIndex") {
    group = "application"
    description = "Build local RAG document indexes"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.rag.RagIndexCliKt")
}