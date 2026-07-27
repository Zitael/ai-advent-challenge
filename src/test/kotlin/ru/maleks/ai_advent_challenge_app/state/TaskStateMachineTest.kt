package ru.maleks.ai_advent_challenge_app.state

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskStateMachineTest {
    @Test
    fun `moves through lifecycle without skipping stages`() {
        val storage = TaskStateStorage(Files.createTempFile("task-state", ".json").toString())
        storage.clear()
        val machine = TaskStateMachine(storage)

        machine.setTask("Add tests")
        assertEquals(TaskStage.PLANNING, machine.current().stage)
        assertTrue(machine.next().success)
        assertEquals(TaskStage.EXECUTION, machine.current().stage)
        assertTrue(machine.next().success)
        assertEquals(TaskStage.VALIDATION, machine.current().stage)
        assertTrue(machine.next().success)
        assertEquals(TaskStage.DONE, machine.current().stage)
    }

    @Test
    fun `rejects transition that skips validation`() {
        val storage = TaskStateStorage(Files.createTempFile("task-state", ".json").toString())
        storage.clear()
        val machine = TaskStateMachine(storage)
        val result = machine.transitionTo(TaskStage.DONE)
        assertFalse(result.success)
        assertEquals(TaskStage.PLANNING, result.currentStage)
    }
}
