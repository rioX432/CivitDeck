package com.riox432.civitdeck.domain.model

data class ModelLicenseInfo(
    val allowNoCredit: Boolean = true,
    val allowCommercialUse: String? = null,
    val allowDerivatives: Boolean = true,
)
