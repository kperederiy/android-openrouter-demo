package com.example.aichallenge


object OllamaConfig {


    const val URL =

        "http://10.0.2.2:11434/api/generate"



    const val MODEL =

        "mistral:7b-instruct-v0.3-q4_K_M"



    /*
        Настройки генерации

        temperature:
        0.0 - максимально точно
        1.0 - больше творчества

        Для RAG лучше низкое значение
    */

    const val TEMPERATURE = 0.2



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