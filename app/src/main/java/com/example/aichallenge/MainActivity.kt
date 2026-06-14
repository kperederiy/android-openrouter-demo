package com.example.aichallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Properties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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

            var selectedStrategy by remember {

                mutableStateOf(
                    MemoryStrategy.SLIDING_WINDOW
                )
            }

            var branches by remember {
                mutableStateOf(
                    agent.getBranches()
                )
            }

            var currentBranch by remember {
                mutableStateOf(
                    agent.getCurrentBranch()
                )
            }

            var showBranchMenu by remember {
                mutableStateOf(false)
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
                            text = "AI Agent",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Стратегия памяти"
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        SingleChoiceSegmentedButtonRow {

                            MemoryStrategy.entries.forEachIndexed { index, strategy ->

                                SegmentedButton(
                                    selected = selectedStrategy == strategy,
                                    onClick = {
                                        selectedStrategy = strategy
                                        agent.setStrategy(strategy)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = MemoryStrategy.entries.size
                                    )
                                ) {
                                    Text(strategy.title)
                                }
                            }
                        }

                        Button(
                            onClick = {

                                agent.loadDemoConversation()

                                responseText =
                                    "Тестовый диалог загружен.\n" +
                                            "Теперь задайте вопрос:\n" +
                                            "\"Какой бюджет проекта?\""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Загрузить тестовый диалог")
                        }

                        Button(
                            onClick = {

                                val branchName =
                                    "branch-" +
                                            System.currentTimeMillis()

                                agent.createCheckpoint(
                                    branchName
                                )

                                branches =
                                    agent.getBranches()

                                responseText =
                                    "Создан checkpoint:\n$branchName"
                            }
                        ) {
                            Text("Checkpoint")
                        }

                        Button(
                            onClick = {

                                val branchName =
                                    "child-" +
                                            System.currentTimeMillis()

                                agent.createBranchFromCurrent(
                                    branchName
                                )

                                branches =
                                    agent.getBranches()

                                responseText =
                                    "Создана новая ветка:\n$branchName"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Создать ветку")
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "Текущая ветка: $currentBranch",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Button(
                            onClick = {
                                showBranchMenu = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Переключить ветку")
                        }

                        if (showBranchMenu) {

                            AlertDialog(
                                onDismissRequest = {
                                    showBranchMenu = false
                                },

                                title = {
                                    Text("Выберите ветку")
                                },

                                text = {

                                    Column {

                                        branches.forEach { branch ->

                                            Button(
                                                onClick = {

                                                    agent.switchBranch(
                                                        branch
                                                    )

                                                    currentBranch =
                                                        branch

                                                    showBranchMenu =
                                                        false

                                                    responseText =
                                                        "Переключено на ветку:\n$branch"
                                                },
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {

                                                Text(branch)
                                            }

                                            Spacer(
                                                modifier =
                                                    Modifier.height(4.dp)
                                            )
                                        }
                                    }
                                },

                                confirmButton = {}
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

──────────────────

Токены запроса:
${stats.promptTokens}

Токены ответа:
${stats.completionTokens}

Всего токенов:
${stats.totalTokens}

Стратегия:
${stats.strategy}

Токены истории:
${stats.historyTokens}

Стоимость запроса:
${"%.8f".format(stats.estimatedCost)} $

Заполнение контекста:
${stats.contextUsagePercent}%

${stats.contextWarning}
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

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        Spacer(modifier = Modifier.height(20.dp))

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