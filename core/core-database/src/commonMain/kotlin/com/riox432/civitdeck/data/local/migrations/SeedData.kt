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
    // id=-1 → TXT2IMG built-in template
    connection.execSQL(
        """INSERT OR IGNORE INTO saved_prompts
            (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, size,
             sourceImageUrl, savedAt, isTemplate, templateName, autoSaved, templateVariables, templateType)
           VALUES (-1, '{positive_prompt}', '{negative_prompt}', 'euler', 20, 7.0, -1,
                   '{checkpoint}', '{width}x{height}', NULL, 0, 1, 'txt2img Default', 0,
                   '[{"name":"positive_prompt","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"negative_prompt","type":"TEXT","defaultValue":"","options":[],"required":false},{"name":"checkpoint","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"steps","type":"NUMBER","defaultValue":"20","options":[],"required":false},{"name":"cfg","type":"NUMBER","defaultValue":"7.0","options":[],"required":false},{"name":"width","type":"NUMBER","defaultValue":"512","options":[],"required":false},{"name":"height","type":"NUMBER","defaultValue":"512","options":[],"required":false}]',
                   'TXT2IMG')""",
    )
    // id=-2 → IMG2IMG built-in template
    connection.execSQL(
        """INSERT OR IGNORE INTO saved_prompts
            (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, size,
             sourceImageUrl, savedAt, isTemplate, templateName, autoSaved, templateVariables, templateType)
           VALUES (-2, '{positive_prompt}', '{negative_prompt}', 'euler', 20, 7.0, -1,
                   '{checkpoint}', '{width}x{height}', NULL, 0, 1, 'img2img Default', 0,
                   '[{"name":"positive_prompt","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"negative_prompt","type":"TEXT","defaultValue":"","options":[],"required":false},{"name":"checkpoint","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"steps","type":"NUMBER","defaultValue":"20","options":[],"required":false},{"name":"cfg","type":"NUMBER","defaultValue":"7.0","options":[],"required":false},{"name":"width","type":"NUMBER","defaultValue":"512","options":[],"required":false},{"name":"height","type":"NUMBER","defaultValue":"512","options":[],"required":false},{"name":"denoise_strength","type":"NUMBER","defaultValue":"0.75","options":[],"required":false}]',
                   'IMG2IMG')""",
    )
    // id=-3 → UPSCALE built-in template
    connection.execSQL(
        """INSERT OR IGNORE INTO saved_prompts
            (id, prompt, negativePrompt, sampler, steps, cfgScale, seed, modelName, size,
             sourceImageUrl, savedAt, isTemplate, templateName, autoSaved, templateVariables, templateType)
           VALUES (-3, '{input_image}', NULL, 'euler', 20, 7.0, -1,
                   NULL, '512x512', NULL, 0, 1, 'Upscale Default', 0,
                   '[{"name":"input_image","type":"TEXT","defaultValue":"","options":[],"required":true},{"name":"upscale_factor","type":"NUMBER","defaultValue":"2","options":[],"required":false}]',
                   'UPSCALE')""",
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
