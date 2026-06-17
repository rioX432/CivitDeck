package com.riox432.civitdeck.domain.service

import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationState

class AppLifecycleTrackerImpl : AppLifecycleTracker {
    override val isInForeground: Boolean
        get() = UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
}
