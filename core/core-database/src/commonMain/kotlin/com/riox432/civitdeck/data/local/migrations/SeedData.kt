@file:Suppress("MaxLineLength")

package com.riox432.civitdeck.data.local.migrations

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Seed data is inserted via onOpen (not in migrations) because Room migrations only run on
// upgrades — a fresh install starts directly at the latest schema version, skipping all
// migration callbacks. Using INSERT OR IGNORE in onOpen ensures required rows are always
// present on every app launch, whether on a new install or after an upgrade.
internal val defaultCollectionCallback = object : RoomDatabase.Callback() {
    override fun onOpen(connection: SQLiteConnection) {
        super.onOpen(connection)
        connection.execSQL(
            "INSERT OR IGNORE INTO `collections` (`id`, `name`, `isDefault`, `createdAt`, `updatedAt`) " +
                "VALUES (1, 'Favorites', 1, 0, 0)",
        )
        seedBuiltInTemplates(connection)
        seedDefaultHashtags(connection)
    }
}

@Suppress("LongMethod")
private fun seedBuiltInTemplates(connection: SQLiteConnection) {
    // INSERT OR IGNORE guarantees idempotency: rows with the same negative IDs are silently
    // skipped if they already exist, so this function is safe to call on every app open.
    // Use UPDATE for existing rows to migrate to new format with metadata and slider types.

    // id=-1 → TXT2IMG built-in template
    upsertBuiltInTemplate(
        connection,
        id = -1,
        name = "txt2img Default",
        type = "TXT2IMG",
        variables = TXT2IMG_VARS,
        metadata = """{"description":"Standard text-to-image generation","category":"GENERAL","version":2,"author":"CivitDeck"}""",
    )
    // id=-2 → IMG2IMG built-in template
    upsertBuiltInTemplate(
        connection,
        id = -2,
        name = "img2img Default",
        type = "IMG2IMG",
        variables = IMG2IMG_VARS,
        metadata = """{"description":"Image-to-image with denoising control","category":"GENERAL","version":2,"author":"CivitDeck"}""",
    )
    // id=-3 → UPSCALE built-in template
    upsertBuiltInTemplate(
        connection,
        id = -3,
        name = "Upscale Default",
        type = "UPSCALE",
        variables = UPSCALE_VARS,
        metadata = """{"description":"Upscale images with adjustable factor","category":"UTILITY","version":2,"author":"CivitDeck"}""",
    )
    // id=-4 → INPAINTING built-in template
    upsertBuiltInTemplate(
        connection,
        id = -4,
        name = "Inpainting Default",
        type = "INPAINTING",
        variables = INPAINTING_VARS,
        metadata = """{"description":"Fill masked areas with AI-generated content","category":"GENERAL","version":1,"author":"CivitDeck"}""",
    )
    // id=-5 → LORA built-in template
    upsertBuiltInTemplate(
        connection,
        id = -5,
        name = "LoRA Default",
        type = "LORA",
        variables = LORA_VARS,
        metadata = """{"description":"Generate with LoRA model injection and strength control","category":"GENERAL","version":1,"author":"CivitDeck"}""",
    )
}

private fun upsertBuiltInTemplate(
    connection: SQLiteConnection,
    id: Int,
    name: String,
    type: String,
    variables: String,
    metadata: String,
) {
    connection.execSQL(
        """INSERT OR REPLACE INTO saved_prompts
            (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, size,
             sourceImageUrl, savedAt, isTemplate, templateName, autoSaved, templateVariables, templateType, templateMetadata)
           VALUES ($id, '', NULL, 'euler', 20, 7.0, -1,
                   NULL, '512x512', NULL, 0, 1, '$name', 0,
                   '$variables',
                   '$type',
                   '$metadata')""",
    )
}

private fun seedDefaultHashtags(connection: SQLiteConnection) {
    val presets = listOf("#AIart", "#ComfyUI", "#StableDiffusion")
    presets.forEach { tag ->
        connection.execSQL(
            "INSERT OR IGNORE INTO share_hashtags (tag, isEnabled, isCustom, addedAt) " +
                "VALUES ('$tag', 1, 0, 0)",
        )
    }
    // Remove legacy Japanese presets that were added in earlier builds
    connection.execSQL("DELETE FROM share_hashtags WHERE tag IN ('#AIイラスト', '#AI画像生成') AND isCustom = 0")
}

// -- Variable JSON constants --

