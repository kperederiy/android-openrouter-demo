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
import java.util.Properties

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = getApiKey()

        val agent = SimpleAgent(
            context = applicationContext,
            apiKey = apiKey
        )

        setContent {

            var userInput by remember {
                mutableStateOf("")
            }

            var responseText by remember {
                mutableStateOf("Введите запрос")
            }

            var isLoading by remember {
                mutableStateOf(false)
            }

            var profileEnabled by remember {

                mutableStateOf(true)
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

                        Text(
                            text = "AI Advent Challenge",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

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
                            modifier = Modifier.height(16.dp)
                        )

                        Button(
                            onClick = {

                                agent.clearMemory()

                                responseText =
                                    "Все слои памяти очищены."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Очистить память")
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Row {

                            Checkbox(

                                checked = profileEnabled,

                                onCheckedChange = {

                                    profileEnabled = it

                                    if (it) {

                                        agent.enableUserProfile()

                                    } else {

                                        agent.disableUserProfile()
                                    }
                                }
                            )

                            Text(
                                "Использовать профиль пользователя"
                            )
                        }

                        Button(
                            onClick = {

                                if (userInput.isBlank()) {
                                    return@Button
                                }

                                isLoading = true
                                responseText = "Получаем ответ..."

                                agent.processRequest(
                                    userInput,

                                    onSuccess = { answer, stats ->

                                        runOnUiThread {

                                            responseText =
                                                """
Ответ:

$answer

────────────────────

Токены запроса:
${stats.promptTokens}

Токены ответа:
${stats.completionTokens}

Всего токенов:
${stats.totalTokens}

Токены памяти:
${stats.historyTokens}

Стоимость:
${"%.8f".format(stats.estimatedCost)} $

Использование контекста:
${stats.contextUsagePercent}%

${stats.contextWarning}

Модель памяти:
${stats.strategy}
                                                """.trimIndent()

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
                            modifier = Modifier.height(16.dp)
                        )


                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
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