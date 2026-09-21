package com.farsifloatvoice

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityTypingService : AccessibilityService() {
    companion object {
        @Volatile private var instance: AccessibilityTypingService? = null
        fun insertText(text: String) { instance?.insert(text) }
    }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun insert(text: String) {
        val root = rootInActiveWindow ?: return
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        if (!node.isEditable) return
        val current = node.text?.toString() ?: ""
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        val a = if (start >= 0) minOf(start, current.length) else current.length
        val b = if (end >= 0) minOf(end, current.length) else a
        val updated = current.substring(0, a) + text + current.substring(b)
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}
