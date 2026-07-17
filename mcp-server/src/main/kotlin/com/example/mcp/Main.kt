package com.example.mcp

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import com.google.gson.Gson

fun main() {

    val gitService = GitService()

    val fileService = FileService()

    val crmService = CrmService()

    val router = McpRouter(

        gitService = gitService,

        fileService = fileService,

        crmService = crmService

    )

    val gson = Gson()

    val server = HttpServer.create(
        InetSocketAddress(8080),
        0
    )

    //----------------------------------------------------------
    // Главная страница
    //----------------------------------------------------------

    server.createContext("/") { exchange ->

        if (exchange.requestMethod != "GET") {
            sendResponse(
                exchange,
                405,
                "Method Not Allowed"
            )
            return@createContext
        }

        sendResponse(
            exchange,
            200,
            """
Developer MCP Server

Available endpoints:

POST /mcp

MCP Tools

git_branch
git_status
git_diff
list_files

crm_users
crm_tickets
crm_user_context
    """.trimIndent()
        )
    }

    //----------------------------------------------------------
    // Получить текущую Git-ветку
    //----------------------------------------------------------

    server.createContext("/branch") { exchange ->

        if (exchange.requestMethod != "GET") {
            sendResponse(
                exchange,
                405,
                "Method Not Allowed"
            )
            return@createContext
        }

        val branch = gitService.currentBranch()
        println("Current branch: $branch")

        val json =
            """
            {
                "branch":"$branch"
            }
            """.trimIndent()

        exchange.responseHeaders.add(
            "Content-Type",
            "application/json; charset=utf-8"
        )

        sendResponse(
            exchange,
            200,
            json
        )
    }

    server.createContext("/status") { exchange ->

        if (exchange.requestMethod != "GET") {

            sendResponse(exchange, 405, "Method Not Allowed")

            return@createContext
        }

        val result = gitService.status()

        sendResponse(
            exchange,
            200,
            result
        )

    }

    server.createContext("/diff") { exchange ->

        if (exchange.requestMethod != "GET") {

            sendResponse(exchange, 405, "Method Not Allowed")

            return@createContext
        }

        val result = gitService.diff()

        sendResponse(
            exchange,
            200,
            result
        )

    }

    server.createContext("/files") { exchange ->

        if (exchange.requestMethod != "GET") {

            sendResponse(exchange, 405, "Method Not Allowed")

            return@createContext
        }

        val result = fileService.listFiles()

        sendResponse(
            exchange,
            200,
            result
        )

    }

    //----------------------------------------------------------
// MCP
//----------------------------------------------------------

    server.createContext("/mcp") { exchange ->

        if (exchange.requestMethod != "POST") {

            sendResponse(
                exchange,
                405,
                "Method Not Allowed"
            )

            return@createContext
        }

        try {

            val body =

                exchange.requestBody
                    .bufferedReader()
                    .readText()

            println("MCP Request:")
            println(body)

            val request =

                gson.fromJson(
                    body,
                    McpRequest::class.java
                )

            val response =

                router.execute(request)

            val json =

                gson.toJson(response)

            exchange.responseHeaders.add(
                "Content-Type",
                "application/json; charset=utf-8"
            )

            sendResponse(
                exchange,
                200,
                json
            )

        } catch (e: Exception) {

            val json =

                gson.toJson(

                    McpResponse(
                        success = false,
                        result = e.message ?: "Unknown error"
                    )

                )

            sendResponse(
                exchange,
                500,
                json
            )
        }

    }

    server.executor = null

    server.start()

    println("----------------------------------------")
    println("Developer MCP Server started")
    println("http://localhost:8080")
    println("----------------------------------------")
}
private fun sendResponse(
    exchange: HttpExchange,
    code: Int,
    text: String
) {

    val bytes =
        text.toByteArray(
            StandardCharsets.UTF_8
        )

    exchange.sendResponseHeaders(
        code,
        bytes.size.toLong()
    )

    val os: OutputStream =
        exchange.responseBody

    os.write(bytes)

    os.close()
}