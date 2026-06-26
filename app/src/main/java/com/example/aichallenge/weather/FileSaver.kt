package com.example.aichallenge.weather

import android.content.Context
import java.io.File

class FileSaver(
    private val context: Context
) {

    fun save(
        content: String,
        filename: String
    ): Map<String, Any> {

        return try {

            val file = File(

                context.filesDir,

                filename.ifBlank {

                    "weather_report.txt"
                }
            )

            file.writeText(content)

            mapOf(

                "status" to "success",

                "content" to
                        "Файл успешно сохранен",

                "filePath" to
                        file.absolutePath
            )

        } catch (e: Exception) {

            mapOf(

                "status" to "error",

                "content" to
                        (e.message ?: "Ошибка")
            )
        }
    }
}