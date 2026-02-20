package com.macrorecorder.app.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Central accessibility service used for two purposes:
 *  1. Recording — capturing touch events (via overlay interception) while the service is active.
 *  2. Playback  — dispatching synthesised gestures via [dispatchGesture].
 *
 * canPerformGestures="true" is declared in accessibility_service_config.xml.
 */
class MacroRecorderAccessibilityService : AccessibilityService() {

    companion object {
        /** Live reference used by PlaybackManager to dispatch gestures. */
        var instance: MacroRecorderAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Touch-event capture is handled via the overlay window (OverlayWidgetService),
        // not through AccessibilityEvents. This callback is reserved for future use.
    }

    override fun onInterrupt() {
        // Called when the system interrupts the feedback this service is providing.
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
