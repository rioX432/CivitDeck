package com.riox432.civitdeck.feature.comfyui.data.repository

actual class LocalIpProvider actual constructor() {
    /**
     * On iOS, getting the local IP requires low-level C interop with getifaddrs.
     * For simplicity, we return a common default subnet. Users can also enter
     * a specific subnet range in settings if auto-detection fails.
     * A full implementation would use NWPathMonitor or getifaddrs via cinterop.
     */
    actual fun getLocalSubnet(): String? {
        // Return a common home network subnet as fallback.
        // Full implementation would use NWPathMonitor to detect the actual interface address.
        return "192.168.1"
    }
}
