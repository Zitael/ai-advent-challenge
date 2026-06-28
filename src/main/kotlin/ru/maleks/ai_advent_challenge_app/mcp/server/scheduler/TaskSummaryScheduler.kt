package ru.maleks.ai_advent_challenge_app.mcp.server.scheduler

import ru.maleks.ai_advent_challenge_app.mcp.server.MockTaskApi
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TaskSummaryScheduler(
    private val mockTaskApi: MockTaskApi,
    private val storage: TaskSummaryStorage = TaskSummaryStorage(),
    private val intervalSeconds: Long = 10,
    private val keepLastSnapshots: Int = 20
) {
    private var executor: ScheduledExecutorService? = null

    fun start() {
        if (executor != null) {
            return
        }

        executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "task-summary-scheduler").apply {
                isDaemon = true
            }
        }

        executor?.scheduleAtFixedRate(
            {
                try {
                    collectOnce()
                } catch (e: Exception) {
                    println("Task summary scheduler failed: ${e.message}")
                }
            },
            0,
            intervalSeconds,
            TimeUnit.SECONDS
        )

        println("Task summary scheduler started. Interval: ${intervalSeconds}s")
    }

    fun stop() {
        executor?.shutdownNow()
        executor = null
    }

    fun collectOnce(): TaskSummarySnapshot {
        val tasks = mockTaskApi.getAllTasks()

        val total = tasks.size
        val todo = tasks.count { it.status.equals("TODO", ignoreCase = true) }
        val inProgress = tasks.count { it.status.equals("IN_PROGRESS", ignoreCase = true) }
        val done = tasks.count { it.status.equals("DONE", ignoreCase = true) }
        val highPriority = tasks.count { it.priority.equals("HIGH", ignoreCase = true) }

        val summary = buildString {
            append("Tasks: $total total, ")
            append("$done done, ")
            append("$inProgress in progress, ")
            append("$todo todo, ")
            append("$highPriority high priority.")
        }

        val snapshot = TaskSummarySnapshot(
            collectedAt = Instant.now().toString(),
            totalTasks = total,
            todoTasks = todo,
            inProgressTasks = inProgress,
            doneTasks = done,
            highPriorityTasks = highPriority,
            summary = summary
        )

        val state = storage.load()
        state.snapshots.add(snapshot)

        while (state.snapshots.size > keepLastSnapshots) {
            state.snapshots.removeAt(0)
        }

        storage.save(state)

        return snapshot
    }

    fun getState(): TaskSummaryState {
        return storage.load()
    }

    fun getAggregatedSummary(limit: Int = 5): String {
        val snapshots = storage.load().snapshots.takeLast(limit)

        if (snapshots.isEmpty()) {
            return "No task summary snapshots collected yet."
        }

        val latest = snapshots.last()

        return """
            Periodic task summary:
            - snapshots returned: ${snapshots.size}
            - latest collection time: ${latest.collectedAt}
            - latest state: ${latest.summary}

            Recent snapshots:
            ${snapshots.joinToString("\n") { snapshot ->
            "- ${snapshot.collectedAt}: ${snapshot.summary}"
        }}
        """.trimIndent()
    }
}