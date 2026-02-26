package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_prompts")
data class SavedPromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val negativePrompt: String?,
    val sampler: String?,
    val steps: Int?,
    val cfgScale: Double?,
    val seed: Long?,
    val modelName: String?,
    val size: String?,
    val sourceImageUrl: String?,
    val savedAt: Long,
    val isTemplate: Boolean = false,
    val templateName: String? = null,
    val autoSaved: Boolean = false,
)
