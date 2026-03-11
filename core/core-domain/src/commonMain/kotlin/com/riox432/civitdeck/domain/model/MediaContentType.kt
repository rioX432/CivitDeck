package com.riox432.civitdeck.domain.model

enum class MediaContentType {
    IMAGE, VIDEO, ANIMATION;

    companion object {
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov")
        private val ANIMATION_EXTENSIONS = setOf("gif")

        fun fromUrl(url: String): MediaContentType {
            val ext = url.substringBefore("?").substringAfterLast(".").lowercase()
            return when (ext) {
                in VIDEO_EXTENSIONS -> VIDEO
                in ANIMATION_EXTENSIONS -> ANIMATION
                else -> IMAGE
            }
        }

        fun fromApiType(type: String?): MediaContentType? = when (type?.lowercase()) {
            "video" -> VIDEO
            "image" -> IMAGE
            "animation" -> ANIMATION
            else -> null
        }
    }
}
