package com.example.aichallenge

class TaskStateMachine {

    fun canMoveForward(
        state: TaskState
    ): Boolean {

        return when(state) {

            TaskState.PLANNING -> true

            TaskState.EXECUTION -> true

            TaskState.VALIDATION -> true

            TaskState.DONE -> false
        }
    }

    fun canMoveBack(
        state: TaskState
    ): Boolean {

        return when(state) {

            TaskState.PLANNING -> false

            TaskState.EXECUTION -> true

            TaskState.VALIDATION -> true

            TaskState.DONE -> true
        }
    }

    fun next(
        state: TaskState
    ): TaskState {

        return when(state) {

            TaskState.PLANNING ->
                TaskState.EXECUTION

            TaskState.EXECUTION ->
                TaskState.VALIDATION

            TaskState.VALIDATION ->
                TaskState.DONE

            TaskState.DONE ->
                TaskState.DONE
        }
    }

    fun previous(
        state: TaskState
    ): TaskState {

        return when(state) {

            TaskState.PLANNING ->
                TaskState.PLANNING

            TaskState.EXECUTION ->
                TaskState.PLANNING

            TaskState.VALIDATION ->
                TaskState.EXECUTION

            TaskState.DONE ->
                TaskState.VALIDATION
        }
    }
}