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
        // Добавляем для отладки
        encodeDefaults = true
        isLenient = true
    }

    suspend fun saveIndex(
        chunks: List<Chunk>
    ): File = withContext(Dispatchers.IO) {
        android.util.Log.d("IndexStorage", "=== SAVING INDEX ===")
        android.util.Log.d("IndexStorage", "Chunks to save: ${chunks.size}")

        if (chunks.isNotEmpty()) {
            android.util.Log.d("IndexStorage", "First chunk: ${chunks.first().chunkId}")
            android.util.Log.d("IndexStorage", "First chunk embedding size: ${chunks.first().embedding.size}")
        }

        val index = Index(chunks = chunks)
        val jsonText = json.encodeToString(index)

        android.util.Log.d("IndexStorage", "JSON size: ${jsonText.length} characters")

        val file = File(context.filesDir, "index.json")
        file.writeText(jsonText)

        android.util.Log.d("IndexStorage", "✅ File saved: ${file.absolutePath}")
        android.util.Log.d("IndexStorage", "✅ File size: ${file.length()} bytes")
        android.util.Log.d("IndexStorage", "=== SAVE COMPLETE ===")

        return@withContext file
    }

    suspend fun loadIndex(): Index? =
        withContext(Dispatchers.IO) {
            android.util.Log.d("IndexStorage", "=== LOADING INDEX ===")

            val file = File(context.filesDir, "index.json")
            android.util.Log.d("IndexStorage", "File path: ${file.absolutePath}")
            android.util.Log.d("IndexStorage", "File exists: ${file.exists()}")

            if (!file.exists()) {
                android.util.Log.e("IndexStorage", "❌ File does not exist!")
                return@withContext null
            }

            android.util.Log.d("IndexStorage", "File size: ${file.length()} bytes")

            try {
                val jsonText = file.readText()
                android.util.Log.d("IndexStorage", "Read ${jsonText.length} characters")

                if (jsonText.isEmpty()) {
                    android.util.Log.e("IndexStorage", "❌ File is empty!")
                    return@withContext null
                }

                // Проверяем начало JSON
                val preview = if (jsonText.length > 100) jsonText.take(100) else jsonText
                android.util.Log.d("IndexStorage", "JSON preview: $preview...")

                val index = json.decodeFromString<Index>(jsonText)
                android.util.Log.d("IndexStorage", "✅ Successfully decoded index")
                android.util.Log.d("IndexStorage", "✅ Chunks: ${index.chunks.size}")

                if (index.chunks.isNotEmpty()) {
                    val firstChunk = index.chunks.first()
                    android.util.Log.d("IndexStorage", "✅ First chunk: ${firstChunk.chunkId}")
                    android.util.Log.d("IndexStorage", "✅ First chunk embedding size: ${firstChunk.embedding.size}")
                }

                android.util.Log.d("IndexStorage", "=== LOAD COMPLETE ===")
                return@withContext index

            } catch (e: Exception) {
                android.util.Log.e("IndexStorage", "❌ Failed to decode index", e)
                android.util.Log.e("IndexStorage", "❌ Error: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }

    fun indexExists(): Boolean {
        val exists = File(context.filesDir, "index.json").exists()
        android.util.Log.d("IndexStorage", "indexExists(): $exists")
        return exists
    }

    fun deleteIndex(): Boolean {
        val file = getIndexFile()
        android.util.Log.d("IndexStorage", "deleteIndex(): ${file.absolutePath}")
        if (!file.exists()) {
            return true
        }
        return file.delete()
    }

    fun getIndexFile(): File {
        return File(context.filesDir, "index.json")
    }
}