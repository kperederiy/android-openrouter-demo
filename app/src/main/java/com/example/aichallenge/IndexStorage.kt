package com.example.aichallenge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.serialization.decodeFromString

class IndexStorage(

    private val context: Context
) {

    private val json = Json {

        prettyPrint = true

        ignoreUnknownKeys = true
    }

    suspend fun saveIndex(

        chunks: List<Chunk>

    ): File = withContext(Dispatchers.IO) {

        val index = Index(

            chunks = chunks
        )

        val jsonText = json.encodeToString(index)

        val file = File(

            context.filesDir,

            "index.json"
        )

        file.writeText(jsonText)

        file
    }

    suspend fun loadIndex(): Index? =
        withContext(Dispatchers.IO) {

            val file = File(

                context.filesDir,

                "index.json"
            )

            if (!file.exists()) {

                return@withContext null
            }

            val jsonText = file.readText()

            json.decodeFromString<Index>(jsonText)
        }

    fun indexExists(): Boolean {

        return File(

            context.filesDir,

            "index.json"

        ).exists()
    }

    fun deleteIndex(): Boolean {

        val file = getIndexFile()

        if (!file.exists()) {
            return true
        }

        return file.delete()
    }

    fun getIndexFile(): File {

        return File(

            context.filesDir,

            "index.json"
        )
    }
}