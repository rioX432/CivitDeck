package com.riox432.civitdeck.domain.model

enum class PollingInterval(val minutes: Int, val displayName: String) {
    Off(0, "Off"),
    FifteenMinutes(15, "15 minutes"),
    ThirtyMinutes(30, "30 minutes"),
    OneHour(60, "1 hour");

    companion object {
        fun fromMinutes(minutes: Int): PollingInterval =
            entries.find { it.minutes == minutes } ?: Off
    }
}
