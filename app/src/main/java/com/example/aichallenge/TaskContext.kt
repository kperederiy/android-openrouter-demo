package com.example.aichallenge

data class TaskContext(

    var currentTask: String = "",

    var currentState: TaskState =
        TaskState.PLANNING,

    var lastResult: String = "",

    var paused: Boolean = false
)