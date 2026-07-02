package com.example.aichallenge

class FixedSizeChunker(

    private val chunkSize: Int = 500
) : ChunkingStrategy {

    override fun createChunks(
        document: Document
    ): List<Chunk> {

        val chunks = mutableListOf<Chunk>()

        var index = 0

        var chunkNumber = 1

        while (index < document.content.length) {

            val endIndex = minOf(
                index + chunkSize,
                document.content.length
            )

            val text = document.content.substring(
                index,
                endIndex
            )

            chunks.add(

                Chunk(

                    chunkId = "${document.fileName}_$chunkNumber",

                    source = document.source,

                    fileName = document.fileName,

                    title = document.title,

                    section = "",

                    text = text
                )
            )

            index = endIndex

            chunkNumber++
        }

        return chunks
    }
}