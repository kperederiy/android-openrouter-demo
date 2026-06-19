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

            var currentState by remember {
                mutableStateOf(
                    agent.getCurrentState()
                )
            }

            var isPaused by remember {
                mutableStateOf(
                    agent.isPaused()
                )
            }

            var promptText by remember {
                mutableStateOf("Промпт еще не отправлялся")
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
                            enabled =
                                !isLoading &&
                                        !isPaused,
                            onClick = {

                                if (userInput.isBlank()) {
                                    return@Button
                                }

                                isLoading = true
                                responseText = "Получаем ответ..."

                                promptText = userInput
                                agent.processRequest(
                                    userInput,

                                    onSuccess = { answer, stats ->

                                        runOnUiThread {

                                            responseText =
                                                """
Состояние FSM: $currentState
Ответ:
$answer
────────────────────
Токены запроса: ${stats.promptTokens}
Токены ответа: ${stats.completionTokens}
Всего токенов: ${stats.totalTokens}
Токены памяти: ${stats.historyTokens}
Стоимость: ${"%.8f".format(stats.estimatedCost)} $
Использование контекста: ${stats.contextUsagePercent}% ${stats.contextWarning}
Модель памяти: ${stats.strategy}
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
                            modifier = Modifier.height(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Button(

                                enabled =
                                    when(currentState) {
                                        TaskState.PLANNING -> false
                                        else -> true
                                    },

                                modifier =
                                    Modifier.weight(1f),

                                onClick = {

                                    agent.moveBackward()

                                    currentState =
                                        agent.getCurrentState()

                                    isLoading = true

                                    responseText =
                                        "Переход в состояние $currentState..."

                                    promptText = userInput
                                    agent.processRequest(

                                        "Продолжай работу согласно текущему состоянию задачи",

                                        updateTask = false,

                                        onSuccess = { answer, stats ->

                                            runOnUiThread {

                                                responseText =
                                                    """
Состояние FSM: $currentState

Ответ:
$answer
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
                                }

                            ) {

                                Text("Назад")
                            }

                            Button(

                                modifier =
                                    Modifier.weight(1f),

                                onClick = {

                                    if (isPaused) {

                                        agent.resumeTask()

                                        isPaused = false

                                        isLoading = true

                                        responseText =
                                            "Восстанавливаем контекст..."

                                        promptText = userInput
                                        agent.processRequest(

                                            "Продолжи выполнение текущей задачи",
                                            updateTask = false,

                                            onSuccess = { answer, stats ->

                                                runOnUiThread {

                                                    responseText =
                                                        """
Ответ:
$answer
────────────────────
Токены запроса: ${stats.promptTokens}
Токены ответа: ${stats.completionTokens}
Всего токенов: ${stats.totalTokens}
Токены памяти: ${stats.historyTokens}
Стоимость: ${"%.8f".format(stats.estimatedCost)} $
Использование контекста: ${stats.contextUsagePercent}% ${stats.contextWarning}
Модель памяти: ${stats.strategy}
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

                                    } else {

                                        agent.pauseTask()

                                        isPaused = true
                                    }
                                }

                            ) {

                                Text(

                                    if (isPaused)
                                        "Продолжить"
                                    else
                                        "Пауза"
                                )
                            }

                            Button(

                                enabled =
                                    currentState != TaskState.DONE,

                                modifier =
                                    Modifier.weight(1f),

                                onClick = {

                                    agent.moveForward()

                                    currentState =
                                        agent.getCurrentState()

                                    isLoading = true

                                    responseText =
                                        "Переход в состояние $currentState..."

                                    promptText = userInput
                                    agent.processRequest(

                                        "Продолжай работу согласно текущему состоянию задачи",

                                        onSuccess = { answer, stats ->

                                            runOnUiThread {

                                                responseText =
                                                    """
Состояние FSM: $currentState

Ответ:
$answer
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
                                }

                            ) {

                                Text("Вперед")
                            }
                        }


                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        /*promptText =
                            """
===== SYSTEM =====

${agent.getDebugPrompt()}

===== USER =====

$userInput
    """.trimIndent()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {

                            SelectionContainer {

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(12.dp)
                                ) {

                                    Text(
                                        text = promptText
                                    )
                                }
                            }
                        }*/

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

                        Button(
                            onClick = {
                                agent.clearMemory()

                                currentState =
                                    agent.getCurrentState()

                                isPaused =
                                    agent.isPaused()

                                responseText =
                                    "Память очищена. Состояние сброшено в PLANNING."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Очистить память")
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