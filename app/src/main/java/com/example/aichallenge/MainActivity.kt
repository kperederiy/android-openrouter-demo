package com.example.aichallenge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.aichallenge.weather.WeatherWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val agent = SimpleAgent(
            apiKey = getApiKey(),
            context = this
        )

        //------------------------------------
        // Запуск WeatherWorker
        //------------------------------------
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<WeatherWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                "weather_worker",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

        //------------------------------------
        // Compose UI
        //------------------------------------
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainScreen(agent = agent)
                }
            }
        }
    }

    /**
     * Получение API-ключа
     */
    private fun getApiKey(): String {
        val properties = Properties()
        assets.open("secrets.properties").use {
            properties.load(it)
        }
        return properties.getProperty("api_key") ?: ""
    }
}

@Composable
fun MainScreen(agent: SimpleAgent) {
    // Состояния для UI
    var userInput by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Для пайплайна
    var pipelineLoading by remember { mutableStateOf(false) }
    var pipelineResult by remember { mutableStateOf("") }
    var pipelineStep by remember { mutableStateOf("") }
    var selectedRange by remember { mutableStateOf("24h") }

    // Для длинного флоу
    var longFlowLoading by remember { mutableStateOf(false) }
    var longFlowStep by remember { mutableStateOf("") }
    var longFlowResult by remember { mutableStateOf("") }

    // Для проверки опасных явлений
    var alertsLoading by remember { mutableStateOf(false) }
    var alertsResult by remember { mutableStateOf("") }

    // Получаем CoroutineScope для запуска корутин
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "MCP Pipeline + Уведомления",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        //----------------------------------
        // Выбор диапазона
        //----------------------------------
        Row {
            FilterChip(
                selected = selectedRange == "24h",
                onClick = { selectedRange = "24h" },
                label = { Text("24ч") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = selectedRange == "48h",
                onClick = { selectedRange = "48h" },
                label = { Text("48ч") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = selectedRange == "7d",
                onClick = { selectedRange = "7d" },
                label = { Text("7 дней") }
            )
        }

        Spacer(Modifier.height(8.dp))

        //----------------------------------
        // Pipeline (только погода)
        //----------------------------------
        Button(
            enabled = !pipelineLoading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                pipelineLoading = true
                pipelineStep = "🔍 Поиск данных..."
                pipelineResult = ""

                // Запускаем в корутине
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        agent.runWeatherPipeline(
                            range = selectedRange,
                            onSuccess = { result ->
                                // Обновляем UI в главном потоке
                                coroutineScope.launch(Dispatchers.Main) {
                                    pipelineStep = "✅ Pipeline завершён"
                                    pipelineResult = result
                                    pipelineLoading = false
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    pipelineStep = "❌ Ошибка"
                                    pipelineResult = error
                                    pipelineLoading = false
                                }
                            }
                        )
                    }
                }
            }
        ) {
            Text("Запустить Pipeline (погода)")
        }

        Spacer(Modifier.height(4.dp))

        if (pipelineLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(pipelineStep)
        }

        Spacer(Modifier.height(4.dp))

        //----------------------------------
        // Длинный флоу
        //----------------------------------
        Button(
            enabled = !longFlowLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            onClick = {
                longFlowLoading = true
                longFlowStep = "🌤️ Шаг 1/5: Получение данных о погоде..."
                longFlowResult = ""

                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        agent.runLongFlow(
                            range = selectedRange,
                            onStepUpdate = { step ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    longFlowStep = step
                                }
                            },
                            onSuccess = { result ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    longFlowResult = result
                                    longFlowLoading = false
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    longFlowResult = error
                                    longFlowLoading = false
                                }
                            }
                        )
                    }
                }
            }
        ) {
            Text("🚀 Длинный флоу (погода + уведомления)")
        }

        Spacer(Modifier.height(4.dp))

        if (longFlowLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(longFlowStep)
        }

        Spacer(Modifier.height(4.dp))

        //----------------------------------
        // Проверка опасных явлений
        //----------------------------------
        Row {
            Button(
                enabled = !alertsLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                onClick = {
                    alertsLoading = true
                    alertsResult = ""

                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            agent.checkAlerts(
                                onSuccess = { result ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        alertsResult = result
                                        alertsLoading = false
                                    }
                                },
                                onError = { error ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        alertsResult = error
                                        alertsLoading = false
                                    }
                                }
                            )
                        }
                    }
                }
            ) {
                Text("⚠️ Проверить опасные явления")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            agent.scheduleAlert(
                                time = "08:00",
                                message = "Ежедневное погодное оповещение",
                                onSuccess = { result ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        responseText = result
                                    }
                                },
                                onError = { error ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        responseText = error
                                    }
                                }
                            )
                        }
                    }
                }
            ) {
                Text("⏰ Настроить оповещение")
            }
        }

        Spacer(Modifier.height(4.dp))

        if (alertsLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text("⏳ Проверка опасных явлений...")
        }

        Spacer(Modifier.height(4.dp))

        //----------------------------------
        // Отображение результатов
        //----------------------------------
        Card(modifier = Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(
                    text = when {
                        longFlowResult.isNotEmpty() -> longFlowResult
                        alertsResult.isNotEmpty() -> alertsResult
                        pipelineResult.isNotEmpty() -> pipelineResult
                        else -> "Результаты будут отображаться здесь"
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Divider()

        Spacer(Modifier.height(16.dp))

        //----------------------------------
        // Чат с агентом
        //----------------------------------
        Text(
            text = "💬 Чат с агентом",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Введите запрос") },
            placeholder = { Text("Например: проверь погоду и оповести") }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (userInput.isBlank()) return@Button
                isLoading = true
                responseText = ""

                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        agent.processRequest(
                            userInput,
                            onSuccess = { result ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    responseText = result
                                    isLoading = false
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    responseText = error
                                    isLoading = false
                                }
                            }
                        )
                    }
                }
            }
        ) {
            Text("Отправить запрос")
        }

        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(responseText)
                }
            }
        }
    }
}