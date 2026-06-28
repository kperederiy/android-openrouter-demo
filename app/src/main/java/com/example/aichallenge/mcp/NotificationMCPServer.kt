package com.example.aichallenge.mcp

import android.content.Context
import android.util.Log
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP-сервер для уведомлений и оповещений
 */
class NotificationMCPServer(
    private val context: Context
) {

    companion object {
        private const val TAG = "NotificationMCPServer"
    }

    // Хранилище запланированных оповещений
    private val scheduledAlerts = ConcurrentHashMap<String, ScheduledAlert>()

    // Хранилище истории уведомлений
    private val notificationHistory = mutableListOf<NotificationRecord>()

    /**
     * Обработка запросов к серверу
     */
    suspend fun handleRequest(method: String, params: Map<String, Any>): Map<String, Any> {
        return try {
            Log.d(TAG, "Обработка запроса: $method")

            // Извлекаем имя инструмента без префикса
            val toolName = when {
                method == "tools/call" -> {
                    val name = params["name"] as? String ?: ""
                    name.substringAfterLast(".")
                }
                else -> method
            }

            when (method) {
                "tools/call" -> {
                    val arguments = params["arguments"] as Map<String, Any>

                    when (toolName) {
                        "checkAlerts" -> checkAlertsTool(arguments)
                        "sendNotification" -> sendNotificationTool(arguments)
                        "scheduleAlert" -> scheduleAlertTool(arguments)
                        "getHistory" -> getHistoryTool(arguments)
                        else -> mapOf(
                            "status" to "error",
                            "content" to "Неизвестный инструмент: $toolName"
                        )
                    }
                }
                else -> mapOf(
                    "status" to "error",
                    "content" to "Неизвестный метод: $method"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обработки запроса", e)
            mapOf(
                "status" to "error",
                "content" to "Ошибка: ${e.message}"
            )
        }
    }

    /**
     * Инструмент: проверка опасных явлений
     */
    private fun checkAlertsTool(args: Map<String, Any>): Map<String, Any> {
        return try {
            Log.d(TAG, "checkAlerts вызван с аргументами: $args")

            val data = args["data"] as? List<*> ?: emptyList<Any>()
            val temperature = args["temperature"]?.toString()?.toDoubleOrNull() ?: 0.0
            val condition = args["condition"]?.toString() ?: ""

            val alerts = mutableListOf<String>()
            val alertLevel = when {
                temperature > 35 -> {
                    alerts.add("☀️ ОПАСНО: Экстремальная жара (>35°C)")
                    "red"
                }
                temperature > 30 -> {
                    alerts.add("⚠️ Внимание: Сильная жара (>30°C)")
                    "yellow"
                }
                temperature < -20 -> {
                    alerts.add("❄️ ОПАСНО: Экстремальный мороз (<-20°C)")
                    "red"
                }
                temperature < -10 -> {
                    alerts.add("⚠️ Внимание: Сильный мороз (<-10°C)")
                    "yellow"
                }
                condition.contains("гроза", ignoreCase = true) -> {
                    alerts.add("⛈️ ОПАСНО: Гроза")
                    "red"
                }
                condition.contains("ураган", ignoreCase = true) ||
                        condition.contains("шторм", ignoreCase = true) -> {
                    alerts.add("🌪️ ОПАСНО: Ураган/Шторм")
                    "red"
                }
                condition.contains("дождь", ignoreCase = true) &&
                        temperature < 5 -> {
                    alerts.add("⚠️ Внимание: Возможен гололёд")
                    "yellow"
                }
                condition.contains("снег", ignoreCase = true) -> {
                    alerts.add("❄️ Внимание: Снегопад")
                    "yellow"
                }
                else -> {
                    // Нет опасных явлений
                    "green"
                }
            }

            // Проверка данных из списка
            if (data.isNotEmpty()) {
                data.forEach { item ->
                    if (item is Map<*, *>) {
                        val temp = (item["temperature"] as? Number)?.toDouble() ?: 0.0
                        val cond = item["condition"]?.toString() ?: ""

                        if (temp > 35) {
                            alerts.add("☀️ ОПАСНО: Экстремальная жара (${temp}°C)")
                        } else if (temp > 30) {
                            alerts.add("⚠️ Внимание: Сильная жара (${temp}°C)")
                        } else if (temp < -20) {
                            alerts.add("❄️ ОПАСНО: Экстремальный мороз (${temp}°C)")
                        } else if (temp < -10) {
                            alerts.add("⚠️ Внимание: Сильный мороз (${temp}°C)")
                        }

                        if (cond.contains("гроза", ignoreCase = true)) {
                            alerts.add("⛈️ ОПАСНО: Гроза")
                        }
                        if (cond.contains("ураган", ignoreCase = true) ||
                            cond.contains("шторм", ignoreCase = true)) {
                            alerts.add("🌪️ ОПАСНО: Ураган/Шторм")
                        }
                    }
                }
            }

            val alertsResult = if (alerts.isEmpty()) {
                listOf("✅ Опасных явлений не обнаружено")
            } else {
                alerts.distinct()
            }

            mapOf(
                "status" to "success",
                "content" to "Проверка опасных явлений завершена",
                "alerts" to alertsResult,
                "alertLevel" to alertLevel,
                "hasAlerts" to alerts.isNotEmpty(),
                "alertCount" to alertsResult.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в checkAlerts", e)
            mapOf(
                "status" to "error",
                "content" to "Ошибка проверки опасных явлений: ${e.message}"
            )
        }
    }

    /**
     * Инструмент: отправка уведомления
     */
    private fun sendNotificationTool(args: Map<String, Any>): Map<String, Any> {
        return try {
            Log.d(TAG, "sendNotification вызван с аргументами: $args")

            val message = args["message"]?.toString() ?: "Без сообщения"
            val title = args["title"]?.toString() ?: "Погодное оповещение"
            val priority = args["priority"]?.toString() ?: "normal"
            val timestamp = System.currentTimeMillis()

            // Создаем запись уведомления
            val notification = NotificationRecord(
                timestamp = timestamp,
                title = title,
                message = message,
                priority = priority
            )

            // Сохраняем в историю
            notificationHistory.add(notification)

            Log.d(TAG, "Отправлено уведомление: $title - $message")

            // Здесь можно добавить реальную отправку уведомления
            // Например, через NotificationManager или Toast

            mapOf(
                "status" to "success",
                "content" to "✅ Уведомление отправлено",
                "notification" to notification,
                "timestamp" to timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в sendNotification", e)
            mapOf(
                "status" to "error",
                "content" to "Ошибка отправки уведомления: ${e.message}"
            )
        }
    }

    /**
     * Инструмент: планирование оповещения
     */
    private fun scheduleAlertTool(args: Map<String, Any>): Map<String, Any> {
        return try {
            Log.d(TAG, "scheduleAlert вызван с аргументами: $args")

            val time = args["time"]?.toString() ?: "08:00"
            val message = args["message"]?.toString() ?: "Погодное оповещение"
            val days = (args["days"] as? List<*>)?.map { it.toString() }
                ?: listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница")

            // Парсим время
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val alertId = "alert_${System.currentTimeMillis()}"
            val scheduledAlert = ScheduledAlert(
                id = alertId,
                time = LocalTime.of(hour, minute),
                message = message,
                days = days,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

            scheduledAlerts[alertId] = scheduledAlert

            Log.d(TAG, "Запланировано оповещение на $time: $message")

            mapOf(
                "status" to "success",
                "content" to "✅ Оповещение запланировано на $time",
                "alertId" to alertId,
                "scheduledAlert" to scheduledAlert
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в scheduleAlert", e)
            mapOf(
                "status" to "error",
                "content" to "Ошибка планирования оповещения: ${e.message}"
            )
        }
    }

    /**
     * Инструмент: получение истории уведомлений
     */
    private fun getHistoryTool(args: Map<String, Any>): Map<String, Any> {
        return try {
            Log.d(TAG, "getHistory вызван")

            val limit = (args["limit"] as? Number)?.toInt() ?: 10
            val history = notificationHistory.takeLast(limit)

            mapOf(
                "status" to "success",
                "content" to "История уведомлений (${history.size} записей)",
                "history" to history,
                "total" to notificationHistory.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в getHistory", e)
            mapOf(
                "status" to "error",
                "content" to "Ошибка получения истории: ${e.message}"
            )
        }
    }
}

/**
 * Модель запланированного оповещения
 */
data class ScheduledAlert(
    val id: String,
    val time: LocalTime,
    val message: String,
    val days: List<String>,
    val isActive: Boolean,
    val createdAt: Long
)

/**
 * Модель уведомления
 */
data class NotificationRecord(
    val timestamp: Long,
    val title: String,
    val message: String,
    val priority: String
)