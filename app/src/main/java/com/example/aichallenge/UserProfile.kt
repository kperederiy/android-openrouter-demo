package com.example.aichallenge

data class UserProfile(

    var name: String = "",

    var responseStyle: String =
        "neutral",

    var responseFormat: String =
        "detailed",

    var restrictions: String =
        ""
)