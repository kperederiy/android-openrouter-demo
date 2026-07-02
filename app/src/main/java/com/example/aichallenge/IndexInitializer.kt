package com.example.aichallenge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IndexInitializer(

    private val context: Context,

    private val documentLoader: DocumentLoader,

    private val chunkingStrategy: ChunkingStrategy,

    private val embeddingService: EmbeddingService
) {

    suspend fun initialize(

        forceRebuild: Boolean = false,

        onProgress: (Int, Int) -> Unit = { _, _ -> }

    ) = withContext(Dispatchers.IO) {

        val storage = IndexStorage(context)

        if (forceRebuild) {

            storage.deleteIndex()
        }

        if (storage.indexExists()) {
            return@withContext
        }

        val builder = IndexBuilder(
            documentLoader,
            chunkingStrategy,
            embeddingService
        )

        val chunks = builder.buildIndex(onProgress)

        storage.saveIndex(chunks)
    }
}