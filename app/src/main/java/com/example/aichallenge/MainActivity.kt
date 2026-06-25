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

        val agent = SimpleAgent(
            apiKey = getApiKey()
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

            var toolsText by remember {
                mutableStateOf("")
            }

            var city by remember {
                mutableStateOf("")
            }

            var toolResult by remember {
                mutableStateOf("")
            }

            var toolLoading by remember {
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

                                toolsText = ""   // очищаем список инструментов

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

                        OutlinedTextField(
                            value = city,

                            onValueChange = {
                                city = it
                            },

                            label = {
                                Text("Город")
                            },

                            modifier = Modifier.fillMaxWidth()
                        )

                        if (toolLoading) {

                            CircularProgressIndicator()
                        }

                        Button(

                            enabled = !toolLoading,

                            onClick = {

                                toolLoading = true

                                agent.processToolRequest(

                                    city = city,

                                    onSuccess = {

                                        runOnUiThread {

                                            toolResult = it
                                            toolLoading = false
                                        }
                                    },

                                    onError = {

                                        runOnUiThread {

                                            toolResult = it
                                            toolLoading = false
                                        }
                                    }
                                )
                            },

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Text("Получить погоду")
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            SelectionContainer {

                                Text(
                                    text = toolResult,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
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
                                        text = toolsText.ifBlank { responseText }
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