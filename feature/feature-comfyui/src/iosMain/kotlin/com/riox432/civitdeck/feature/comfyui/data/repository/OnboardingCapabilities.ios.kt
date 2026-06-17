package com.riox432.civitdeck.feature.comfyui.data.repository

/**
 * iOS [LocalIpProvider] returns a hardcoded fallback subnet, so LAN auto-scan is
 * unreliable. The onboarding flow hides it; iOS users connect via QR or manual entry.
 */
actual val lanScanSupported: Boolean = false
