package com.riox432.civitdeck.domain.model

/**
 * Where a [BaseModel] stands in CivitAI's catalog. It describes the catalog only: many retired
 * values still return models, and a value CivitAI added after the last catalog update is
 * [Unknown] until the next one.
 */
enum class BaseModelStatus {
    /** In `ActiveBaseModel`: creators can still pick it for a new upload. */
    Active,

    /** In `BaseModel` but not in `ActiveBaseModel`. */
    Retired,

    /** In neither list, e.g. removed by CivitAI or restored from a newer backup. */
    Unknown,
}

/**
 * CivitAI's base model catalog from `GET /api/v1/enums`: [active] is `ActiveBaseModel`, [retired]
 * is the rest of `BaseModel`. Both keep the API order, which keeps families together.
 */
data class BaseModelCatalog(
    val active: List<BaseModel>,
    val retired: List<BaseModel>,
) {
    private val activeSet = active.toSet()
    private val retiredSet = retired.toSet()

    fun statusOf(baseModel: BaseModel): BaseModelStatus = when (baseModel) {
        in activeSet -> BaseModelStatus.Active
        in retiredSet -> BaseModelStatus.Retired
        else -> BaseModelStatus.Unknown
    }
}
