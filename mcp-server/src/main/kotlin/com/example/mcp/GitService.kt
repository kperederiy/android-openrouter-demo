package com.example.mcp

import java.io.File
import java.util.concurrent.TimeUnit

class GitService {

    /**
     * Корень Git-репозитория.
     */
    private val projectDir = File(".").canonicalFile

    //----------------------------------------------------------
    // Текущая ветка
    //----------------------------------------------------------

    fun currentBranch(): String {

        return executeGit(
            "branch",
            "--show-current"
        )

    }

    //----------------------------------------------------------
    // Git Status
    //----------------------------------------------------------

    fun status(): String {

        return executeGit(
            "status",
            "--short"
        )

    }

    //----------------------------------------------------------
    // Git Diff
    //----------------------------------------------------------

    fun diff(): String {

        return executeGit(
            "diff",
            "--stat"
        )

    }

    //----------------------------------------------------------
    // Выполнить любую git-команду
    //----------------------------------------------------------

    private fun executeGit(
        vararg args: String
    ): String {

        return try {

            val command =

                mutableListOf("git").apply {
                    addAll(args)
                }

            println("Execute: ${command.joinToString(" ")}")

            val process =

                ProcessBuilder(command)
                    .directory(projectDir)
                    .redirectErrorStream(true)
                    .start()

            if (!process.waitFor(5, TimeUnit.SECONDS)) {

                process.destroyForcibly()

                return "Git command timeout"

            }

            val result =

                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()

            if (result.isBlank()) {

                "(empty)"

            } else {

                result

            }

        } catch (e: Exception) {

            "ERROR: ${e.message}"

        }

    }

}