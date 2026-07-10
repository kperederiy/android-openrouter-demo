package com.example.aichallenge


class SimpleAgent(

    apiKey: String

) {


    private val openRouterClient =

        OpenRouterClient(

            apiKey = apiKey

        )



    private val ollamaClient =

        OllamaClient()



    private val localPromptBuilder =

        LocalPromptBuilder()



    var provider =

        LlmProvider.OPEN_ROUTER



    fun processRequest(


        userRequest: String,


        onSuccess: (String) -> Unit,


        onError: (String) -> Unit


    ) {


        when(provider) {



            LlmProvider.OPEN_ROUTER -> {


                openRouterClient.generate(

                    prompt = userRequest,

                    onSuccess = onSuccess,

                    onError = onError

                )

            }



            LlmProvider.OLLAMA -> {


                val localPrompt =

                    localPromptBuilder.buildRagPrompt(

                        userRequest

                    )



                ollamaClient.generate(

                    prompt = localPrompt,

                    onSuccess = onSuccess,

                    onError = onError

                )

            }

        }

    }



    fun useOpenRouter(){

        provider =

            LlmProvider.OPEN_ROUTER

    }



    fun useOllama(){

        provider =

            LlmProvider.OLLAMA

    }



    fun isUsingOllama(): Boolean =

        provider == LlmProvider.OLLAMA



    fun isUsingOpenRouter(): Boolean =

        provider == LlmProvider.OPEN_ROUTER

}