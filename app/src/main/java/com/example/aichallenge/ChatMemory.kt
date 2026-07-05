package com.example.aichallenge

class ChatMemory(

    private val maxMessages: Int = 10

) {

    private val messages = mutableListOf<ChatMessage>()

    fun addUserMessage(

        text: String

    ) {

        messages.add(

            ChatMessage(

                role = "USER",

                text = text
            )
        )

        trim()
    }

    fun addAssistantMessage(

        text: String

    ) {

        messages.add(

            ChatMessage(

                role = "ASSISTANT",

                text = text
            )
        )

        trim()
    }

    fun getMessages(): List<ChatMessage> {

        return messages.toList()
    }

    fun clear() {

        messages.clear()
    }

    private fun trim() {

        while (messages.size > maxMessages) {

            messages.removeAt(0)
        }
    }
}