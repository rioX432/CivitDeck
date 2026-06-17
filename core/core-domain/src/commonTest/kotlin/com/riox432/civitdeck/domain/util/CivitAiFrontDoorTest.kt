package com.riox432.civitdeck.domain.util

import com.riox432.civitdeck.domain.model.FrontDoorMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CivitAiFrontDoorTest {

    @Test
    fun defaultWebHostIsSfwBeforeAnyEmission() {
        val frontDoor = CivitAiFrontDoor()
        assertEquals(FrontDoorMode.Sfw.webHost, frontDoor.webHost.value)
    }

    @Test
    fun togglingFrontDoorModeUpdatesWebHost() = runTest {
        val frontDoor = CivitAiFrontDoor()
        val modeFlow = MutableStateFlow(FrontDoorMode.Sfw)
        // UnconfinedTestDispatcher collects eagerly; backgroundScope's job cancels
        // the never-completing collector when the test ends.
        val collectorScope = CoroutineScope(
            backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler),
        )
        frontDoor.start(modeFlow, collectorScope)
        assertEquals("https://civitai.com", frontDoor.webHost.value)

        modeFlow.value = FrontDoorMode.Full
        assertEquals("https://civitai.red", frontDoor.webHost.value)

        modeFlow.value = FrontDoorMode.Sfw
        assertEquals("https://civitai.com", frontDoor.webHost.value)
    }
}
