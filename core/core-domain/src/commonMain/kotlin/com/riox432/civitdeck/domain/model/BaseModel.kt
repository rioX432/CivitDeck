package com.riox432.civitdeck.domain.model

/**
 * A CivitAI base model value, sent to the API unchanged as `baseModels`.
 *
 * Not an enum: CivitAI adds base models every week, and a saved filter or backup can hold any
 * of them. Not a `value class`: Kotlin/Native does not export inline classes to Swift, and iOS
 * reads `selectedBaseModels` directly.
 */
data class BaseModel(val apiValue: String) {
    val displayName: String get() = apiValue

    companion object {
        val DEFAULT_OPTIONS: List<BaseModel> = listOf(
            "SD 1.5",
            "SDXL 1.0",
            "Pony",
            "Flux.1 D",
            "Flux.1 S",
            "SD 2.1",
            "SVD",
        ).map(::BaseModel)

        /**
         * [DEFAULT_OPTIONS] plus any selected value outside them, so it stays visible and
         * deselectable. Extras are sorted because a set coming from Swift has no stable order.
         */
        fun filterOptions(selected: Set<BaseModel>): List<BaseModel> =
            DEFAULT_OPTIONS + selected.filterNot { it in DEFAULT_OPTIONS }.sortedBy { it.apiValue }
    }
}
