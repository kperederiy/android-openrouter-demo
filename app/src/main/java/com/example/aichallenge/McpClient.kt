package com.example.aichallenge

import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException


class McpClient {


    private val client =
        OkHttpClient()


    private val gson =
        Gson()


    private val serverUrl =
        "http://10.0.2.2:8080/mcp"



    fun callTool(

        tool: String,

        onSuccess: (String)->Unit,

        onError: (String)->Unit

    ) {


        val requestObject =

            mapOf(
                "tool" to tool
            )


        val json =

            gson.toJson(requestObject)



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

                object: Callback {


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


                        val text =

                            response.body
                                ?.string()
                                ?: ""



                        onSuccess(text)

                    }

                }

            )

    }

}