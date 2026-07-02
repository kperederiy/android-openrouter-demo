package com.example.aichallenge

class ParagraphChunker : ChunkingStrategy {

    override fun createChunks(
        document: Document
    ): List<Chunk> {

        val chunks = mutableListOf<Chunk>()

        val paragraphs = document.content
            .split("\n\n")

        var chunkNumber = 1

        for (paragraph in paragraphs) {

            val text = paragraph.trim()

            if (text.isBlank()) {
                continue
            }

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

            chunkNumber++
        }

        return chunks
    }
}