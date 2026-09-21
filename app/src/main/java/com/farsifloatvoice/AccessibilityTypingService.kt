package com.farsifloatvoice

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityTypingService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: AccessibilityTypingService? = null

        fun beginDictation(): Boolean = instance?.begin() == true
        fun updateDictation(text: String): Boolean = instance?.update(text) == true
        fun commitDictation(): Boolean = instance?.commit() == true
        fun cancelDictation() = instance?.cancel()
        fun isReady(): Boolean = instance != null
    }

    private var baseText = ""
    private var baseStart = -1
    private var baseEnd = -1
    private var active = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Keep the service alive and let Android update rootInActiveWindow.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun focusedEditable(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused?.isEditable == true) return focused
        return findEditable(root)
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditable(child)
            if (found != null) return found
        }
        return null
    }

    private fun begin(): Boolean {
        val node = focusedEditable() ?: return false
        baseText = node.text?.toString() ?: ""
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        baseStart = if (start >= 0) start else baseText.length
        baseEnd = if (end >= 0) end else baseStart
        active = true
        return true
    }

    private fun update(text: String): Boolean {
        if (!active && !begin()) return false
        val node = focusedEditable() ?: return false
        val a = baseStart.coerceIn(0, baseText.length)
        val b = baseEnd.coerceIn(a, baseText.length)
        val updated = baseText.substring(0, a) + text + baseText.substring(b)

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        if (ok) {
            val cursor = a + text.length
            node.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION,
                Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
                }
            )
        }
        return ok
    }

    private fun commit(): Boolean {
        val node = focusedEditable() ?: return false
        baseText = node.text?.toString() ?: baseText
        val cursor = node.textSelectionEnd.takeIf { it >= 0 } ?: baseText.length
        baseStart = cursor
        baseEnd = cursor
        return true
    }

    private fun cancel() {
        active = false
        baseText = ""
        baseStart = -1
        baseEnd = -1
    }
}
