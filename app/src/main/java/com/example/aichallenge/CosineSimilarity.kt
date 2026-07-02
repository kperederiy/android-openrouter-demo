package com.example.aichallenge

import kotlin.math.sqrt

object CosineSimilarity {

    fun calculate(

        vectorA: List<Float>,

        vectorB: List<Float>

    ): Double {

        if (vectorA.isEmpty()) {
            return 0.0
        }

        if (vectorB.isEmpty()) {
            return 0.0
        }

        if (vectorA.size != vectorB.size) {
            return 0.0
        }

        var dotProduct = 0.0

        var normA = 0.0

        var normB = 0.0

        for (i in vectorA.indices) {

            dotProduct +=
                vectorA[i] * vectorB[i]

            normA +=
                vectorA[i] * vectorA[i]

            normB +=
                vectorB[i] * vectorB[i]
        }

        if (normA == 0.0) {
            return 0.0
        }

        if (normB == 0.0) {
            return 0.0
        }

        return dotProduct /
                (sqrt(normA) * sqrt(normB))
    }
}