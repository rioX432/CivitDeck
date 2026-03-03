package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.repository.ModelRepository

class GetModelLicenseUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(versionId: Long): ModelLicenseInfo? =
        repository.getModelLicense(versionId)
}
