package com.example.aichallenge

class SimilarityFilter(

    private val threshold: Double = 0.75,

    private val topKAfter: Int = 3

) {

    fun filter(

        results: List<SearchResult>

    ): List<SearchResult> {

        if (results.isEmpty()) {

            return emptyList()
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