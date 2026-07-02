package com.example.aichallenge

class PromptBuilder {

    fun buildPrompt(

        question: String,

        chunks: List<Chunk>

    ): String {

        val builder = StringBuilder()

        builder.appendLine(
            "Ты помощник, который отвечает на вопросы, используя только предоставленный контекст."
        )

        builder.appendLine()

        builder.appendLine("Контекст:")

        builder.appendLine()

        chunks.forEachIndexed { index, chunk ->

            builder.appendLine(
                "Фрагмент ${index + 1}"
            )

            builder.appendLine(
                "Источник: ${chunk.fileName}"
            )

            if (chunk.section.isNotBlank()) {

                builder.appendLine(
                    "Раздел: ${chunk.section}"
                )
            }

            builder.appendLine()

            builder.appendLine(
                chunk.text
            )

            builder.appendLine()

            builder.appendLine(
                "----------------------------------------"
            )

            builder.appendLine()
        }

        builder.appendLine(
            "Вопрос пользователя:"
        )

        builder.appendLine(
            question
        )

        builder.appendLine()

        builder.appendLine(
            "Если ответ отсутствует в контексте, честно сообщи, что информации недостаточно."
        )

        return builder.toString()
    }
}