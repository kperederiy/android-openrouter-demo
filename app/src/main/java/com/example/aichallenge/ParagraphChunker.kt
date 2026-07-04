package com.example.aichallenge

class ParagraphChunker : ChunkingStrategy {

    override fun createChunks(
        document: Document
    ): List<Chunk> {

        val chunks = mutableListOf<Chunk>()

        // Разбиваем по двойным переносам строки или по заголовкам
        val paragraphs = document.content
            .split(Regex("\n\\s*\n"))

        var chunkNumber = 1
        var currentSection = ""

        for (paragraph in paragraphs) {

            val text = paragraph.trim()

            if (text.isBlank()) {
                continue
            }

            // Проверяем, является ли абзац заголовком
            if (text.startsWith("#")) {
                currentSection = text.replace(Regex("^#+\\s*"), "").trim()
                // Заголовки тоже сохраняем как отдельные чанки
            }

            chunks.add(

                Chunk(

                    chunkId = "${document.fileName}_$chunkNumber",

                    source = document.source,

                    fileName = document.fileName,

                    title = document.title,

                    section = currentSection,  // Сохраняем текущую секцию

                    text = text
                )
            )

            chunkNumber++
        }

        android.util.Log.d(
            "ParagraphChunker",
            "Created ${chunks.size} chunks for ${document.fileName}"
        )

        return chunks
    }
}