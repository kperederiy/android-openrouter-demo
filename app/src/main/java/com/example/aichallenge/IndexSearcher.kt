package com.example.aichallenge

import android.content.Context

class IndexSearcher(
    private val context: Context,
    private val embeddingService: EmbeddingService
) {
    suspend fun search(
        question: String,
        topK: Int = 10
    ): List<SearchResult> {
        android.util.Log.d("IndexSearcher", "=== SEARCH START ===")
        android.util.Log.d("IndexSearcher", "Question: $question")

        val storage = IndexStorage(context)

        // Проверяем файл напрямую
        val indexFile = storage.getIndexFile()
        android.util.Log.d("IndexSearcher", "Index file exists: ${indexFile.exists()}")
        android.util.Log.d("IndexSearcher", "Index file path: ${indexFile.absolutePath}")

        if (!indexFile.exists()) {
            android.util.Log.e("IndexSearcher", "❌ Index file does not exist at: ${indexFile.absolutePath}")
            return emptyList()
        }

        android.util.Log.d("IndexSearcher", "Index file size: ${indexFile.length()} bytes")

        // Пытаемся загрузить
        val index = storage.loadIndex()

        if (index == null) {
            android.util.Log.e("IndexSearcher", "❌ Index is null after loadIndex()")
            return emptyList()
        }

        if (index.chunks.isEmpty()) {
            android.util.Log.e("IndexSearcher", "❌ Index has no chunks")
            return emptyList()
        }

        android.util.Log.d("IndexSearcher", "✅ Index loaded successfully")
        android.util.Log.d("IndexSearcher", "✅ Total chunks in index: ${index.chunks.size}")

        val questionEmbedding = embeddingService.createEmbedding(question)
        android.util.Log.d("IndexSearcher", "Question embedding size: ${questionEmbedding.size}")

        val results = index.chunks
            .map { chunk ->
                val similarity = CosineSimilarity.calculate(
                    questionEmbedding,
                    chunk.embedding
                )
                SearchResult(
                    chunk = chunk,
                    similarity = similarity
                )
            }
            .sortedByDescending { it.similarity }
            .take(topK)

        android.util.Log.d("IndexSearcher", "Returning ${results.size} results")
        if (results.isNotEmpty()) {
            android.util.Log.d("IndexSearcher", "Top similarity: ${results.first().similarity}")
        }

        return results
    }
}