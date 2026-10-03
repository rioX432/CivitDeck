package com.riox432.civitdeck.feature.comfyui.data.repository

/**
 * Whether the onboarding flow offers LAN auto-detection on this platform.
 *
 * Every platform's [LocalIpProvider] derives the subnet from a real interface address.
 * iOS still returns false because its onboarding has no finished/empty state for a scan.
 */
expect val lanScanSupported: Boolean
