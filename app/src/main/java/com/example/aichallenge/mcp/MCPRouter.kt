package com.example.aichallenge.mcp

import android.util.Log

/**
 * Маршрутизатор для MCP-серверов
 * Поддерживает префиксы: "weather.*" → WeatherMCPServer, "notify.*" → NotificationMCPServer
 */
class MCPRouter {

    companion object {
        private const val TAG = "MCPRouter"
    }

    private val servers = mutableMapOf<String, Any>()
    private val toolToServerMap = mutableMapOf<String, String>()

    /**
     * Регистрация сервера
     */
    fun register(prefix: String, server: Any) {
        servers[prefix] = server
        Log.d(TAG, "Зарегистрирован сервер с префиксом: $prefix")
    }

    /**
     * Регистрация инструментов для сервера
     */
    fun registerTools(prefix: String, tools: List<String>) {
        tools.forEach { tool ->
            val fullToolName = "$prefix.$tool"
            toolToServerMap[fullToolName] = prefix
            Log.d(TAG, "Зарегистрирован инструмент: $fullToolName")
        }
    }

    /**
     * Вызов инструмента через маршрутизатор
     */
    suspend fun callTool(toolName: String, arguments: Map<String, Any>): Map<String, Any> {
        return try {
            Log.d(TAG, "Вызов инструмента: $toolName")

            // Определяем префикс
            val prefix = toolToServerMap[toolName]
                ?: toolName.split(".").firstOrNull()
                ?: return mapOf(
                    "status" to "error",
                    "content" to "Неизвестный инструмент: $toolName"
                )

            // Получаем сервер
            val server = servers[prefix]
                ?: return mapOf(
                    "status" to "error",
                    "content" to "Сервер не найден для префикса: $prefix"
                )

            // Вызываем метод handleRequest на сервере
            when (server) {
                is WeatherMCPServer -> {
                    val method = if (toolName.contains(".")) {
                        "tools/call"
                    } else {
                        toolName
                    }
                    server.handleRequest(method, mapOf(
                        "name" to toolName.substringAfterLast("."),
                        "arguments" to arguments
                    ))
                }
                is NotificationMCPServer -> {
                    val method = if (toolName.contains(".")) {
                        "tools/call"
                    } else {
                        toolName
                    }
                    server.handleRequest(method, mapOf(
                        "name" to toolName.substringAfterLast("."),
                        "arguments" to arguments
                    ))
                }
                else -> {
                    mapOf(
                        "status" to "error",
                        "content" to "Неизвестный тип сервера"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка вызова инструмента $toolName", e)
            mapOf(
                "status" to "error",
                "content" to "Ошибка: ${e.message}"
            )
        }
    }

    /**
     * Проверка доступности сервера
     */
    fun isServerAvailable(prefix: String): Boolean {
        return servers.containsKey(prefix)
    }

    /**
     * Получение списка зарегистрированных инструментов
     */
    fun getRegisteredTools(): List<String> {
        return toolToServerMap.keys.toList()
    }
}