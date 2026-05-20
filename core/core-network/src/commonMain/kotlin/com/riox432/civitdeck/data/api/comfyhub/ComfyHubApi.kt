package com.riox432.civitdeck.data.api.comfyhub

/**
 * API client for browsing published ComfyUI workflows.
 *
 * Since ComfyHub does not have a stable public API, this implementation
 * provides a curated set of built-in workflows and supports searching
 * by name, category, and tags. This can be swapped out for a real API
 * when one becomes available.
 */
class ComfyHubApi {

    /**
     * Search workflows with optional query, category filter, and sort order.
     */
    fun searchWorkflows(
        query: String = "",
        category: String = "All",
        sort: String = "Most Downloaded",
        page: Int = 1,
    ): ComfyHubSearchResponse {
        val filtered = builtInWorkflows.filter { workflow ->
            val matchesQuery = query.isBlank() ||
                workflow.name.contains(query, ignoreCase = true) ||
                workflow.description.contains(query, ignoreCase = true) ||
                workflow.tags.any { it.contains(query, ignoreCase = true) }
            val matchesCategory = category == "All" ||
                workflow.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }.let { list ->
            when (sort) {
                "Highest Rated" -> list.sortedByDescending { it.rating }
                "Newest" -> list.reversed()
                else -> list.sortedByDescending { it.downloads }
            }
        }
        return ComfyHubSearchResponse(
            items = filtered,
            metadata = ComfyHubPagination(
                totalItems = filtered.size,
                currentPage = page,
                totalPages = 1,
            ),
        )
    }

    /**
     * Get detailed workflow including the JSON definition.
     */
    fun getWorkflowDetail(workflowId: String): ComfyHubWorkflowDto {
        return builtInWorkflows.firstOrNull { it.id == workflowId }
            ?: error("Workflow not found: $workflowId")
    }
}
