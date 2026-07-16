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

    fun buildReviewPrompt(

        gitBranch: String,

        gitDiff: String,

        projectFiles: String,

        chunks: List<Chunk>

    ): String {

        val builder = StringBuilder()

        //--------------------------------------------------
        // System
        //--------------------------------------------------

        builder.appendLine(
            "Ты Senior Android Kotlin разработчик."
        )

        builder.appendLine()

        builder.appendLine(
            "Ты выполняешь Code Review Pull Request."
        )

        builder.appendLine()

        builder.appendLine(
            "Используй ТОЛЬКО предоставленную информацию."
        )

        builder.appendLine()

        builder.appendLine(
            "Во время анализа учитывай:"
        )

        builder.appendLine("- документацию проекта")
        builder.appendLine("- архитектуру проекта")
        builder.appendLine("- текущую git-ветку")
        builder.appendLine("- список файлов проекта")
        builder.appendLine("- git diff")

        builder.appendLine()

        builder.appendLine(
            "Найди:"
        )

        builder.appendLine("1. Возможные ошибки")
        builder.appendLine("2. Архитектурные проблемы")
        builder.appendLine("3. Нарушения документации")
        builder.appendLine("4. Что можно улучшить")
        builder.appendLine("5. Что сделано хорошо")

        builder.appendLine()

        builder.appendLine(
            "Ответь строго в формате:"
        )

        builder.appendLine()

        builder.appendLine("## Summary")
        builder.appendLine()

        builder.appendLine("## Bugs")
        builder.appendLine()

        builder.appendLine("## Architecture")
        builder.appendLine()

        builder.appendLine("## Documentation")
        builder.appendLine()

        builder.appendLine("## Recommendations")
        builder.appendLine()

        //--------------------------------------------------
        // RAG
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Документация проекта"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        chunks.forEachIndexed { index, chunk ->

            builder.appendLine("Фрагмент ${index + 1}")

            builder.appendLine("source=${chunk.source}")

            builder.appendLine("file=${chunk.fileName}")

            builder.appendLine("section=${chunk.section}")

            builder.appendLine()

            builder.appendLine(chunk.text)

            builder.appendLine()

            builder.appendLine("----------------------------------------")

            builder.appendLine()

        }

        //--------------------------------------------------
        // Branch
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Git Branch"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        builder.appendLine(gitBranch)

        builder.appendLine()

        //--------------------------------------------------
        // Files
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Project Files"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        builder.appendLine(projectFiles)

        builder.appendLine()

        //--------------------------------------------------
        // Diff
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Git Diff"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        builder.appendLine(gitDiff)

        builder.appendLine()

        builder.appendLine(
            "Выполни полноценное Code Review."
        )

        return builder.toString()

    }
}