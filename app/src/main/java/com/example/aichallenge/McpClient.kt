package com.example.aichallenge

import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

class McpClient {

    private val client =
        OkHttpClient()

    private val gson =
        Gson()

    private val serverUrl =
        "http://10.0.2.2:8080/mcp"

    //----------------------------------------------------
    // Общий вызов MCP Tool
    //----------------------------------------------------

    private fun callTool(

        tool: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val json = gson.toJson(

            mapOf(

                "tool" to tool

            )

        )

        val body =

            RequestBody.create(

                "application/json".toMediaType(),

                json

            )

        val request =

            Request.Builder()

                .url(serverUrl)

                .post(body)

                .build()

        client.newCall(request)

            .enqueue(

                object : Callback {

                    override fun onFailure(

                        call: Call,

                        e: IOException

                    ) {

                        onError(

                            e.message ?: "MCP error"

                        )

                    }

                    override fun onResponse(

                        call: Call,

                        response: Response

                    ) {

                        onSuccess(

                            response.body?.string() ?: ""

                        )

                    }

                }

            )

    }

    //----------------------------------------------------
    // Git Branch
    //----------------------------------------------------

    fun getBranch(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            "git_branch",

            onSuccess,

            onError

        )

    }

    //----------------------------------------------------
    // Git Status
    //----------------------------------------------------

    fun getStatus(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            "git_status",

            onSuccess,

            onError

        )

    }

    //----------------------------------------------------
    // Project Files
    //----------------------------------------------------

    fun getFiles(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            "list_files",

            onSuccess,

            onError

        )

    }

    //----------------------------------------------------
    // Git Diff
    //----------------------------------------------------

    fun getDiff(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            "git_diff",

            onSuccess,

            onError

        )

    }

}