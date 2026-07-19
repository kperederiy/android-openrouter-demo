package com.example.mcp

class McpRouter(

    private val gitService: GitService,

    private val fileService: FileService,

    private val crmService: CrmService

) {

    fun execute(
        request: McpRequest
    ): McpResponse {

        val args = request.arguments

        val result =

            when (request.tool) {

                //--------------------------------------------------
                // Git
                //--------------------------------------------------

                "git_branch" ->
                    gitService.currentBranch()

                "git_status" ->
                    gitService.status()

                "git_diff" ->
                    gitService.diff()

                //--------------------------------------------------
                // Files
                //--------------------------------------------------

                "list_files" ->
                    fileService.listFiles()

                "read_file" ->

                    fileService.readFile(

                        args["path"] ?: ""

                    )

                "search_text" ->

                    fileService.searchText(

                        text = args["text"] ?: ""

                    )

                "write_file" ->

                    fileService.writeFile(

                        path = args["path"] ?: "",

                        content = args["content"] ?: ""

                    )

                "update_file" ->

                    fileService.updateFile(

                        path = args["path"] ?: "",

                        newContent = args["content"] ?: ""

                    )

                "replace_text" ->

                    fileService.replaceText(

                        path = args["path"] ?: "",

                        oldText = args["old"] ?: "",

                        newText = args["new"] ?: ""

                    )

                //--------------------------------------------------
                // CRM
                //--------------------------------------------------

                "crm_users" ->

                    crmService.loadUsers()

                        .joinToString("\n") {

                            "${it.id} | ${it.name} | ${it.email} | ${it.plan}"

                        }

                "crm_tickets" ->

                    crmService.loadTickets()

                        .joinToString("\n") {

                            "#${it.id}  user=${it.userId}  ${it.title}  ${it.status}"

                        }

                "crm_user_context" ->

                    crmService.buildUserContext(

                        userId = 1

                    )

                //--------------------------------------------------

                else ->

                    "Unknown tool: ${request.tool}"

            }

        return McpResponse(

            success = true,

            result = result

        )

    }

}