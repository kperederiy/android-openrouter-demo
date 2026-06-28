package com.example.aichallenge

import android.content.Context
import android.util.Log
import com.example.aichallenge.mcp.MCPRouter
import com.example.aichallenge.mcp.NotificationMCPServer
import com.example.aichallenge.mcp.WeatherMCPServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SimpleAgent(
    private val apiKey: String,
    context: Context
) {
    companion object {
        private const val TAG = "SimpleAgent"
        private const val MODEL = "openai/gpt-4o-mini"
    }

    private val client = OkHttpClient()
    private val router: MCPRouter

    // Последняя сформированная сводка
    private var lastSummary = ""

    // Результаты последнего длинного флоу
    private var lastFlowResults = mutableMapOf<String, Any>()

    init {
        // Инициализация маршрутизатора
        router = MCPRouter().apply {
            // Регистрируем WeatherMCP сервер
            val weatherServer = WeatherMCPServer(context)
            register("weather", weatherServer)
            registerTools("weather", listOf(
                "weather", "search", "summarize", "saveToFile", "pipeline"
            ))

            // Регистрируем NotificationMCP сервер
            val notificationServer = NotificationMCPServer(context)
            register("notify", notificationServer)
            registerTools("notify", listOf(
                "checkAlerts", "sendNotification", "scheduleAlert", "getHistory"
            ))

            Log.d(TAG, "MCPRouter инициализирован с серверами: weather, notify")
        }
    }

    /**
     * Запуск полного MCP Pipeline (только погода)
     */
    fun runWeatherPipeline(
        range: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = router.callTool(
                    "weather.pipeline",
                    mapOf(
                        "query" to "Брянск",
                        "range" to range,
                        "filename" to "weather_report.txt"
                    )
                )

                if (result["status"] == "success") {
                    lastSummary = result["summary"]?.toString() ?: ""
                    onSuccess(result["content"].toString())
                } else {
                    onError(result["content"].toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pipeline error", e)
                onError(e.message ?: "Pipeline error")
            }
        }
    }

    /**
     * Длинный флоу: погода + уведомления
     */
    fun runLongFlow(
        range: String = "24h",
        onStepUpdate: (String) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                lastFlowResults.clear()
                val steps = mutableListOf<Map<String, Any>>()

                // Шаг 1: Получение погоды
                withContext(Dispatchers.Main) {
                    onStepUpdate("🌤️ Шаг 1/5: Получение данных о погоде...")
                }

                val weatherResult = router.callTool(
                    "weather.search",
                    mapOf(
                        "query" to "Брянск",
                        "range" to range
                    )
                )

                if (weatherResult["status"] != "success") {
                    withContext(Dispatchers.Main) {
                        onError("❌ Ошибка получения погоды: ${weatherResult["content"]}")
                    }
                    return@launch
                }

                steps.add(mapOf(
                    "step" to "Получение погоды",
                    "result" to weatherResult
                ))
                lastFlowResults["weather"] = weatherResult

                withContext(Dispatchers.Main) {
                    onStepUpdate("✅ Шаг 1/5: Данные о погоде получены")
                }

                // Шаг 2: Проверка опасных явлений
                withContext(Dispatchers.Main) {
                    onStepUpdate("⚠️ Шаг 2/5: Проверка опасных явлений...")
                }

                val alertsResult = router.callTool(
                    "notify.checkAlerts",
                    mapOf(
                        "data" to (weatherResult["data"] ?: emptyList<Any>())
                    )
                )

                steps.add(mapOf(
                    "step" to "Проверка опасных явлений",
                    "result" to alertsResult
                ))
                lastFlowResults["alerts"] = alertsResult

                val hasAlerts = alertsResult["hasAlerts"] as? Boolean ?: false
                val alertMessage = if (hasAlerts) {
                    "⚠️ Обнаружены опасные явления!"
                } else {
                    "✅ Опасных явлений не обнаружено"
                }

                withContext(Dispatchers.Main) {
                    onStepUpdate("✅ Шаг 2/5: $alertMessage")
                }

                // Шаг 3: Создание сводки
                withContext(Dispatchers.Main) {
                    onStepUpdate("📊 Шаг 3/5: Создание сводки...")
                }

                val summaryResult = router.callTool(
                    "weather.summarize",
                    mapOf(
                        "data" to (weatherResult["data"] ?: emptyList<Any>())
                    )
                )

                if (summaryResult["status"] != "success") {
                    withContext(Dispatchers.Main) {
                        onError("❌ Ошибка создания сводки: ${summaryResult["content"]}")
                    }
                    return@launch
                }

                steps.add(mapOf(
                    "step" to "Создание сводки",
                    "result" to summaryResult
                ))
                lastFlowResults["summary"] = summaryResult
                lastSummary = summaryResult["summary"]?.toString() ?: ""

                withContext(Dispatchers.Main) {
                    onStepUpdate("✅ Шаг 3/5: Сводка создана")
                }

                // Шаг 4: Отправка уведомления
                withContext(Dispatchers.Main) {
                    onStepUpdate("🔔 Шаг 4/5: Отправка уведомления...")
                }

                val notificationMessage = buildString {
                    appendLine("📋 Сводка погоды в Брянске")
                    appendLine()
                    appendLine(lastSummary)
                    appendLine()
                    appendLine("⚠️ Опасные явления:")
                    if (hasAlerts) {
                        val alerts = alertsResult["alerts"] as? List<*> ?: emptyList<Any>()
                        alerts.forEach { alert ->
                            appendLine("  $alert")
                        }
                    } else {
                        appendLine("  ✅ Не обнаружены")
                    }
                }

                val notificationResult = router.callTool(
                    "notify.sendNotification",
                    mapOf(
                        "title" to "🌤️ Погодный отчет",
                        "message" to notificationMessage,
                        "priority" to if (hasAlerts) "high" else "normal"
                    )
                )

                steps.add(mapOf(
                    "step" to "Отправка уведомления",
                    "result" to notificationResult
                ))
                lastFlowResults["notification"] = notificationResult

                withContext(Dispatchers.Main) {
                    onStepUpdate("✅ Шаг 4/5: Уведомление отправлено")
                }

                // Шаг 5: Сохранение отчета
                withContext(Dispatchers.Main) {
                    onStepUpdate("💾 Шаг 5/5: Сохранение отчета...")
                }

                val saveResult = router.callTool(
                    "weather.saveToFile",
                    mapOf(
                        "content" to buildString {
                            appendLine("=== Погодный отчет с оповещениями ===")
                            appendLine()
                            appendLine(lastSummary)
                            appendLine()
                            appendLine("Опасные явления:")
                            if (hasAlerts) {
                                val alerts = alertsResult["alerts"] as? List<*> ?: emptyList<Any>()
                                alerts.forEach { alert ->
                                    appendLine("  $alert")
                                }
                            } else {
                                appendLine("  Не обнаружены")
                            }
                            appendLine()
                            appendLine("Время отчета: ${System.currentTimeMillis()}")
                        },
                        "filename" to "weather_with_alerts_${System.currentTimeMillis()}.txt"
                    )
                )

                steps.add(mapOf(
                    "step" to "Сохранение отчета",
                    "result" to saveResult
                ))
                lastFlowResults["save"] = saveResult

                withContext(Dispatchers.Main) {
                    onStepUpdate("✅ Шаг 5/5: Отчет сохранен")
                }

                // Формируем финальный результат
                val finalResult = buildString {
                    appendLine("✅ Длинный флоу успешно завершен!")
                    appendLine()
                    appendLine("📊 Выполненные шаги:")
                    steps.forEachIndexed { index, step ->
                        appendLine("  ${index + 1}. ${step["step"]} - ✅")
                    }
                    appendLine()
                    appendLine("📋 Сводка:")
                    appendLine(lastSummary)
                    appendLine()
                    if (hasAlerts) {
                        appendLine("⚠️ ОБНАРУЖЕНЫ ОПАСНЫЕ ЯВЛЕНИЯ!")
                        val alerts = alertsResult["alerts"] as? List<*> ?: emptyList<Any>()
                        alerts.forEach { alert ->
                            appendLine("  $alert")
                        }
                    } else {
                        appendLine("✅ Опасных явлений не обнаружено")
                    }
                    appendLine()
                    val filePath = saveResult["filePath"]?.toString()
                    if (!filePath.isNullOrBlank()) {
                        appendLine("💾 Отчет сохранен: $filePath")
                    }
                }

                withContext(Dispatchers.Main) {
                    onSuccess(finalResult)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Long flow error", e)
                withContext(Dispatchers.Main) {
                    onError("❌ Ошибка в длинном флоу: ${e.message}")
                }
            }
        }
    }

    /**
     * Проверка опасных явлений
     */
    fun checkAlerts(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Сначала получаем текущую погоду
                val weatherResult = router.callTool(
                    "weather.search",
                    mapOf(
                        "query" to "Брянск",
                        "range" to "24h"
                    )
                )

                if (weatherResult["status"] != "success") {
                    withContext(Dispatchers.Main) {
                        onError("❌ Не удалось получить данные о погоде")
                    }
                    return@launch
                }

                // Проверяем опасные явления
                val alertsResult = router.callTool(
                    "notify.checkAlerts",
                    mapOf(
                        "data" to (weatherResult["data"] ?: emptyList<Any>())
                    )
                )

                if (alertsResult["status"] != "success") {
                    withContext(Dispatchers.Main) {
                        onError("❌ ${alertsResult["content"]}")
                    }
                    return@launch
                }

                val hasAlerts = alertsResult["hasAlerts"] as? Boolean ?: false
                val alerts = alertsResult["alerts"] as? List<*> ?: emptyList<Any>()
                val alertLevel = alertsResult["alertLevel"]?.toString() ?: "green"

                val result = buildString {
                    appendLine("🔍 Проверка опасных явлений")
                    appendLine()
                    appendLine("Уровень опасности: ${when(alertLevel) {
                        "red" -> "🔴 КРАСНЫЙ (Высокий)"
                        "yellow" -> "🟡 ЖЕЛТЫЙ (Средний)"
                        else -> "🟢 ЗЕЛЕНЫЙ (Низкий)"
                    }}")
                    appendLine()
                    if (hasAlerts) {
                        appendLine("⚠️ Обнаружены опасные явления:")
                        alerts.forEach { alert ->
                            appendLine("  $alert")
                        }
                    } else {
                        appendLine("✅ Опасных явлений не обнаружено")
                    }
                }

                withContext(Dispatchers.Main) {
                    onSuccess(result)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Check alerts error", e)
                withContext(Dispatchers.Main) {
                    onError("❌ Ошибка: ${e.message}")
                }
            }
        }
    }

    /**
     * Настройка оповещения
     */
    fun scheduleAlert(
        time: String,
        message: String = "Погодное оповещение",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = router.callTool(
                    "notify.scheduleAlert",
                    mapOf(
                        "time" to time,
                        "message" to message,
                        "days" to listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница")
                    )
                )

                if (result["status"] == "success") {
                    withContext(Dispatchers.Main) {
                        onSuccess(result["content"].toString())
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(result["content"].toString())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Schedule alert error", e)
                withContext(Dispatchers.Main) {
                    onError("❌ Ошибка: ${e.message}")
                }
            }
        }
    }

    /**
     * Получение истории уведомлений
     */
    fun getNotificationHistory(
        limit: Int = 10,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = router.callTool(
                    "notify.getHistory",
                    mapOf("limit" to limit)
                )

                if (result["status"] == "success") {
                    val history = result["history"] as? List<*> ?: emptyList<Any>()
                    val resultText = buildString {
                        appendLine("📋 История уведомлений (${history.size} записей)")
                        appendLine()
                        history.forEach { item ->
                            if (item is Map<*, *>) {
                                val title = item["title"]?.toString() ?: "Без названия"
                                val message = item["message"]?.toString() ?: ""
                                val priority = item["priority"]?.toString() ?: "normal"
                                appendLine("🔔 $title")
                                appendLine("   $message")
                                appendLine("   Приоритет: $priority")
                                appendLine()
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess(resultText)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(result["content"].toString())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get history error", e)
                withContext(Dispatchers.Main) {
                    onError("❌ Ошибка: ${e.message}")
                }
            }
        }
    }

    /**
     * Главная точка входа агента (обновленная)
     */
    fun processRequest(
        userRequest: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val text = userRequest.lowercase()

        //------------------------------------------------------
        // Длинный флоу
        //------------------------------------------------------
        if ("проверить погоду и оповестить" in text ||
            "длинный флоу" in text) {
            runLongFlow(
                range = when {
                    "недел" in text -> "7d"
                    "2 дня" in text -> "48h"
                    else -> "24h"
                },
                onStepUpdate = { step ->
                    // Шаги обновляются через callback
                },
                onSuccess = onSuccess,
                onError = onError
            )
            return
        }

        //------------------------------------------------------
        // Проверка опасных явлений
        //------------------------------------------------------
        if ("опасные явления" in text ||
            "есть ли опасные" in text ||
            "проверить опасные" in text) {
            checkAlerts(onSuccess, onError)
            return
        }

        //------------------------------------------------------
        // Настройка оповещения
        //------------------------------------------------------
        if ("оповещение" in text && ":" in text) {
            // Извлекаем время
            val timeRegex = Regex("(\\d{1,2}:\\d{2})")
            val time = timeRegex.find(text)?.value ?: "08:00"

            val message = when {
                "погода" in text -> "Ежедневное погодное оповещение"
                else -> "Погодное оповещение"
            }

            scheduleAlert(time, message, onSuccess, onError)
            return
        }

        //------------------------------------------------------
        // История уведомлений
        //------------------------------------------------------
        if ("история уведомлений" in text) {
            getNotificationHistory(10, onSuccess, onError)
            return
        }

        //------------------------------------------------------
        // Пайплайн (только погода)
        //------------------------------------------------------
        if ("пайплайн" in text) {
            val range = when {
                "недел" in text -> "7d"
                "2 дня" in text -> "48h"
                else -> "24h"
            }
            runWeatherPipeline(range, onSuccess, onError)
            return
        }

        //------------------------------------------------------
        // Последняя сводка
        //------------------------------------------------------
        if ("показать сводку" in text) {
            if (lastSummary.isBlank()) {
                onError("Сводка ещё не сформирована.")
            } else {
                onSuccess(lastSummary)
            }
            return
        }

        //------------------------------------------------------
        // Сохранение последней сводки
        //------------------------------------------------------
        if ("сохранить отчет" in text) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = router.callTool(
                        "weather.saveToFile",
                        mapOf(
                            "content" to lastSummary,
                            "filename" to "weather_report.txt"
                        )
                    )
                    onSuccess(result["content"].toString())
                } catch (e: Exception) {
                    onError(e.message ?: "Save error")
                }
            }
            return
        }

        //------------------------------------------------------
        // Текущая погода
        //------------------------------------------------------
        if ("погода сейчас" in text) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = router.callTool(
                        "weather.weather",
                        mapOf("city" to "Брянск")
                    )
                    onSuccess(result["content"].toString())
                } catch (e: Exception) {
                    onError(e.message ?: "Weather error")
                }
            }
            return
        }

        //------------------------------------------------------
        // Погода за день
        //------------------------------------------------------
        if ("погода за день" in text) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val search = router.callTool(
                        "weather.search",
                        mapOf(
                            "query" to "Брянск",
                            "range" to "24h"
                        )
                    )

                    val summary = router.callTool(
                        "weather.summarize",
                        mapOf("data" to (search["data"] ?: emptyList<Any>()))
                    )

                    lastSummary = summary["summary"]?.toString() ?: ""
                    onSuccess(lastSummary)
                } catch (e: Exception) {
                    onError(e.message ?: "Summary error")
                }
            }
            return
        }

        //------------------------------------------------------
        // Средняя температура
        //------------------------------------------------------
        if ("средняя температура" in text) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val search = router.callTool(
                        "weather.search",
                        mapOf(
                            "query" to "Брянск",
                            "range" to "24h"
                        )
                    )

                    val data = search["data"] as? List<*> ?: emptyList<Any>()
                    val temps = data.mapNotNull { item ->
                        if (item is Map<*, *>) {
                            (item["temperature"] as? Number)?.toDouble()
                        } else null
                    }

                    val average = if (temps.isNotEmpty()) temps.average() else 0.0
                    onSuccess("Средняя температура за последние 24 часа: %.1f°C".format(average))
                } catch (e: Exception) {
                    onError(e.message ?: "Ошибка получения средней температуры")
                }
            }
            return
        }

        //------------------------------------------------------
        // Полная статистика
        //------------------------------------------------------
        if ("статистика погоды" in text) {
            runWeatherPipeline("24h", onSuccess, onError)
            return
        }

        //------------------------------------------------------
        // Если специальных команд нет — обращаемся к LLM
        //------------------------------------------------------
        callLLM(
            prompt = userRequest,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Запрос к OpenRouter
     */
    private fun callLLM(
        prompt: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject()
        json.put("model", MODEL)

        val messages = JSONArray()
        messages.put(
            JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }
        )
        json.put("messages", messages)

        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onError("Ошибка сети: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        onError("HTTP ${response.code}\n$body")
                        return
                    }

                    try {
                        val answer = JSONObject(body)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        onSuccess(answer)
                    } catch (e: Exception) {
                        onError(e.message ?: "Ошибка обработки ответа")
                    }
                }
            }
        )
    }
}