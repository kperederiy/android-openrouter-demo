package com.example.aichallenge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class BenchmarkStorage(

    private val context: Context

) {

    private val json = Json {

        prettyPrint = true

        ignoreUnknownKeys = true
    }

    suspend fun saveResults(

        results: List<BenchmarkResult>

    ): File = withContext(Dispatchers.IO) {

        val jsonText = json.encodeToString(results)

        val file = File(

            context.filesDir,

            "benchmark.json"
        )

        file.writeText(jsonText)

        file
    }

    suspend fun loadResults():

            List<BenchmarkResult> =

        withContext(Dispatchers.IO) {

            val file = File(

                context.filesDir,

                "benchmark.json"
            )

            if (!file.exists()) {

                return@withContext emptyList()
            }

            val jsonText = file.readText()

            json.decodeFromString(jsonText)
        }

    fun getBenchmarkFile(): File {

        return File(

            context.filesDir,

            "benchmark.json"
        )
    }

    fun benchmarkExists(): Boolean {

        return getBenchmarkFile().exists()
    }

    fun deleteBenchmark(): Boolean {

        val file = getBenchmarkFile()

        if (!file.exists()) {

            return true
        }

        return file.delete()
    }
}