package com.example.mcp

class CrmTools(

    private val crmService: CrmService

) {

    //--------------------------------------------------
    // Карточка пользователя
    //--------------------------------------------------

    fun user(): String {

        return crmService.buildUserContext(1)

    }

    //--------------------------------------------------
    // Только тикеты
    //--------------------------------------------------

    fun tickets(): String {

        val tickets =
            crmService.findOpenTickets(1)

        if (tickets.isEmpty()) {

            return "Открытых тикетов нет."

        }

        val builder = StringBuilder()

        builder.appendLine("Открытые тикеты")
        builder.appendLine()

        tickets.forEach {

            builder.appendLine(
                "#${it.id}"
            )

            builder.appendLine(
                it.title
            )

            builder.appendLine(
                "Priority: ${it.priority}"
            )

            builder.appendLine(
                "Status: ${it.status}"
            )

            builder.appendLine()

        }

        return builder.toString()

    }

}