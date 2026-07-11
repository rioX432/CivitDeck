package com.riox432.civitdeck.domain.model

enum class NsfwFilterLevel {
    Off,
    Soft,
    All,
}

/**
 * Value for the CivitAI `/models` `nsfw` query parameter. The parameter is
 * boolean-only; when it is omitted the API strips non-PG images from the
 * response (leaving NSFW-capable models with empty image arrays), so it must
 * always be sent explicitly. Level filtering finer than on/off happens
 * client-side via [filterNsfwImages].
 */
fun NsfwFilterLevel.includeNsfwModels(): Boolean = this != NsfwFilterLevel.Off

/**
 * Inclusive upper bound for the CivitAI `/images` `nsfw` query parameter.
 * Omitting the parameter defaults to PG-only server-side, so [NsfwFilterLevel.All]
 * must explicitly request [NsfwLevel.X] (which includes XXX content).
 */
fun NsfwFilterLevel.imagesQueryMaxLevel(): NsfwLevel = when (this) {
    NsfwFilterLevel.Off -> NsfwLevel.None
    NsfwFilterLevel.Soft -> NsfwLevel.Soft
    NsfwFilterLevel.All -> NsfwLevel.X
}
