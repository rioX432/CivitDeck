package com.riox432.civitdeck

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
