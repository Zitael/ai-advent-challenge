package ru.ai_advent_app.day1.day10

import api.OpenRouterMessage
import api.TokenCounter

class BranchingStrategy : ContextStrategy {

    override val name = "Branching"

    private val branches = linkedMapOf(
        "main" to DialogueBranch("main")
    )

    private var currentBranchName = "main"

    private val currentBranch: DialogueBranch
        get() = branches[currentBranchName]
            ?: error("Current branch not found: $currentBranchName")

    override suspend fun onUserMessage(message: OpenRouterMessage) {
        currentBranch.messages.add(message)
    }

    override suspend fun onAssistantMessage(message: OpenRouterMessage) {
        currentBranch.messages.add(message)
    }

    override fun buildContext(): List<OpenRouterMessage> {
        return currentBranch.messages
    }

    fun checkpoint(newBranchName: String) {
        branches[newBranchName] = DialogueBranch(
            name = newBranchName,
            messages = currentBranch.messages.toMutableList()
        )
        currentBranchName = newBranchName
    }

    fun switchBranch(branchName: String) {
        if (!branches.containsKey(branchName)) {
            error("Branch not found: $branchName")
        }
        currentBranchName = branchName
    }

    fun listBranches() {
        println("Branches:")
        branches.keys.forEach { branch ->
            val marker = if (branch == currentBranchName) "*" else " "
            println("$marker $branch")
        }
    }

    override fun printStats(tokenCounter: TokenCounter) {
        println("Strategy: $name")
        println("Current branch: $currentBranchName")
        println("Branches: ${branches.keys.joinToString()}")
        println("Messages in current branch: ${currentBranch.messages.size}")
        println("Context tokens: ${tokenCounter.estimateMessagesTokens(buildContext())}")
    }
}