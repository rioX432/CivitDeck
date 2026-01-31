package com.omooooori.civitdeck

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
