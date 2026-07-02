package com.example.aichallenge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IndexBuilder(

    private val documentLoader: DocumentLoader,

    private val chunkingStrategy: ChunkingStrategy,

    private val embeddingService: EmbeddingService
) {

    suspend fun buildIndex(
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<Chunk> =
        withContext(Dispatchers.IO) {

            val index = mutableListOf<Chunk>()

            val documents = documentLoader.loadDocuments()

            val allChunks = mutableListOf<Chunk>()

            for (document in documents) {
                allChunks += chunkingStrategy.createChunks(document)
            }

            val total = allChunks.size

            allChunks.forEachIndexed { indexInList, chunk ->

                val chunkWithEmbedding =
                    embeddingService.createEmbedding(chunk)

                index.add(chunkWithEmbedding)

                onProgress(indexInList + 1, total)
            }

            index
        }
}