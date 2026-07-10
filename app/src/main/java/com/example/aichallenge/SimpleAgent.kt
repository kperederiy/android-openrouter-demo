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
    // текущий провайдер
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

            LlmProvider.OPEN_ROUTER ->

                openRouterClient.generate(

                    prompt = userRequest,

                    onSuccess = onSuccess,

                    onError = onError

                )

            //--------------------------------------------------

            LlmProvider.OLLAMA ->

                ollamaClient.generate(

                    prompt = userRequest,

                    onSuccess = onSuccess,

                    onError = onError

                )
        }
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

    fun isUsingOllama(): Boolean {

        return provider ==

                LlmProvider.OLLAMA
    }

    //--------------------------------------------------

    fun isUsingOpenRouter(): Boolean {

        return provider ==

                LlmProvider.OPEN_ROUTER
    }
}