package com.example.aichallenge

class AgentRouter {

    fun detectIntent(
        question: String
    ): AgentIntent {

        val q = question.lowercase().trim()

        //--------------------------------------------------
        // Явные команды
        //--------------------------------------------------

        if (q.startsWith("/review"))
            return AgentIntent.REVIEW

        if (q.startsWith("/help"))
            return AgentIntent.HELP

        if (q.startsWith("/support"))
            return AgentIntent.SUPPORT

        if (q.startsWith("/files"))
            return AgentIntent.FILES

        //--------------------------------------------------
        // Разработка
        //--------------------------------------------------

        if (

            q.contains("архитектур") ||

            q.contains("код") ||

            q.contains("класс") ||

            q.contains("файл") ||

            q.contains("git") ||

            q.contains("branch") ||

            q.contains("diff") ||

            q.contains("review") ||

            q.contains("mcp")

        ) {

            return AgentIntent.HELP

        }

        //--------------------------------------------------
        // Поддержка
        //--------------------------------------------------

        if (

            q.contains("авториза") ||

            q.contains("логин") ||

            q.contains("не работает") ||

            q.contains("ошибка") ||

            q.contains("не могу") ||

            q.contains("тикет") ||

            q.contains("аккаунт") ||

            q.contains("пароль")

        ) {

            return AgentIntent.SUPPORT

        }

        if (

            q.contains("readme") ||

            q.contains("changelog") ||

            q.contains("adr") ||

            q.contains("обнови файл") ||

            q.contains("создай файл") ||

            q.contains("найди использование") ||

            q.contains("проверь файлы")

        ) {

            return AgentIntent.FILES

        }

        //--------------------------------------------------

        return AgentIntent.RAG

    }

}