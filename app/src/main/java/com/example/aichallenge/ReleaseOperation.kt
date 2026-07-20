package com.example.aichallenge

data class ReleaseOperation(

    val type: String,

    val path: String,

    val find: String = "",

    val replace: String = "",

    val after: String = "",

    val before: String = "",

    val text: String = ""

)