package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.service.GenerationNotificationService

/**
 * No-op [GenerationNotificationService] for tests, recording the last calls so
 * tests can assert that notifications were (or were not) fired.
 */
class FakeGenerationNotificationService : GenerationNotificationService {
    var lastCompletePromptId: String? = null
    var lastErrorPromptId: String? = null

    override fun notifyGenerationComplete(promptId: String, imageCount: Int, elapsedMs: Long) {
        lastCompletePromptId = promptId
    }

    override fun notifyGenerationError(promptId: String, errorMessage: String) {
        lastErrorPromptId = promptId
    }
}
