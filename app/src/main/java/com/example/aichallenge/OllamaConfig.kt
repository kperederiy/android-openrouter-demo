package com.example.aichallenge


object OllamaConfig {


    const val URL =
        "https://overeager-syrup-gawk.ngrok-free.dev/api/chat"



    const val MODEL =

        "mistral:7b-instruct-v0.3-q4_K_M"



    /*
        Настройки генерации

        temperature:
        0.0 - максимально точно
        1.0 - больше творчества

        Для RAG лучше низкое значение
    */

    const val TEMPERATURE = 0.5



    /*
        Ограничение длины ответа
    */

    const val MAX_TOKENS = 512



    /*
        Размер контекста модели

        Mistral 7B поддерживает большие окна,
        но для Android RAG нам достаточно
    */

    const val NUM_CTX = 4096

}