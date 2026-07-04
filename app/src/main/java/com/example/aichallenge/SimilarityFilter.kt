package com.example.aichallenge

class SimilarityFilter(

    private val threshold: Double = 0.3,  // Снижен с 0.75 до 0.3

    private val topKAfter: Int = 5        // Увеличен с 3 до 5

) {

    fun filter(

        results: List<SearchResult>

    ): List<SearchResult> {

        if (results.isEmpty()) {

            return emptyList()
        }

        // Логируем результаты для отладки
        results.forEach { result ->
            android.util.Log.d(
                "SimilarityFilter",
                "Chunk: ${result.chunk.chunkId}, Similarity: ${result.similarity}"
            )
        }

        return results

            .filter {

                it.similarity >= threshold
            }

            .sortedByDescending {

                it.similarity
            }

            .take(topKAfter)
    }
}