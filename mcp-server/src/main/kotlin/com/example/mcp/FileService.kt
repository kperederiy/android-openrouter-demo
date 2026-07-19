package com.example.mcp

import java.io.File

class FileService {

    private val projectDir =
        File(".").canonicalFile

    //----------------------------------------------------
    // Список файлов проекта
    //----------------------------------------------------

    fun listFiles(): String {

        val builder = StringBuilder()

        projectDir
            .walkTopDown()
            .filter { it.isFile }
            .filterNot { it.path.contains(".git") }
            .filterNot { it.path.contains("build") }
            .filterNot { it.path.contains(".gradle") }
            .forEach {

                builder.append(

                    projectDir
                        .toPath()
                        .relativize(it.toPath())
                        .toString()

                )

                builder.append("\n")
            }

        return builder.toString()
    }

    //----------------------------------------------------
    // Чтение файла
    //----------------------------------------------------

    fun readFile(
        path: String
    ): String {

        if (path.isBlank()) {
            return "Путь не указан."
        }

        val file =
            File(projectDir, path)

        if (!file.exists()) {
            return "Файл не найден."
        }

        if (!file.isFile) {
            return "Это не файл."
        }

        return file.readText()
    }

    //----------------------------------------------------
    // Поиск текста по проекту
    //----------------------------------------------------

    fun searchText(
        text: String
    ): String {

        if (text.isBlank()) {
            return "Текст поиска пуст."
        }

        val builder = StringBuilder()

        projectDir
            .walkTopDown()
            .filter { it.isFile }
            .filterNot { it.path.contains(".git") }
            .filterNot { it.path.contains("build") }
            .filterNot { it.path.contains(".gradle") }
            .forEach { file ->

                val lines =
                    file.readLines()

                lines.forEachIndexed { index, line ->

                    if (line.contains(text, true)) {

                        builder.appendLine(
                            projectDir.toPath()
                                .relativize(file.toPath())
                        )

                        builder.appendLine(
                            "строка ${index + 1}"
                        )

                        builder.appendLine(
                            line.trim()
                        )

                        builder.appendLine(
                            "--------------------------------"
                        )
                    }

                }

            }

        if (builder.isEmpty()) {

            return "Совпадений не найдено."
        }

        return builder.toString()
    }

    //----------------------------------------------------
    // Создание или перезапись файла
    //----------------------------------------------------

    fun writeFile(

        path: String,

        content: String

    ): String {

        if (path.isBlank()) {
            return "Путь не указан."
        }

        val file =
            File(projectDir, path)

        file.parentFile?.mkdirs()

        file.writeText(content)

        return buildString {

            appendLine("Файл сохранён")

            appendLine()

            appendLine(
                projectDir.toPath()
                    .relativize(file.toPath())
            )

        }
    }

    //----------------------------------------------------
    // Замена текста
    //----------------------------------------------------

    fun replaceText(

        path: String,

        oldText: String,

        newText: String

    ): String {

        if (path.isBlank()) {
            return "Путь не указан."
        }

        val file =
            File(projectDir, path)

        if (!file.exists()) {
            return "Файл не найден."
        }

        val source =
            file.readText()

        if (!source.contains(oldText)) {

            return "Текст для замены не найден."
        }

        val updated =

            source.replace(

                oldText,

                newText

            )

        file.writeText(updated)

        return buildString {

            appendLine("Файл обновлён")

            appendLine()

            appendLine(
                projectDir.toPath()
                    .relativize(file.toPath())
            )

        }
    }

    fun updateFile(

        path: String,

        newContent: String

    ): String {

        val file = File(projectDir, path)

        if (!file.exists()) {

            return "Файл не найден."
        }

        val before = file.readText()

        file.writeText(newContent)

        return buildDiff(

            path = path,

            before = before,

            after = newContent

        )
    }

    private fun buildDiff(

        path: String,

        before: String,

        after: String

    ): String {

        val oldLines = before.lines()

        val newLines = after.lines()

        val builder = StringBuilder()

        builder.appendLine("diff -- $path")
        builder.appendLine()

        val max = maxOf(

            oldLines.size,

            newLines.size

        )

        for (i in 0 until max) {

            val oldLine = oldLines.getOrNull(i)
            val newLine = newLines.getOrNull(i)

            when {

                oldLine == newLine ->

                    builder.appendLine(" $oldLine")

                oldLine == null ->

                    builder.appendLine("+$newLine")

                newLine == null ->

                    builder.appendLine("-$oldLine")

                else -> {

                    builder.appendLine("-$oldLine")
                    builder.appendLine("+$newLine")

                }

            }

        }

        return builder.toString()

    }

}