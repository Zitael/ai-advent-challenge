# RAG CLI

The RAG CLI provides three Gradle tasks for interacting with local document indexes and Ollama models:

## `runRagIndex`
Builds local RAG document indexes from source files.

```kotlin
tasks.register<JavaExec>("runRagIndex") {
    group = "application"
    description = "Build local RAG document indexes"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.maleks.ai_advent_challenge_app.rag.RagIndexCliKt")
}
```

## `runRagAsk`
Asks a question with and without RAG context.

```kotlin
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
```

## `runRagChat`
Runs an interactive RAG chat with a local Ollama model.

```kotlin
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
```

These tasks follow the architecture pattern where CLI only orchestrates and business logic resides in services.

Summary:
The RAG CLI provides three Gradle tasks for interacting with local document indexes and Ollama models, following the architecture pattern where CLI only orchestrates and business logic resides in services.