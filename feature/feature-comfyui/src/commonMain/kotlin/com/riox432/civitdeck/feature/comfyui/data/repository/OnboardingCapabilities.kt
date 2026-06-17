package com.riox432.civitdeck.feature.comfyui.data.repository

/**
 * Whether reliable LAN auto-detection is available on this platform.
 *
 * Android/Desktop enumerate network interfaces to find the real subnet. iOS uses a
 * hardcoded fallback subnet ([LocalIpProvider] stub), so auto-scan is unreliable there
 * and the onboarding flow hides it.
 */
expect val lanScanSupported: Boolean
