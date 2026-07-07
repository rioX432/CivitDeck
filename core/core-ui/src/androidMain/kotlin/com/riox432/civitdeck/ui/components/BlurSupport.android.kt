package com.riox432.civitdeck.ui.components

import android.os.Build

actual fun isBlurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
