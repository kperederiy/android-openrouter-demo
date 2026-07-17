package com.example.mcp

class McpRouter(

    private val gitService: GitService,

    private val fileService: FileService,

    private val crmService: CrmService

) {

    fun execute(
        request: McpRequest
    ): McpResponse {

        val result =

            when (request.tool) {

                //----------------------------------
                // Git
                //----------------------------------

                "git_branch" ->
                    gitService.currentBranch()

                "git_status" ->
                    gitService.status()

                "git_diff" ->
                    gitService.diff()

                //----------------------------------
                // Files
                //----------------------------------

                "list_files" ->
                    fileService.listFiles()

                //----------------------------------
                // CRM
                //----------------------------------

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

                //----------------------------------

                else ->

                    "Unknown tool: ${request.tool}"

            }

        return McpResponse(
            success = true,
            result = result
        )
    }
}