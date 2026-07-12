package com.example.aichallenge

class ConversationHistory(

    private val maxMessages: Int = 20

) {

    private val messages = mutableListOf<ChatMessage>()

    fun clear() {

        messages.clear()
    }

    fun addUser(

        text: String

    ) {

        messages.add(

            ChatMessage(

                role = "user",

                text = text
            )
        )

        trim()
    }

    fun addAssistant(

        text: String

    ) {

        messages.add(

            ChatMessage(

                role = "assistant",

                text = text
            )
        )

        trim()
    }

    fun getMessages(): List<ChatMessage> {

        return messages.toList()
    }

    private fun trim() {

        while (messages.size > maxMessages) {

            messages.removeAt(0)
        }
    }
}