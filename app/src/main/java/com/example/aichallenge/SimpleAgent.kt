package com.example.aichallenge

class SimpleAgent(

    apiKey: String

) {

    //--------------------------------------------------
    // OpenRouter
    //--------------------------------------------------

    private val openRouterClient =

        OpenRouterClient(

            apiKey = apiKey

        )

    //--------------------------------------------------
    // Ollama
    //--------------------------------------------------

    private val ollamaClient =

        OllamaClient()

    //--------------------------------------------------
    // Prompt Builder
    //--------------------------------------------------

    private val localPromptBuilder =

        LocalPromptBuilder()

    //--------------------------------------------------
    // История локального чата
    //--------------------------------------------------

    private val conversationHistory =

        ConversationHistory()

    //--------------------------------------------------
    // Текущий провайдер
    //--------------------------------------------------

    var provider =

        LlmProvider.OPEN_ROUTER

    //--------------------------------------------------

    fun processRequest(

        userRequest: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        when (provider) {

            //--------------------------------------------------
            // OpenRouter
            //--------------------------------------------------

            LlmProvider.OPEN_ROUTER -> {

                openRouterClient.generate(

                    prompt = userRequest,

                    onSuccess = onSuccess,

                    onError = onError

                )
            }

            //--------------------------------------------------
            // Ollama Chat
            //--------------------------------------------------

            LlmProvider.OLLAMA -> {

                val prompt =

                    localPromptBuilder.buildRagPrompt(

                        userRequest

                    )

                //--------------------------------------------------
                // добавляем вопрос пользователя
                //--------------------------------------------------

                conversationHistory.addUser(

                    prompt

                )

                //--------------------------------------------------
                // отправляем всю историю
                //--------------------------------------------------

                ollamaClient.chat(

                    messages =

                        conversationHistory.getMessages(),

                    onSuccess = { answer ->

                        //--------------------------------------------------
                        // сохраняем ответ
                        //--------------------------------------------------

                        conversationHistory.addAssistant(

                            answer

                        )

                        onSuccess(answer)
                    },

                    onError = onError

                )
            }
        }
    }

    //--------------------------------------------------
    // Очистить историю
    //--------------------------------------------------

    fun clearConversation() {

        conversationHistory.clear()

    }

    //--------------------------------------------------

    fun useOpenRouter() {

        provider =

            LlmProvider.OPEN_ROUTER

    }

    //--------------------------------------------------

    fun useOllama() {

        provider =

            LlmProvider.OLLAMA

    }

    //--------------------------------------------------

    fun isUsingOllama(): Boolean =

        provider == LlmProvider.OLLAMA

    //--------------------------------------------------

    fun isUsingOpenRouter(): Boolean =

        provider == LlmProvider.OPEN_ROUTER

}