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

        arguments: Map<String, String> = emptyMap(),

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val json = gson.toJson(

            mapOf(

                "tool" to tool,

                "arguments" to arguments

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
    // Git
    //----------------------------------------------------

    fun getBranch(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "git_branch",

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun getStatus(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "git_status",

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun getDiff(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "git_diff",

            onSuccess = onSuccess,

            onError = onError

        )

    }

    //----------------------------------------------------
    // Files
    //----------------------------------------------------

    fun getFiles(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "list_files",

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun readFile(

        path: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "read_file",

            arguments = mapOf(

                "path" to path

            ),

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun searchText(

        text: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "search_text",

            arguments = mapOf(

                "text" to text

            ),

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun writeFile(

        path: String,

        content: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "write_file",

            arguments = mapOf(

                "path" to path,

                "content" to content

            ),

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun updateFile(

        path: String,

        content: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "update_file",

            arguments = mapOf(

                "path" to path,

                "content" to content

            ),

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun replaceText(

        path: String,

        oldText: String,

        newText: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "replace_text",

            arguments = mapOf(

                "path" to path,

                "old" to oldText,

                "new" to newText

            ),

            onSuccess = onSuccess,

            onError = onError

        )

    }

    //----------------------------------------------------
    // CRM
    //----------------------------------------------------

    fun getUsers(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "crm_users",

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun getTickets(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "crm_tickets",

            onSuccess = onSuccess,

            onError = onError

        )

    }

    fun getUserContext(

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        callTool(

            tool = "crm_user_context",

            onSuccess = onSuccess,

            onError = onError

        )

    }

}