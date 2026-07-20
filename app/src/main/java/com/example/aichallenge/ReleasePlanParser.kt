package com.example.aichallenge

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class ReleasePlanParser {

    private val gson = Gson()

    fun parse(
        response: String
    ): ReleasePlan {

        return try {

            val json = extractJson(response)

            gson.fromJson(
                json,
                ReleasePlan::class.java
            )

        } catch (e: Exception) {

            ReleasePlan()

        }

    }

    //--------------------------------------------------
    // Извлекаем JSON из ответа LLM
    //--------------------------------------------------

    private fun extractJson(
        text: String
    ): String {

        var source = text

        //--------------------------------------------
        // Убираем markdown
        //--------------------------------------------

        source = source
            .replace("```json", "")
            .replace("```JSON", "")
            .replace("```Json", "")
            .replace("```", "")
            .trim()

        //--------------------------------------------
        // Ищем первый {
        //--------------------------------------------

        val start = source.indexOf('{')

        if (start == -1) {

            throw IllegalArgumentException(
                "JSON not found"
            )

        }

        //--------------------------------------------
        // Ищем закрывающую }
        //--------------------------------------------

        var balance = 0

        for (i in start until source.length) {

            when (source[i]) {

                '{' -> balance++

                '}' -> {

                    balance--

                    if (balance == 0) {

                        return source.substring(
                            start,
                            i + 1
                        )

                    }

                }

            }

        }

        throw JsonSyntaxException(
            "Invalid JSON"
        )

    }

}