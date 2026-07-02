package com.example.aichallenge

import android.content.Context

class DocumentLoader(

    private val context: Context
) {

    fun loadDocuments(): List<Document> {

        val documents = mutableListOf<Document>()

        val assetFiles = context.assets.list("") ?: emptyArray()

        for (fileName in assetFiles) {

            if (!fileName.endsWith(".md")) {
                continue
            }

            val content = context.assets.open(fileName)
                .bufferedReader()
                .use {
                    it.readText()
                }

            val title = fileName.removeSuffix(".md")

            documents.add(

                Document(

                    fileName = fileName,

                    source = "assets/$fileName",

                    title = title,

                    content = content
                )
            )
        }

        return documents
    }
}