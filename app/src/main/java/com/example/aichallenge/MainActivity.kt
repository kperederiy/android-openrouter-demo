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
import androidx.work.Constraints
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val work =
            PeriodicWorkRequestBuilder<
                    WeatherWorker
                    >(
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
                work
            )

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

            var aggregatedData by remember {
                mutableStateOf("")
            }

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            SelectionContainer {

                                Text(
                                    text = aggregatedData,
                                    modifier =
                                        Modifier.padding(
                                            16.dp
                                        )
                                )
                            }
                        }

                        Button(
                            onClick = {

                                agent.processAggregatedWeather(

                                    onSuccess = {

                                        runOnUiThread {

                                            aggregatedData = it
                                        }
                                    },

                                    onError = {

                                        runOnUiThread {

                                            aggregatedData = it
                                        }
                                    }
                                )
                            }
                        ) {

                            Text(
                                "Показать сводку"
                            )
                        }

                        OutlinedTextField(
                            value = userInput,
                            onValueChange = {
                                userInput = it
                            },
                            label = {
                                Text("Введите запрос")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Button(
                            enabled = !isLoading,
                            onClick = {

                                if (userInput.isBlank()) {
                                    return@Button
                                }

                                isLoading = true

                                agent.processRequest(
                                    userInput,

                                    onSuccess = { answer ->

                                        runOnUiThread {

                                            responseText = answer
                                            isLoading = false
                                        }
                                    },

                                    onError = { error ->

                                        runOnUiThread {

                                            responseText = error
                                            isLoading = false
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отправить")
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

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
                                        text = responseText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getApiKey(): String {

        val properties = Properties()

        assets.open("secrets.properties").use {
            properties.load(it)
        }

        return properties.getProperty("api_key") ?: ""
    }
}