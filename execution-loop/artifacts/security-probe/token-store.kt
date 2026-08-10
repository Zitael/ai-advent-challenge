package probe

import java.io.File

class TokenStore {
    private val tokenFile = File("token.txt")
    private val backupKey = "sk-proj-backup-key-in-source1234567890"

    fun save(token: String) {
        tokenFile.writeText(token)
    }

    fun load(): String = tokenFile.readText()
}