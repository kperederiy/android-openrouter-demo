package com.example.aichallenge

class ReleasePromptBuilder {

    fun buildPrompt(

        gitBranch: String,

        gitStatus: String,

        gitDiff: String,

        projectFiles: String,

        documentation: List<Chunk>

    ): String {

        val builder = StringBuilder()

        //--------------------------------------------------
        // ROLE
        //--------------------------------------------------

        builder.appendLine(
            "Ты опытный Release Engineer и AI Agent."
        )

        builder.appendLine()

        builder.appendLine(
            "Твоя задача — подготовить релиз проекта."
        )

        builder.appendLine()

        builder.appendLine(
            "Ты НЕ должен переписывать существующие файлы полностью."
        )

        builder.appendLine(
            "Ты должен вернуть список минимальных операций редактирования."
        )

        builder.appendLine()

        builder.appendLine(
            "Используй следующие операции:"
        )

        builder.appendLine()

        builder.appendLine(
            "write - создать новый файл"
        )

        builder.appendLine(
            "update - полностью заменить файл (использовать ТОЛЬКО если это действительно необходимо)"
        )

        builder.appendLine(
            "replace - заменить небольшой фрагмент текста"
        )

        builder.appendLine(
            "append_after - вставить текст после существующего раздела"
        )

        builder.appendLine()

        builder.appendLine(
            "Всегда предпочитай replace и append_after."
        )

        builder.appendLine(
            "update использовать только если невозможно выполнить редактирование."
        )

        builder.appendLine()

        builder.appendLine(
            "Если README уже существует — никогда не используй update."
        )

        builder.appendLine(
            "Если требуется добавить новую версию — используй append_after."
        )

        builder.appendLine()

        builder.appendLine(
            "Верни ТОЛЬКО JSON."
        )

        builder.appendLine(
            "Без Markdown."
        )

        builder.appendLine(
            "Без пояснений."
        )

        builder.appendLine()

        builder.appendLine(
            "Формат:"
        )

        builder.appendLine()

        builder.appendLine(
            "{"
        )

        builder.appendLine(
            "  \"operations\": ["
        )

        builder.appendLine(
            "    {"
        )

        builder.appendLine(
            "      \"type\":\"append_after\","
        )

        builder.appendLine(
            "      \"path\":\"app/src/main/assets/README.md\","
        )

        builder.appendLine(
            "      \"after\":\"## Changelog\","
        )

        builder.appendLine(
            "      \"text\":\"### v1.2.0\\n- Added Release Agent\""
        )

        builder.appendLine(
            "    },"
        )

        builder.appendLine()

        builder.appendLine(
            "    {"
        )

        builder.appendLine(
            "      \"type\":\"write\","
        )

        builder.appendLine(
            "      \"path\":\"CHANGELOG.md\","
        )

        builder.appendLine(
            "      \"text\":\"...\""
        )

        builder.appendLine(
            "    }"
        )

        builder.appendLine(
            "  ]"
        )

        builder.appendLine(
            "}"
        )

        //--------------------------------------------------
        // Дополнительные правила
        //--------------------------------------------------

        builder.appendLine()

        builder.appendLine(
            "Правила:"
        )

        builder.appendLine()

        builder.appendLine(
            "- не придумывай новые пути"
        )

        builder.appendLine(
            "- используй только существующие пути проекта"
        )

        builder.appendLine(
            "- не удаляй существующий текст README"
        )

        builder.appendLine(
            "- не переписывай весь README"
        )

        builder.appendLine(
            "- сохраняй существующую структуру документа"
        )

        builder.appendLine(
            "- добавляй только новые разделы"
        )

        builder.appendLine(
            "- минимизируй изменения"
        )

        //--------------------------------------------------
        // Git
        //--------------------------------------------------

        builder.appendLine()

        builder.appendLine("================================")
        builder.appendLine("Git Branch")
        builder.appendLine("================================")
        builder.appendLine(gitBranch)

        builder.appendLine()

        builder.appendLine("================================")
        builder.appendLine("Git Status")
        builder.appendLine("================================")
        builder.appendLine(gitStatus)

        builder.appendLine()

        builder.appendLine("================================")
        builder.appendLine("Git Diff")
        builder.appendLine("================================")
        builder.appendLine(gitDiff)

        //--------------------------------------------------
        // Files
        //--------------------------------------------------

        builder.appendLine()

        builder.appendLine("================================")
        builder.appendLine("Project Files")
        builder.appendLine("================================")
        builder.appendLine(projectFiles)

        //--------------------------------------------------
        // Documentation
        //--------------------------------------------------

        builder.appendLine()

        builder.appendLine("================================")
        builder.appendLine("Documentation")
        builder.appendLine("================================")

        documentation.forEach {

            builder.appendLine()

            builder.appendLine(
                "File: ${it.fileName}"
            )

            builder.appendLine(
                "Section: ${it.section}"
            )

            builder.appendLine()

            builder.appendLine(
                it.text
            )

            builder.appendLine()

        }

        return builder.toString()

    }

}