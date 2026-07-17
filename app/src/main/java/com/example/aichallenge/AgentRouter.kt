package com.example.aichallenge

class AgentRouter {

    fun detectAgent(
        question: String
    ): AgentType {

        val q = question.lowercase()

        return when {

            q.startsWith("/review") ->
                AgentType.REVIEW

            q.startsWith("/help") ->
                AgentType.DEVELOPER

            q.startsWith("/support") ->
                AgentType.SUPPORT

            else ->
                AgentType.CHAT
        }

    }

}