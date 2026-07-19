package com.example.aichallenge

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
class RagAgent(
    private val context: Context,
    private val embeddingService: EmbeddingService,
    private val simpleAgent: SimpleAgent,
    private val mcpClient: McpClient
) {
    private val indexSearcher = IndexSearcher(
        context = context,
        embeddingService = embeddingService
    )
    private val promptBuilder = PromptBuilder()
    private val developerPromptBuilder =
        DeveloperPromptBuilder()
    private val supportPromptBuilder =
        SupportPromptBuilder()
    private val queryRewriter = QueryRewriter()
    private val router =
        AgentRouter()

    // Снижаем порог до 0.3
    private val similarityFilter = SimilarityFilter(
        threshold = 0.3,  // Значительно снижаем порог
        topKAfter = 3
    )

    private val chatMemory = ChatMemory()

    fun processRequest(
        question: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val intent =

            router.detectIntent(question)

        when (intent) {

            AgentIntent.HELP -> {

                handleHelp(

                    question,

                    onSuccess,

                    onError

                )

                return

            }

            AgentIntent.REVIEW -> {

                handleReview(

                    onSuccess,

                    onError

                )

                return

            }

            AgentIntent.SUPPORT -> {

                handleSupport(

                    question,

                    onSuccess,

                    onError

                )

                return

            }

            AgentIntent.FILES -> {

                handleFiles(

                    question,

                    onSuccess,

                    onError

                )

                return

            }

            AgentIntent.RAG -> {

                Thread {
                    try {
                        Log.d("RagAgent", "Processing question: $question")
                        chatMemory.addUserMessage(question)

                        val rewrittenQuestion = queryRewriter.rewrite(question)
                        Log.d("RagAgent", "Rewritten: $rewrittenQuestion")

                        val searchResults = runBlocking {
                            indexSearcher.search(
                                question = rewrittenQuestion,
                                topK = 10
                            )
                        }

                        Log.d("RagAgent", "Found ${searchResults.size} results")

                        // Логируем максимальное сходство
                        if (searchResults.isNotEmpty()) {
                            val maxSim = searchResults.maxOf { it.similarity }
                            Log.d("RagAgent", "Max similarity: $maxSim")
                        }

                        // Фильтруем результаты
                        val filteredResults = similarityFilter.filter(searchResults)
                        Log.d("RagAgent", "Filtered to ${filteredResults.size} results")

                        val chunks = filteredResults.map { it.chunk }

                        // ✅ ИСПРАВЛЕНО: Если нет релевантных чанков - возвращаем "Не знаю"
                        // БЕЗ вызова SimpleAgent как fallback
                        if (chunks.isEmpty()) {
                            Log.w("RagAgent", "No relevant chunks found (similarity < threshold)")

                            val answer = """
Ответ:
Не знаю. Пожалуйста, уточните вопрос.

Источники:
нет

Цитаты:
нет
                    """.trimIndent()

                            chatMemory.addAssistantMessage(answer)
                            onSuccess(answer)
                            return@Thread
                        }

                        // Строим промпт ТОЛЬКО с релевантными чанками
                        val prompt = promptBuilder.buildPrompt(
                            question = question,
                            history = chatMemory.getMessages(),
                            chunks = chunks
                        )

                        Log.d("RagAgent", "Prompt length: ${prompt.length}")

                        // Используем SimpleAgent ТОЛЬКО для генерации ответа на основе контекста
                        simpleAgent.processRequest(
                            userRequest = prompt,
                            onSuccess = { answer ->
                                Log.d("RagAgent", "LLM answered successfully")
                                chatMemory.addAssistantMessage(answer)
                                onSuccess(answer)
                            },
                            onError = { error ->
                                Log.e("RagAgent", error)
                                onError(error)
                            }
                        )

                    } catch (e: Exception) {
                        Log.e("RagAgent", "Error", e)
                        onError(e.message ?: "Ошибка RAG")
                    }
                }.start()

            }

        }
    }

    private fun handleFiles(

        question: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val command =

            question.removePrefix("/files")
                .trim()

        when {

            command.startsWith("find") -> {

                handleFindUsages(

                    command,

                    onSuccess,

                    onError

                )

            }

            command.startsWith("update readme") -> {

                handleUpdateReadme(

                    onSuccess,

                    onError

                )

            }

            command.startsWith("generate changelog") -> {

                handleGenerateChangelog(

                    onSuccess,

                    onError

                )

            }

            command.startsWith("check") -> {

                handleCheckProject(

                    onSuccess,

                    onError

                )

            }

            else -> {

                onSuccess(

                    """
Доступные команды:

/files find <Component>

/files update readme

/files generate changelog

/files check
                """.trimIndent()

                )

            }

        }

    }

    private fun handleFindUsages(

        command: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val text =

            command.removePrefix("find").trim()

        mcpClient.searchText(

            text = text,

            onSuccess = onSuccess,

            onError = onError

        )

    }

    private fun handleUpdateReadme(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        mcpClient.readFile(

            path = "app/src/main/assets/README.md",

            onSuccess = { readme ->

                mcpClient.getDiff(

                    onSuccess = { diff ->

                        mcpClient.getFiles(

                            onSuccess = { files ->

                                val prompt = """

Обнови README проекта.

Текущий README:

$readme

Изменения Git:

$diff

Файлы проекта:

$files

Верни только новый README.

""".trimIndent()

                                simpleAgent.processRequest(

                                    prompt,

                                    onSuccess = { newReadme ->

                                        mcpClient.updateFile(

                                            path = "app/src/main/assets/README.md",

                                            content = newReadme,

                                            onSuccess = onSuccess,

                                            onError = onError

                                        )

                                    },

                                    onError = onError

                                )

                            },

                            onError

                        )

                    },

                    onError

                )

            },

            onError

        )

    }

    private fun handleGenerateChangelog(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        mcpClient.getDiff(

            onSuccess = { diff ->

                val prompt = """

Сгенерируй CHANGELOG.md.

Git diff:

$diff

Верни только markdown.

""".trimIndent()

                simpleAgent.processRequest(

                    prompt,

                    onSuccess = { markdown ->

                        mcpClient.writeFile(

                            path = "CHANGELOG.md",

                            content = markdown,

                            onSuccess = onSuccess,

                            onError = onError

                        )

                    },

                    onError

                )

            },

            onError

        )

    }

    private fun handleCheckProject(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        mcpClient.getFiles(

            onSuccess = { files ->

                val prompt = """

Проверь проект.

Файлы:

$files

Найди:

- отсутствующий README

- отсутствующий CHANGELOG

- отсутствующий LICENSE

- подозрительные файлы

- нарушения структуры

""".trimIndent()

                simpleAgent.processRequest(

                    prompt,

                    onSuccess,

                    onError

                )

            },

            onError

        )

    }

    private fun handleSupport(

        question: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        Thread {

            try {

                val realQuestion =

                    question.removePrefix("/support").trim()

                //--------------------------------------------------
                // RAG
                //--------------------------------------------------

                val results = runBlocking {

                    indexSearcher.search(

                        question = realQuestion,

                        topK = 5

                    )

                }

                val documentation = buildString {

                    results.forEach {

                        appendLine(it.chunk.text)

                        appendLine()

                    }

                }

                //--------------------------------------------------
                // CRM
                //--------------------------------------------------

                mcpClient.getUserContext(

                    onSuccess = { user ->

                        mcpClient.getTickets(

                            onSuccess = { tickets ->

                                val prompt =

                                    supportPromptBuilder.buildPrompt(

                                        question = realQuestion,

                                        documentation = documentation,

                                        userContext = user,

                                        tickets = tickets

                                    )

                                simpleAgent.processRequest(

                                    userRequest = prompt,

                                    onSuccess = onSuccess,

                                    onError = onError

                                )

                            },

                            onError = onError

                        )

                    },

                    onError = onError

                )

            }

            catch (e: Exception) {

                onError(

                    e.message ?: "Support error"

                )

            }

        }.start()

    }

    private fun handleReview(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        //--------------------------------------------------
        // Получаем текущую ветку
        //--------------------------------------------------

        mcpClient.getBranch(

            onSuccess = { branch ->

                //--------------------------------------------------
                // Получаем git diff
                //--------------------------------------------------

                mcpClient.getDiff(

                    onSuccess = { diff ->

                        //--------------------------------------------------
                        // Получаем список файлов
                        //--------------------------------------------------

                        mcpClient.getFiles(

                            onSuccess = { files ->

                                //--------------------------------------------------
                                // Получаем документацию (RAG)
                                //--------------------------------------------------

                                buildProjectDocumentation(

                                    onSuccess = { chunks ->

                                        val prompt =

                                            promptBuilder.buildReviewPrompt(

                                                gitBranch = branch,

                                                gitDiff = diff,

                                                projectFiles = files,

                                                chunks = chunks

                                            )

                                        simpleAgent.processRequest(

                                            userRequest = prompt,

                                            onSuccess = onSuccess,

                                            onError = onError

                                        )

                                    },

                                    onError = onError

                                )

                            },

                            onError = onError

                        )

                    },

                    onError = onError

                )

            },

            onError = onError

        )

    }
    private fun handleHelp(
        question: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        Thread {

            try {

                //--------------------------------------------------
                // Убираем команду /help
                //--------------------------------------------------

                val realQuestion =
                    question.removePrefix("/help").trim()

                //--------------------------------------------------
                // Ищем документацию
                //--------------------------------------------------

                val results = runBlocking {

                    indexSearcher.search(

                        question = realQuestion,

                        topK = 3

                    )

                }

                //--------------------------------------------------
                // Собираем документацию
                //--------------------------------------------------

                val documentation = buildString {

                    results.forEach {

                        appendLine(
                            "Файл: ${it.chunk.fileName}"
                        )

                        appendLine()

                        appendLine(
                            it.chunk.text
                        )

                        appendLine()

                        appendLine("--------------------------------")

                    }

                }

                //--------------------------------------------------
                // Получаем список файлов
                //--------------------------------------------------

                //--------------------------------------------------
// Получаем Git Branch
//--------------------------------------------------

                mcpClient.getBranch(

                    onSuccess = { branch ->

                        //--------------------------------------------------
                        // Затем получаем список файлов
                        //--------------------------------------------------

                        mcpClient.getFiles(

                            onSuccess = { files ->

                                val prompt =

                                    developerPromptBuilder.buildPrompt(

                                        question = realQuestion,

                                        documentation = documentation,

                                        projectFiles = files,

                                        gitBranch = branch

                                    )

                                simpleAgent.processRequest(

                                    userRequest = prompt,

                                    onSuccess = onSuccess,

                                    onError = onError

                                )

                            },

                            onError = onError

                        )

                    },

                    onError = onError

                )

            }

            catch (e: Exception) {

                onError(

                    e.message ?: "Ошибка"

                )

            }

        }.start()

    }

    fun buildProjectDocumentation(

        onSuccess: (List<Chunk>) -> Unit,

        onError: (String) -> Unit

    ) {

        Thread {

            try {

                //--------------------------------------------------
                // Ищем документацию проекта
                //--------------------------------------------------

                val searchResults = runBlocking {

                    indexSearcher.search(

                        question = "README project documentation architecture api docs",

                        topK = 20

                    )

                }

                //--------------------------------------------------
                // Оставляем только релевантные документы
                //--------------------------------------------------

                val filtered =

                    similarityFilter.filter(searchResults)

                //--------------------------------------------------
                // Если ничего нет
                //--------------------------------------------------

                if (filtered.isEmpty()) {

                    onSuccess(emptyList())

                    return@Thread

                }

                //--------------------------------------------------
                // Возвращаем Chunk
                //--------------------------------------------------

                val chunks =

                    filtered.map {

                        it.chunk

                    }

                onSuccess(chunks)

            }

            catch (e: Exception) {

                onError(

                    e.message ?: "Ошибка RAG"

                )

            }

        }.start()

    }

    fun clearMemory() {
        chatMemory.clear()
    }

    fun getHistory(): List<ChatMessage> {
        return chatMemory.getMessages()
    }
}