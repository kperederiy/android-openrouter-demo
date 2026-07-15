package com.example.mcp

import java.io.File

class FileService {

    private val projectDir =
        File(".").canonicalFile

    /**
     * Возвращает список файлов проекта.
     */
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

}