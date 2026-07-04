package com.example.aichallenge

class PromptBuilder {

    fun buildPrompt(

        question: String,

        chunks: List<Chunk>

    ): String {

        val builder = StringBuilder()

        builder.appendLine(
            "Ты помощник, который отвечает ИСКЛЮЧИТЕЛЬНО на основе предоставленного контекста."
        )

        builder.appendLine()

        builder.appendLine(
            "Запрещено использовать собственные знания."
        )

        builder.appendLine()

        builder.appendLine(
            "Если информации недостаточно, обязательно ответь:"
        )

        builder.appendLine()

        builder.appendLine(
            "Ответ:"
        )

        builder.appendLine(
            "Не знаю. Пожалуйста, уточните вопрос."
        )

        builder.appendLine()

        builder.appendLine(
            "Источники:"
        )

        builder.appendLine(
            "нет"
        )

        builder.appendLine()

        builder.appendLine(
            "Цитаты:"
        )

        builder.appendLine(
            "нет"
        )

        builder.appendLine()

        builder.appendLine(
            "Во всех остальных случаях ОБЯЗАТЕЛЬНО используй следующий формат:"
        )

        builder.appendLine()

        builder.appendLine(
            "Ответ:"
        )

        builder.appendLine(
            "<ответ>"
        )

        builder.appendLine()

        builder.appendLine(
            "Источники:"
        )

        builder.appendLine(
            "- source=<source>; section=<section>; chunkId=<chunkId>"
        )

        builder.appendLine()

        builder.appendLine(
            "Цитаты:"
        )

        builder.appendLine(
            "- <короткая цитата из найденного чанка>"
        )

        builder.appendLine()

        builder.appendLine(
            "Контекст:"
        )

        builder.appendLine()

        chunks.forEachIndexed { index, chunk ->

            builder.appendLine(
                "Фрагмент ${index + 1}"
            )

            builder.appendLine(
                "source=${chunk.source}"
            )

            builder.appendLine(
                "file=${chunk.fileName}"
            )

            builder.appendLine(
                "section=${chunk.section}"
            )

            builder.appendLine(
                "chunkId=${chunk.chunkId}"
            )

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
            "Вопрос:"
        )

        builder.appendLine(
            question
        )

        return builder.toString()
    }
}