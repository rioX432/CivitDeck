package com.riox432.civitdeck.feature.comfyui.data.repository

/**
 * iOS [LocalIpProvider] reads the Wi-Fi (en0) subnet, but onboarding has no iOS
 * finished/empty state for a scan yet, so it stays hidden there; iOS users connect via
 * QR or manual entry during onboarding and can use "Scan LAN" in ComfyUI settings.
 */
actual val lanScanSupported: Boolean = false
