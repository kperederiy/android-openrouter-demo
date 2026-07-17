package com.example.mcp

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class CrmService {

    private val gson = Gson()

    //----------------------------------------------------
    // Файлы CRM
    //----------------------------------------------------

    private val usersFile =
        File("crm/users.json")

    private val ticketsFile =
        File("crm/tickets.json")

    //----------------------------------------------------
    // Пользователи
    //----------------------------------------------------

    fun loadUsers(): List<User> {

        if (!usersFile.exists()) {
            return emptyList()
        }

        val json = usersFile.readText()

        println(json)

        val type =
            object : TypeToken<List<User>>() {}.type

        return gson.fromJson(json, type)
    }

    //----------------------------------------------------
    // Тикеты
    //----------------------------------------------------

    fun loadTickets(): List<Ticket> {

        if (!ticketsFile.exists()) {
            return emptyList()
        }

        val json =
            ticketsFile.readText()

        val type =
            object : TypeToken<List<Ticket>>() {}.type

        return gson.fromJson(json, type)
    }

    //----------------------------------------------------
    // Найти пользователя
    //----------------------------------------------------

    fun findUser(
        id: Int
    ): User? {

        return loadUsers()

            .firstOrNull {

                it.id == id

            }

    }

    //----------------------------------------------------
    // Найти пользователя по email
    //----------------------------------------------------

    fun findUserByEmail(
        email: String
    ): User? {

        return loadUsers()

            .firstOrNull {

                it.email.equals(
                    email,
                    ignoreCase = true
                )

            }

    }

    //----------------------------------------------------
    // Все тикеты пользователя
    //----------------------------------------------------

    fun findTickets(
        userId: Int
    ): List<Ticket> {

        return loadTickets()

            .filter {

                it.userId == userId

            }

    }

    //----------------------------------------------------
    // Только открытые тикеты
    //----------------------------------------------------

    fun findOpenTickets(
        userId: Int
    ): List<Ticket> {

        return findTickets(userId)

            .filter {

                it.status == "OPEN"

            }

    }

    //----------------------------------------------------
    // Построить контекст пользователя
    //----------------------------------------------------

    fun buildUserContext(
        userId: Int
    ): String {

        val user =
            findUser(userId)
                ?: return "Пользователь не найден."

        val tickets =
            findOpenTickets(userId)

        val builder = StringBuilder()

        builder.appendLine("Пользователь")
        builder.appendLine()

        builder.appendLine("ID: ${user.id}")
        builder.appendLine("Имя: ${user.name}")
        builder.appendLine("Email: ${user.email}")
        builder.appendLine("Тариф: ${user.plan}")
        builder.appendLine()

        builder.appendLine("Открытые тикеты")
        builder.appendLine()

        if (tickets.isEmpty()) {

            builder.appendLine("нет")

        } else {

            tickets.forEach {

                builder.appendLine(
                    "#${it.id}"
                )

                builder.appendLine(
                    it.title
                )

                builder.appendLine(
                    "Приоритет: ${it.priority}"
                )

                builder.appendLine(
                    "Статус: ${it.status}"
                )

                builder.appendLine()
            }

        }

        return builder.toString()
    }

}