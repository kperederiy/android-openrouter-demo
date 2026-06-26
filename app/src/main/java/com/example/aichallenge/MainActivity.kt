package com.example.aichallenge

import android.os.Bundle
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
import java.util.Properties
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

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

            .setRequiredNetworkType(
                NetworkType.CONNECTED
            )

            .build()

        val request =

            PeriodicWorkRequestBuilder<WeatherWorker>(

                15,
                TimeUnit.MINUTES

            )

                .setConstraints(
                    constraints
                )

                .build()

        WorkManager

            .getInstance(this)

            .enqueueUniquePeriodicWork(

                "weather_worker",

                ExistingPeriodicWorkPolicy.UPDATE,

                request
            )

        //------------------------------------
        // Compose
        //------------------------------------

        setContent {

            var userInput by remember {

                mutableStateOf("")
            }

            var responseText by remember {

                mutableStateOf("")
            }

            var isLoading by remember {

                mutableStateOf(false)
            }

            //--------------------------------

            var pipelineLoading by remember {

                mutableStateOf(false)
            }

            var pipelineResult by remember {

                mutableStateOf("")
            }

            var pipelineStep by remember {

                mutableStateOf("")
            }

            var selectedRange by remember {

                mutableStateOf("24h")
            }

            //--------------------------------

            MaterialTheme {

                Surface(

                    modifier =
                        Modifier.fillMaxSize()

                ) {

                    Column(

                        modifier = Modifier

                            .fillMaxSize()

                            .padding(16.dp)

                    ) {

                        Text(

                            text = "MCP Pipeline",

                            style =
                                MaterialTheme.typography.titleLarge
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        //----------------------------------
                        // Выбор диапазона
                        //----------------------------------

                        Row {

                            FilterChip(

                                selected =
                                    selectedRange == "24h",

                                onClick = {

                                    selectedRange = "24h"
                                },

                                label = {

                                    Text("24ч")
                                }
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            FilterChip(

                                selected =
                                    selectedRange == "48h",

                                onClick = {

                                    selectedRange = "48h"
                                },

                                label = {

                                    Text("48ч")
                                }
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            FilterChip(

                                selected =
                                    selectedRange == "7d",

                                onClick = {

                                    selectedRange = "7d"
                                },

                                label = {

                                    Text("7 дней")
                                }
                            )
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        //----------------------------------
                        // Pipeline
                        //----------------------------------

                        Button(

                            enabled =
                                !pipelineLoading,

                            modifier =
                                Modifier.fillMaxWidth(),

                            onClick = {

                                pipelineLoading = true

                                pipelineStep =
                                    "🔍 Поиск данных..."

                                agent.runWeatherPipeline(

                                    range =
                                        selectedRange,

                                    onSuccess = {

                                        runOnUiThread {

                                            pipelineStep =
                                                "✅ Pipeline завершён"

                                            pipelineResult =
                                                it

                                            pipelineLoading =
                                                false
                                        }
                                    },

                                    onError = {

                                        runOnUiThread {

                                            pipelineStep =
                                                "❌ Ошибка"

                                            pipelineResult =
                                                it

                                            pipelineLoading =
                                                false
                                        }
                                    }
                                )
                            }

                        ) {

                            Text(
                                "Запустить Pipeline"
                            )
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        if (pipelineLoading) {

                            LinearProgressIndicator(

                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            Spacer(
                                Modifier.height(8.dp)
                            )

                            Text(
                                pipelineStep
                            )
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Card(

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            SelectionContainer {

                                Text(

                                    text =
                                        pipelineResult,

                                    modifier =
                                        Modifier.padding(16.dp)
                                )
                            }
                        }

                        Spacer(
                            Modifier.height(16.dp)
                        )
                        //----------------------------------
                        // Агент
                        //----------------------------------

                        OutlinedTextField(

                            value = userInput,

                            onValueChange = {

                                userInput = it
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            label = {

                                Text("Введите запрос")
                            }
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Button(

                            enabled = !isLoading,

                            modifier =
                                Modifier.fillMaxWidth(),

                            onClick = {

                                if (userInput.isBlank()) {
                                    return@Button
                                }

                                isLoading = true

                                responseText = ""

                                agent.processRequest(

                                    userInput,

                                    onSuccess = {

                                        runOnUiThread {

                                            responseText = it

                                            isLoading = false
                                        }
                                    },

                                    onError = {

                                        runOnUiThread {

                                            responseText = it

                                            isLoading = false
                                        }
                                    }
                                )
                            }

                        ) {

                            Text("Отправить")
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        if (isLoading) {

                            CircularProgressIndicator()

                            Spacer(
                                Modifier.height(8.dp)
                            )
                        }

                        Card(

                            modifier = Modifier

                                .fillMaxWidth()

                                .weight(1f)

                        ) {

                            SelectionContainer {

                                Column(

                                    modifier = Modifier

                                        .fillMaxSize()

                                        .verticalScroll(

                                            rememberScrollState()

                                        )

                                        .padding(16.dp)

                                ) {

                                    Text(
                                        responseText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Получение API-ключа
     */
    private fun getApiKey(): String {

        val properties =
            Properties()

        assets.open(
            "secrets.properties"
        ).use {

            properties.load(it)
        }

        return properties.getProperty(
            "api_key"
        ) ?: ""
    }
}