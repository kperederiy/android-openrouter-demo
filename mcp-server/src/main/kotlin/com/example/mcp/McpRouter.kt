package com.example.mcp

class McpRouter(

    private val gitService: GitService,

    private val fileService: FileService

) {

    fun execute(
        request: McpRequest
    ): McpResponse {

        val result =

            when (request.tool) {

                "git_branch" ->
                    gitService.currentBranch()

                "git_status" ->
                    gitService.status()

                "git_diff" ->
                    gitService.diff()

                "list_files" ->
                    fileService.listFiles()

                else ->
                    "Unknown tool: ${request.tool}"
            }

        return McpResponse(
            success = true,
            result = result
        )
    }
}