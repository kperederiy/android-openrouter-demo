package com.example.aichallenge

class ReviewPromptBuilder {

    fun buildPrompt(

        diff: String,

        projectContext: String,

        documentation: String

    ): String {

        val builder = StringBuilder()

        //--------------------------------------------------
        // System
        //--------------------------------------------------

        builder.appendLine(
            "Ты опытный Senior Android Kotlin разработчик."
        )

        builder.appendLine()

        builder.appendLine(
            "Твоя задача — выполнить code review."
        )

        builder.appendLine()

        builder.appendLine(
            "Используй только предоставленный diff, документацию и структуру проекта."
        )

        builder.appendLine()

        builder.appendLine(
            "Не придумывай несуществующие проблемы."
        )

        builder.appendLine()

        builder.appendLine(
            "Если изменений недостаточно для анализа — так и скажи."
        )

        builder.appendLine()

        //--------------------------------------------------
        // Documentation
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

        builder.appendLine(documentation)

        builder.appendLine()

        //--------------------------------------------------
        // Project context
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Структура проекта"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        builder.appendLine(projectContext)

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

        builder.appendLine(diff)

        builder.appendLine()

        //--------------------------------------------------
        // Output format
        //--------------------------------------------------

        builder.appendLine(
            "========================================"
        )

        builder.appendLine(
            "Формат ответа"
        )

        builder.appendLine(
            "========================================"
        )

        builder.appendLine()

        builder.appendLine(
            "Верни ответ строго в следующем формате:"
        )

        builder.appendLine()

        builder.appendLine(
            "# Общая оценка"
        )

        builder.appendLine(
            "<краткое описание>"
        )

        builder.appendLine()

        builder.appendLine(
            "# Возможные ошибки"
        )

        builder.appendLine(
            "- ..."
        )

        builder.appendLine()

        builder.appendLine(
            "# Архитектурные замечания"
        )

        builder.appendLine(
            "- ..."
        )

        builder.appendLine()

        builder.appendLine(
            "# Рекомендации"
        )

        builder.appendLine(
            "- ..."
        )

        builder.appendLine()

        builder.appendLine(
            "# Итог"
        )

        builder.appendLine(
            "<итоговая оценка>"
        )

        return builder.toString()
    }
}