private const val TXT2IMG_VARS =
    """[{"name":"positive_prompt","label":"Prompt","type":"TEXT","defaultValue":"","required":true},{"name":"negative_prompt","label":"Negative Prompt","type":"TEXT","defaultValue":"","required":false},{"name":"checkpoint","label":"Checkpoint","type":"TEXT","defaultValue":"","required":true},{"name":"steps","label":"Steps","type":"SLIDER","defaultValue":"20","min":1.0,"max":150.0,"step":1.0,"required":false},{"name":"cfg","label":"CFG Scale","type":"SLIDER","defaultValue":"7.0","min":1.0,"max":30.0,"step":0.5,"required":false},{"name":"width","label":"Width","type":"SELECT","defaultValue":"512","options":["256","384","512","640","768","832","896","1024","1280"],"required":false},{"name":"height","label":"Height","type":"SELECT","defaultValue":"512","options":["256","384","512","640","768","832","896","1024","1280"],"required":false}]"""

private const val IMG2IMG_VARS =
    """[{"name":"positive_prompt","label":"Prompt","type":"TEXT","defaultValue":"","required":true},{"name":"negative_prompt","label":"Negative Prompt","type":"TEXT","defaultValue":"","required":false},{"name":"checkpoint","label":"Checkpoint","type":"TEXT","defaultValue":"","required":true},{"name":"steps","label":"Steps","type":"SLIDER","defaultValue":"20","min":1.0,"max":150.0,"step":1.0,"required":false},{"name":"cfg","label":"CFG Scale","type":"SLIDER","defaultValue":"7.0","min":1.0,"max":30.0,"step":0.5,"required":false},{"name":"width","label":"Width","type":"SELECT","defaultValue":"512","options":["256","384","512","640","768","832","896","1024","1280"],"required":false},{"name":"height","label":"Height","type":"SELECT","defaultValue":"512","options":["256","384","512","640","768","832","896","1024","1280"],"required":false},{"name":"denoise_strength","label":"Denoise Strength","type":"SLIDER","defaultValue":"0.75","min":0.0,"max":1.0,"step":0.05,"required":false}]"""

private const val UPSCALE_VARS =
    """[{"name":"input_image","label":"Input Image","type":"TEXT","defaultValue":"","required":true},{"name":"upscale_factor","label":"Upscale Factor","type":"SLIDER","defaultValue":"2","min":1.0,"max":4.0,"step":0.5,"required":false}]"""

private const val INPAINTING_VARS =
    """[{"name":"positive_prompt","label":"Prompt","type":"TEXT","defaultValue":"","required":true},{"name":"negative_prompt","label":"Negative Prompt","type":"TEXT","defaultValue":"","required":false},{"name":"checkpoint","label":"Checkpoint","type":"TEXT","defaultValue":"","required":true},{"name":"steps","label":"Steps","type":"SLIDER","defaultValue":"20","min":1.0,"max":150.0,"step":1.0,"required":false},{"name":"cfg","label":"CFG Scale","type":"SLIDER","defaultValue":"7.0","min":1.0,"max":30.0,"step":0.5,"required":false},{"name":"denoise_strength","label":"Denoise Strength","type":"SLIDER","defaultValue":"1.0","min":0.0,"max":1.0,"step":0.05,"required":false}]"""

private const val LORA_VARS =
    """[{"name":"positive_prompt","label":"Prompt","type":"TEXT","defaultValue":"","required":true},{"name":"negative_prompt","label":"Negative Prompt","type":"TEXT","defaultValue":"","required":false},{"name":"checkpoint","label":"Checkpoint","type":"TEXT","defaultValue":"","required":true},{"name":"lora_name","label":"LoRA Model","type":"TEXT","defaultValue":"","required":true},{"name":"lora_strength","label":"LoRA Strength","type":"SLIDER","defaultValue":"0.8","min":0.0,"max":2.0,"step":0.05,"required":false},{"name":"steps","label":"Steps","type":"SLIDER","defaultValue":"20","min":1.0,"max":150.0,"step":1.0,"required":false},{"name":"cfg","label":"CFG Scale","type":"SLIDER","defaultValue":"7.0","min":1.0,"max":30.0,"step":0.5,"required":false},{"name":"width","label":"Width","type":"SELECT","defaultValue":"512","options":["256","384","512","640","768","832","896","1024","1280"],"required":false},{"name":"height","label":"Height","type":"SELECT","defaultValue":"512","options":["256","384","512","640","768","832","896","1024","1280"],"required":false}]"""
