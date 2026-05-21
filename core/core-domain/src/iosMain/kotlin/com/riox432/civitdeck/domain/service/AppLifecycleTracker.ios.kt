package com.riox432.civitdeck.domain.service

import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationState

actual class AppLifecycleTracker {
    actual val isInForeground: Boolean
        get() = UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
}
