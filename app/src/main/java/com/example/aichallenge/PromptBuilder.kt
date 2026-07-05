package com.example.aichallenge

class PromptBuilder {

    fun buildPrompt(

        question: String,

        history: List<ChatMessage>,

        chunks: List<Chunk>

    ): String {

        val builder = StringBuilder()

        //--------------------------------------------------
        // System instructions
        //--------------------------------------------------

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
            "Если информации достаточно, обязательно используй следующий формат:"
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
            "- <короткая цитата>"
        )

        builder.appendLine()

        //--------------------------------------------------
        // Chat history
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "История диалога"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        if (history.isEmpty()) {

            builder.appendLine(
                "История отсутствует."
            )

        } else {

            history.forEach { message ->

                builder.appendLine(
                    "${message.role}:"
                )

                builder.appendLine(
                    message.text
                )

                builder.appendLine()
            }
        }

        //--------------------------------------------------
        // RAG Context
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Контекст"
        )

        builder.appendLine(
            "========================================"
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

        //--------------------------------------------------
        // User question
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Вопрос пользователя"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        builder.appendLine(
            question
        )

        builder.appendLine()

        builder.appendLine(
            "Ответь строго по указанному формату."
        )

        builder.appendLine(
            "Не используй информацию вне контекста."
        )

        builder.appendLine(
            "Обязательно укажи источники и цитаты."
        )

        return builder.toString()
    }
}