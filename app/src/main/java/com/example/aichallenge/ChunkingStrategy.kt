package com.example.aichallenge

interface ChunkingStrategy {

    fun createChunks(
        document: Document
    ): List<Chunk>
}