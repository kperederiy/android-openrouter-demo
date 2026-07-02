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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Properties

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "INDEX"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = getApiKey()

        val agent = SimpleAgent(
            apiKey = apiKey
        )

        val documentLoader = DocumentLoader(
            context = this
        )

        val chunkingStrategy: ChunkingStrategy =
            ParagraphChunker()

        val embeddingService = EmbeddingService(
            apiKey = apiKey
        )

        val indexInitializer = IndexInitializer(
            context = this,
            documentLoader = documentLoader,
            chunkingStrategy = chunkingStrategy,
            embeddingService = embeddingService
        )

        lifecycleScope.launch {

            Log.d(TAG, "========================================")
            Log.d(TAG, "Начало индексации")
            Log.d(TAG, "========================================")

            try {

                indexInitializer.initialize { current, total ->

                    Log.d(
                        TAG,
                        "Обработано chunk: $current / $total"
                    )
                }

                val storage = IndexStorage(this@MainActivity)

                Log.d(TAG, "========================================")
                Log.d(TAG, "Индексация успешно завершена")
                Log.d(
                    TAG,
                    "Файл индекса: ${storage.getIndexFile().absolutePath}"
                )
                Log.d(
                    TAG,
                    "Размер файла: ${storage.getIndexFile().length()} байт"
                )
                Log.d(TAG, "========================================")

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Ошибка индексации",
                    e
                )
            }
        }

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

            var isIndexing by remember {
                mutableStateOf(false)
            }

            var indexingText by remember {
                mutableStateOf("")
            }

            var indexingProgress by remember {
                mutableFloatStateOf(0f)
            }

            var selectedChunking by remember {
                mutableStateOf(ChunkingType.PARAGRAPH)
            }

            var fixedChunkSize by remember {
                mutableStateOf("1000")
            }

            fun rebuildIndex() {

                lifecycleScope.launch {

                    isIndexing = true

                    indexingProgress = 0f

                    indexingText = "Начинаем индексацию..."

                    val strategy: ChunkingStrategy =
                        when (selectedChunking) {

                            ChunkingType.PARAGRAPH ->
                                ParagraphChunker()

                            ChunkingType.FIXED_SIZE ->
                                FixedSizeChunker(
                                    fixedChunkSize.toIntOrNull() ?: 1000
                                )
                        }

                    val initializer = IndexInitializer(

                        context = this@MainActivity,

                        documentLoader = DocumentLoader(this@MainActivity),

                        chunkingStrategy = strategy,

                        embeddingService = EmbeddingService(apiKey)

                    )

                    try {

                        initializer.initialize(
                            forceRebuild = true
                        ) { current, total ->

                            runOnUiThread {

                                indexingProgress =
                                    current.toFloat() / total.toFloat()

                                indexingText =
                                    "Индексация $current из $total"
                            }
                        }

                        runOnUiThread {

                            indexingProgress = 1f

                            indexingText = "Индекс успешно создан"

                            isIndexing = false
                        }

                    } catch (e: Exception) {

                        runOnUiThread {

                            indexingText =
                                e.message ?: "Неизвестная ошибка"

                            isIndexing = false
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {

                rebuildIndex()
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
                            text = "Стратегия Chunking"
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Row {

                            RadioButton(

                                selected =
                                    selectedChunking ==
                                            ChunkingType.PARAGRAPH,

                                onClick = {

                                    selectedChunking =
                                        ChunkingType.PARAGRAPH
                                }
                            )

                            Text("Paragraph")
                        }

                        Row {

                            RadioButton(

                                selected =
                                    selectedChunking ==
                                            ChunkingType.FIXED_SIZE,

                                onClick = {

                                    selectedChunking =
                                        ChunkingType.FIXED_SIZE
                                }
                            )

                            Text("Fixed Size")
                        }

                        if (selectedChunking ==
                            ChunkingType.FIXED_SIZE
                        ) {

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            OutlinedTextField(

                                value = fixedChunkSize,

                                onValueChange = {

                                    fixedChunkSize = it
                                },

                                label = {

                                    Text("Размер chunk")
                                }
                            )
                        }
                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Button(

                            enabled = !isIndexing,

                            onClick = {

                                rebuildIndex()
                            }

                        ) {

                            Text("Переиндексировать")
                        }

                        if (isIndexing) {

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            LinearProgressIndicator(

                                progress = {

                                    indexingProgress

                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(indexingText)

                            Text(

                                "${(indexingProgress * 100).toInt()} %"

                            )

                            Spacer(
                                modifier = Modifier.height(16.dp)
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

                        if (isLoading) {
                            CircularProgressIndicator()
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