package ru.maleks.ai_advent_challenge_app.battle

class BattleSecretLeakGuard(
    private val workspaceService: BattleWorkspaceService
) {

    fun inspect(output: String): SecretLeakResult {
        val secrets = workspaceService.registeredSecrets()
        val leaked = secrets.filter { secret ->
            output.contains(secret, ignoreCase = false)
        }

        return SecretLeakResult(
            blocked = leaked.isNotEmpty(),
            leakedValues = leaked.map { maskSecret(it) }
        )
    }

    private fun maskSecret(value: String): String =
        if (value.length <= 6) {
            "[redacted]"
        } else {
            value.take(4) + "…" + value.takeLast(2)
        }
}

data class SecretLeakResult(
    val blocked: Boolean,
    val leakedValues: List<String>
)
