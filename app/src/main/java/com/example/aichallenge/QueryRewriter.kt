package com.example.aichallenge

class QueryRewriter {

    fun rewrite(

        question: String

    ): String {

        var rewritten = question.trim()

        rewritten = rewritten.replace(

            Regex("\\s+"),

            " "
        )

        rewritten = rewritten.replace(

            "DI",

            "Dependency Injection",

            ignoreCase = true
        )

        rewritten = rewritten.replace(

            "VM",

            "ViewModel",

            ignoreCase = true
        )

        rewritten = rewritten.replace(

            "UI",

            "User Interface",

            ignoreCase = true
        )

        rewritten = rewritten.replace(

            "Compose UI",

            "Jetpack Compose",

            ignoreCase = true
        )

        rewritten = rewritten.replace(

            "Coroutine",

            "Kotlin Coroutine",

            ignoreCase = true
        )

        return rewritten
    }
}