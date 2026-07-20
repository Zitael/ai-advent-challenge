package ru.maleks.ai_advent_challenge_app.mcp.project

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.relativeTo

class ProjectMcpServerFactory(
    private val projectRoot: Path
) {

    fun create(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "ai-advent-project-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        registerGitCurrentBranch(server)
        registerGitStatus(server)
        registerGitDiff(server)
        registerProjectFiles(server)

        return server
    }

    private fun registerGitCurrentBranch(server: Server) {
        server.addTool(
            name = "git_current_branch",
            description = "Return the current git branch of the project.",
            inputSchema = emptySchema()
        ) {
            textResult(runGit("branch", "--show-current").ifBlank { "DETACHED_HEAD" })
        }
    }

    private fun registerGitStatus(server: Server) {
        server.addTool(
            name = "git_status",
            description = "Return concise git status for the project.",
            inputSchema = emptySchema()
        ) {
            textResult(runGit("status", "--short", "--branch").ifBlank { "Working tree is clean." })
        }
    }

    private fun registerGitDiff(server: Server) {
        server.addTool(
            name = "git_diff",
            description = "Return current unstaged and staged git diff, truncated for safe display.",
            inputSchema = emptySchema()
        ) {
            val unstaged = runGit("diff")
            val staged = runGit("diff", "--cached")
            val combined = buildString {
                appendLine("## Unstaged diff")
                appendLine(unstaged.ifBlank { "No unstaged changes." })
                appendLine()
                appendLine("## Staged diff")
                appendLine(staged.ifBlank { "No staged changes." })
            }
            textResult(combined.take(MAX_OUTPUT_CHARS))
        }
    }

    private fun registerProjectFiles(server: Server) {
        server.addTool(
            name = "project_files",
            description = "Return project file tree excluding build, IDE and git directories.",
            inputSchema = emptySchema()
        ) {
            val ignored = setOf(".git", ".gradle", ".idea", ".kotlin", "build")
            val files = Files.walk(projectRoot).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) }
                    .filter { path ->
                        path.relativeTo(projectRoot).none { part -> part.toString() in ignored }
                    }
                    .map { it.relativeTo(projectRoot).toString().replace('\\', '/') }
                    .sorted()
                    .limit(300)
                    .toList()
            }
            textResult(files.joinToString("\n").ifBlank { "No project files found." })
        }
    }

    private fun runGit(vararg args: String): String {
        val command = listOf("git", "-C", projectRoot.toAbsolutePath().toString()) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val finished = process.waitFor(10, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return "Git command timed out: ${args.joinToString(" ")}"
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return if (process.exitValue() == 0) {
            output
        } else {
            "Git command failed (${process.exitValue()}): ${output.ifBlank { "unknown error" }}"
        }
    }

    private fun emptySchema(): ToolSchema {
        return ToolSchema(
            properties = buildJsonObject { },
            required = emptyList()
        )
    }

    private fun textResult(text: String): CallToolResult {
        return CallToolResult(content = listOf(TextContent(text = text)))
    }

    private companion object {
        const val MAX_OUTPUT_CHARS = 20_000
    }
}
