package com.example.mcp

import java.io.File

class FileService {

    private val projectDir =
        File(".").canonicalFile

    //----------------------------------------------------
    // Список файлов проекта
    //----------------------------------------------------

    fun listFiles(): String {

        return projectDir
            .walkTopDown()
            .filter { it.isFile }
            .filterNot { it.path.contains(".git") }
            .filterNot { it.path.contains("build") }
            .filterNot { it.path.contains(".gradle") }
            .joinToString("\n") {

                projectDir
                    .toPath()
                    .relativize(it.toPath())
                    .toString()

            }

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
    // Поиск текста
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

                file.readLines()
                    .forEachIndexed { index, line ->

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

                            builder.appendLine("--------------------------------")

                        }

                    }

            }

        return if (builder.isEmpty()) {

            "Совпадений не найдено."

        } else {

            builder.toString()

        }

    }

    //----------------------------------------------------
    // Создать файл
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

            appendLine("Файл создан")
            appendLine()
            appendLine(path)

        }

    }

    //----------------------------------------------------
    // Полная замена файла
    //----------------------------------------------------

    fun updateFile(
        path: String,
        newContent: String
    ): String {

        val file =
            File(projectDir, path)

        if (!file.exists()) {
            return "Файл не найден."
        }

        val before =
            file.readText()

        file.writeText(newContent)

        return buildDiff(
            path,
            before,
            newContent
        )

    }

    //----------------------------------------------------
    // Заменить текст
    //----------------------------------------------------

    fun replaceText(
        path: String,
        oldText: String,
        newText: String
    ): String {

        val file =
            File(projectDir, path)

        if (!file.exists()) {
            return "Файл не найден."
        }

        val before =
            file.readText()

        if (!before.contains(oldText)) {

            return "Фрагмент не найден."

        }

        val after =
            before.replace(
                oldText,
                newText
            )

        file.writeText(after)

        return buildDiff(
            path,
            before,
            after
        )

    }

    //----------------------------------------------------
    // Вставить после текста
    //----------------------------------------------------

    fun insertAfter(
        path: String,
        afterText: String,
        text: String
    ): String {

        val file =
            File(projectDir, path)

        if (!file.exists()) {
            return "Файл не найден."
        }

        val before =
            file.readText()

        val index =
            before.indexOf(afterText)

        if (index == -1) {
            return "Фрагмент не найден."
        }

        val insertPosition =
            index + afterText.length

        val result =
            before.substring(0, insertPosition) +
                    text +
                    before.substring(insertPosition)

        file.writeText(result)

        return buildDiff(
            path,
            before,
            result
        )

    }

    //----------------------------------------------------
    // Вставить перед текстом
    //----------------------------------------------------

    fun insertBefore(
        path: String,
        beforeText: String,
        text: String
    ): String {

        val file =
            File(projectDir, path)

        if (!file.exists()) {
            return "Файл не найден."
        }

        val before =
            file.readText()

        val index =
            before.indexOf(beforeText)

        if (index == -1) {
            return "Фрагмент не найден."
        }

        val result =
            before.substring(0, index) +
                    text +
                    before.substring(index)

        file.writeText(result)

        return buildDiff(
            path,
            before,
            result
        )

    }

    //----------------------------------------------------
    // Удалить блок
    //----------------------------------------------------

    fun deleteText(
        path: String,
        text: String
    ): String {

        val file =
            File(projectDir, path)

        if (!file.exists()) {
            return "Файл не найден."
        }

        val before =
            file.readText()

        if (!before.contains(text)) {
            return "Фрагмент не найден."
        }

        val after =
            before.replace(text, "")

        file.writeText(after)

        return buildDiff(
            path,
            before,
            after
        )

    }

    //----------------------------------------------------
    // Построение diff
    //----------------------------------------------------

    private fun buildDiff(
        path: String,
        before: String,
        after: String
    ): String {

        val oldLines =
            before.lines()

        val newLines =
            after.lines()

        val builder =
            StringBuilder()

        builder.appendLine("diff -- $path")
        builder.appendLine()

        val max =
            maxOf(
                oldLines.size,
                newLines.size
            )

        for (i in 0 until max) {

            val oldLine =
                oldLines.getOrNull(i)

            val newLine =
                newLines.getOrNull(i)

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

    //----------------------------------------------------
// Вставка текста после указанного блока
//----------------------------------------------------

    fun appendAfter(

        path: String,

        after: String,

        text: String

    ): String {

        if (path.isBlank()) {

            return "Путь не указан."

        }


        val file = File(

            projectDir,

            path

        )


        if (!file.exists()) {

            return "Файл не найден."

        }


        val source = file.readText()


        if (after.isBlank()) {

            return "Точка вставки не указана."

        }


        if (!source.contains(after)) {

            return buildString {

                appendLine(
                    "Текст для вставки не найден."
                )

                appendLine()

                appendLine(
                    "Искомый фрагмент:"
                )

                appendLine(after)

            }

        }


        val before = source


        val updated = source.replace(

            after,

            after + "\n" + text

        )


        file.writeText(updated)


        return buildDiff(

            path = path,

            before = before,

            after = updated

        )

    }

}