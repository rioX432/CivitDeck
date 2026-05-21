package com.riox432.civitdeck.domain.service

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual class GenerationNotificationService {

    actual fun notifyGenerationComplete(promptId: String, imageCount: Int, elapsedMs: Long) {
        val elapsedSec = elapsedMs / MILLIS_PER_SECOND
        val content = UNMutableNotificationContent().apply {
            setTitle("Generation Complete")
            val suffix = if (imageCount == 1) "" else "s"
            setBody("$imageCount image$suffix generated in ${elapsedSec}s")
            setSound(UNNotificationSound.defaultSound)
        }
        scheduleNotification(promptId, content)
    }

    actual fun notifyGenerationError(promptId: String, errorMessage: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Generation Failed")
            setBody(errorMessage)
            setSound(UNNotificationSound.defaultSound)
        }
        scheduleNotification(promptId, content)
    }

    private fun scheduleNotification(promptId: String, content: UNMutableNotificationContent) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val identifier = "generation-$promptId-${NSDate().timeIntervalSince1970}"
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = null,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    private companion object {
        private const val MILLIS_PER_SECOND = 1000L
    }
}
