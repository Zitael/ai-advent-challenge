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

    implementation("io.ktor:ktor-server-core:3.3.0")
    implementation("io.ktor:ktor-server-cio:3.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.0")
    implementation("io.ktor:ktor-server-status-pages:3.3.0")

    implementation("io.ktor:ktor-serialization-jackson:3.3.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")

    implementation("io.modelcontextprotocol:kotlin-sdk-client:0.13.0")
    implementation("io.modelcontextprotocol:kotlin-sdk-server:0.13.0")
}

application {
    mainClass.set("ru.maleks.ai_advent_challenge_app.MainKt")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8"
    )
}

tasks.register<JavaExec>("runRagIndex") {
    group = "application"
    description = "Build local RAG document indexes"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.rag.RagIndexCliKt")
}

tasks.register<JavaExec>("runRagAsk") {
    group = "application"
    description = "Ask a question with and without RAG"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.rag.RagAskCliKt")

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8"
    )
}

tasks.register<JavaExec>("runRagChat") {
    group = "application"
    description = "Run interactive RAG chat with local Ollama model"

    classpath = sourceSets["main"].runtimeClasspath

    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.rag.chat.RagChatCliKt"
    )

    standardInput = System.`in`

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8"
    )
}

tasks.register<JavaExec>("runOllamaDemo") {
    group = "application"
    description = "Run three requests against a local Ollama model"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaDemoCliKt"
    )
}

tasks.register<JavaExec>("runOllamaOptimization") {
    group = "application"
    description = "Compare baseline and optimized Ollama profiles"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaOptimizationCliKt"
    )
}

tasks.register<JavaExec>("runPrivateAiServer") {
    group = "application"
    description = "Run private HTTP API backed by local Ollama"

    classpath = sourceSets["main"].runtimeClasspath

    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.privateai.PrivateAiServerKt"
    )
}
tasks.register<JavaExec>("runDeveloperAssistant") {
    group = "application"
    description = "Run Day 31 developer assistant with project RAG and Git MCP"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.developer.DeveloperAssistantCliKt"
    )
    standardInput = System.`in`
}

tasks.register<JavaExec>("reviewPullRequest") {
    group = "verification"
    description = "Review pull request diff with RAG and Ollama"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.developer.PullRequestReviewMainKt"
    )
}


tasks.register<JavaExec>("runSupportAssistant") {
    group = "application"
    description = "Run Day 33 support assistant with RAG and CRM MCP"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.support.SupportAssistantCliKt"
    )
    standardInput = System.`in`
}

tasks.register<JavaExec>("runProjectFileAssistant") {
    group = "application"
    description = "Run Day 34 assistant that searches, analyzes and modifies project files"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileAssistantCliKt"
    )
    standardInput = System.`in`
}

tasks.register<JavaExec>("prepareRelease") {
    group = "application"
    description = "Run Day 35 AI release preparation pipeline"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.release.ReleaseAssistantCliKt"
    )
    args(
        providers.gradleProperty("releaseVersion").getOrElse("day35-local")
    )
    if (providers.gradleProperty("skipChecks").orNull == "true") {
        args("--skip-checks")
    }
}

tasks.register<JavaExec>("runExecutionLoop") {
    group = "application"
    description = "Run Day 5 autonomous execution loop over task-pool.md"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(
        "ru.maleks.ai_advent_challenge_app.executionloop.ExecutionLoopCliKt"
    )
    if (providers.gradleProperty("executionLoopLimit").isPresent) {
        args("--limit=${providers.gradleProperty("executionLoopLimit").get()}")
    }
    if (providers.gradleProperty("executionLoopQueue").isPresent) {
        args("--queue=${providers.gradleProperty("executionLoopQueue").get()}")
    }
    if (providers.gradleProperty("executionLoopRun").isPresent) {
        args("--run=${providers.gradleProperty("executionLoopRun").get()}")
    }
}

tasks.register<JavaExec>("buildDataset") {
    group = "application"
    description = "Build Day 6 fine-tuning dataset (train.jsonl + eval.jsonl)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.dataset.DatasetBuildCliKt")
}

tasks.register<JavaExec>("validateDataset") {
    group = "verification"
    description = "Validate Day 6 JSONL dataset format"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.dataset.DatasetValidateCliKt")
}

tasks.register<JavaExec>("runDatasetBaseline") {
    group = "application"
    description = "Run gpt-4o-mini baseline on 10 eval examples"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.dataset.DatasetBaselineCliKt")
}

tasks.register<JavaExec>("prepareFineTuning") {
    group = "application"
    description = "Prepare OpenAI fine-tuning workflow (dry-run by default)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.dataset.OpenAiFineTuningCliKt")
    if (project.hasProperty("executeFineTuning")) {
        args("--execute")
    }
}

tasks.register<JavaExec>("runConfidenceInference") {
    group = "application"
    description = "Run Day 7 confidence-controlled ticket classification evaluation"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.classification.ConfidenceInferenceCliKt")
}

tasks.register<JavaExec>("runModelRouting") {
    group = "application"
    description = "Run Day 8 cheap-to-strong model routing evaluation"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.routing.ModelRoutingCliKt")
}

tasks.register<JavaExec>("runDecompositionEvaluation") {
    group = "application"
    description = "Run Day 9 monolithic vs multi-stage inference comparison"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.decomposition.DecompositionCliKt")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Exec>("runSmokeTests") {
    group = "verification"
    description = "Run Playwright smoke scenarios against Private AI UI"

    val isWindows = System.getProperty("os.name")
        .lowercase()
        .contains("windows")

    if (isWindows) {
        commandLine(
            "cmd",
            "/c",
            "npx",
            "playwright",
            "test",
            "testing/smoke/private-ai.smoke.spec.js"
        )
    } else {
        commandLine(
            "npx",
            "playwright",
            "test",
            "testing/smoke/private-ai.smoke.spec.js"
        )
    }
}

tasks.register("fullVerification") {
    group = "verification"
    description = "Run code tests and UI smoke tests, then collect a unified report"
    dependsOn("test", "runSmokeTests")
}
