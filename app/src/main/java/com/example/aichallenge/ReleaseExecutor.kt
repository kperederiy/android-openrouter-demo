package com.example.aichallenge

class ReleaseExecutor(

    private val mcpClient: McpClient

) {

    fun execute(

        plan: ReleasePlan,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val report = StringBuilder()

        executeOperation(

            operations = plan.operations,

            index = 0,

            report = report,

            onSuccess = onSuccess,

            onError = onError

        )

    }

    //--------------------------------------------------
    // Выполнение операций
    //--------------------------------------------------

    private fun executeOperation(

        operations: List<ReleaseOperation>,

        index: Int,

        report: StringBuilder,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        if (index >= operations.size) {

            finish(

                report,

                onSuccess,

                onError

            )

            return

        }

        val operation = operations[index]

        val path = normalizePath(operation.path)

        val callback: (String) -> Unit = { result ->

            report.appendLine("================================")
            report.appendLine(operation.type.uppercase())
            report.appendLine("================================")
            report.appendLine()

            report.appendLine("Path:")
            report.appendLine(path)
            report.appendLine()

            report.appendLine(result)
            report.appendLine()

            executeOperation(

                operations,

                index + 1,

                report,

                onSuccess,

                onError

            )

        }

        when (operation.type.lowercase()) {

            //--------------------------------------------------
            // Создание файла
            //--------------------------------------------------

            "write" ->

                mcpClient.writeFile(

                    path = path,

                    content = operation.text,

                    onSuccess = callback,

                    onError = onError

                )

            //--------------------------------------------------
            // Полная замена файла
            //--------------------------------------------------

            "update" ->

                mcpClient.updateFile(

                    path = path,

                    content = operation.text,

                    onSuccess = callback,

                    onError = onError

                )

            //--------------------------------------------------
            // Замена текста
            //--------------------------------------------------

            "replace" ->

                mcpClient.replaceText(

                    path = path,

                    oldText = operation.find,

                    newText = operation.replace,

                    onSuccess = callback,

                    onError = onError

                )

            //--------------------------------------------------
            // Вставка после текста
            //--------------------------------------------------

            "append_after" ->

                mcpClient.appendAfter(

                    path = path,

                    after = operation.after,

                    text = operation.text,

                    onSuccess = callback,

                    onError = onError

                )

            //--------------------------------------------------

            else -> {

                report.appendLine(

                    "Неизвестная операция: ${operation.type}"

                )

                report.appendLine()

                executeOperation(

                    operations,

                    index + 1,

                    report,

                    onSuccess,

                    onError

                )

            }

        }

    }

    //--------------------------------------------------
    // Нормализация путей
    //--------------------------------------------------

    private fun normalizePath(

        path: String

    ): String {

        val fileName =

            path.substringAfterLast('/')

        return when (fileName) {

            "README.md" ->

                "app/src/main/assets/README.md"

            "CHANGELOG.md" ->

                "app/src/main/assets/CHANGELOG.md"

            "RELEASE_NOTES.md" ->

                "app/src/main/assets/RELEASE_NOTES.md"

            else ->

                path

        }

    }

    //--------------------------------------------------
    // Финальный git diff
    //--------------------------------------------------

    private fun finish(

        report: StringBuilder,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        mcpClient.getDiff(

            onSuccess = { diff ->

                report.appendLine()

                report.appendLine("================================")
                report.appendLine("Git Diff")
                report.appendLine("================================")
                report.appendLine()

                report.appendLine(diff)

                onSuccess(

                    report.toString()

                )

            },

            onError = onError

        )

    }

}