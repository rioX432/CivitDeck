package com.riox432.civitdeck.domain.model

enum class BackupCategory(val displayName: String) {
    COLLECTIONS("Collections & Favorites"),
    NOTES("Model Notes"),
    TAGS("Personal Tags"),
    PROMPTS("Saved Prompts"),
    SETTINGS("Settings"),
    SEARCH_FILTERS("Saved Search Filters"),
    FOLLOWED_CREATORS("Followed Creators"),
    CONNECTIONS("Server Connections"),
    HIDDEN_MODELS("Hidden Models & Excluded Tags"),
}

enum class RestoreStrategy(val displayName: String) {
    MERGE("Merge (keep existing, add new)"),
    OVERWRITE("Overwrite (replace all)"),
}
