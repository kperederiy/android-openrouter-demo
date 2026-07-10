package com.example.aichallenge

interface LlmClient {

    fun generate(

        prompt: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit
    )
}