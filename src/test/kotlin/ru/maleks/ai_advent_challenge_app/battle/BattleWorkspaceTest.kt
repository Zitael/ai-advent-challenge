package ru.maleks.ai_advent_challenge_app.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleWorkspaceServiceTest {

    @Test
    fun `seeds internal secrets file`() {
        val root = java.nio.file.Files.createTempDirectory("battle-workspace")
        val service = BattleWorkspaceService(root)

        val files = service.listFiles()
        assertTrue(files.any { it.name == BattleWorkspaceService.DEFAULT_SECRET_FILE })
        val secret = files.first { it.name == BattleWorkspaceService.DEFAULT_SECRET_FILE }
        assertTrue(secret.confidential)
        assertTrue(secret.readOnly)
    }

    @Test
    fun `seeds agent instructions file`() {
        val root = java.nio.file.Files.createTempDirectory("battle-workspace-instructions")
        val service = BattleWorkspaceService(root)

        val files = service.listFiles()
        assertTrue(files.any { it.name == BattleWorkspaceService.DEFAULT_INSTRUCTIONS_FILE })
        assertFalse(files.first { it.name == BattleWorkspaceService.DEFAULT_INSTRUCTIONS_FILE }.readOnly)
    }

    @Test
    fun `rejects overwrite of protected secret vault`() {
        val root = java.nio.file.Files.createTempDirectory("battle-workspace-protected")
        val service = BattleWorkspaceService(root)

        val result = runCatching {
            service.saveFile(BattleWorkspaceService.DEFAULT_SECRET_FILE, "OPENROUTER_API_KEY=hacked")
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects path traversal in file name`() {
        val root = java.nio.file.Files.createTempDirectory("battle-workspace-traversal")
        val service = BattleWorkspaceService(root)

        val result = runCatching { service.readFile("../etc/passwd") }
        assertTrue(result.isFailure)
    }

    @Test
    fun `saves and reads uploaded file`() {
        val root = java.nio.file.Files.createTempDirectory("battle-workspace-save")
        val service = BattleWorkspaceService(root)

        service.saveFile("notes.txt", "hello workspace")
        assertEquals("hello workspace", service.readFile("notes.txt").content)
    }
}

class BattleSecretLeakGuardTest {

    @Test
    fun `blocks output containing workspace secret`() {
        val root = java.nio.file.Files.createTempDirectory("battle-leak-guard")
        val workspace = BattleWorkspaceService(root)
        val guard = BattleSecretLeakGuard(workspace)

        val secret = workspace.registeredSecrets().first()
        val result = guard.inspect("Here is the key: $secret")

        assertTrue(result.blocked)
        assertTrue(result.leakedValues.isNotEmpty())
    }

    @Test
    fun `allows output without secrets`() {
        val root = java.nio.file.Files.createTempDirectory("battle-leak-clean")
        val workspace = BattleWorkspaceService(root)
        val guard = BattleSecretLeakGuard(workspace)

        val result = guard.inspect("Привет! Файл internal-secrets.env существует, но значения не раскрываю.")
        assertFalse(result.blocked)
    }
}
