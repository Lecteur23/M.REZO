package com.lecteur23.mrezo

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.Normalizer

private const val USSD_PREFS_NAME = "mrezo_security"
private const val USSD_KEY_STEPS = "ussd_steps"
private const val USSD_KEY_STEP_INDEX = "ussd_step_index"
private const val USSD_KEY_RUNNING = "ussd_running"
private const val USSD_KEY_RESULT_MESSAGE = "ussd_result_message"
private const val USSD_KEY_RESULT_STATUS = "ussd_result_status"

class MRezoUssdAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var submitting = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || submitting || !isSessionRunning()) return

        MRezoOverlayController.showFromAccessibilityService(this)

        val activeRoot = rootInActiveWindow ?: return
        val activePackageName = activeRoot.packageName?.toString().orEmpty().lowercase()
        val root = findUssdRoot(activeRoot, activePackageName) ?: return

        val visibleMessage = collectText(root)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val normalizedMessage = normalize(visibleMessage)
        val steps = currentSteps()
        val index = prefs().getInt(USSD_KEY_STEP_INDEX, 0)

        if (isNetworkRefusal(normalizedMessage)) {
            finishSession(visibleMessage, isError = true, root = root)
            return
        }

        if (index >= steps.size) {
            val finalMessage = visibleMessage.ifBlank { "Parcours USSD termine." }
            finishSession(finalMessage, isError = false, root = root)
            return
        }

        submitting = true
        handler.postDelayed({
            val freshRoot = rootInActiveWindow ?: root
            val freshMessage = collectText(freshRoot).joinToString(" ").replace(Regex("\\s+"), " ").trim()
            if (isNetworkRefusal(normalize(freshMessage))) {
                finishSession(freshMessage, isError = true, root = freshRoot)
                submitting = false
                return@postDelayed
            }

            val sent = submitStep(freshRoot, steps[index])
            if (sent) {
                prefs().edit().putInt(USSD_KEY_STEP_INDEX, index + 1).apply()
            }
            submitting = false
        }, 700L)
    }

    override fun onInterrupt() = Unit

    private fun prefs() = getSharedPreferences(USSD_PREFS_NAME, MODE_PRIVATE)

    private fun isSessionRunning(): Boolean = prefs().getBoolean(USSD_KEY_RUNNING, false)

    private fun currentSteps(): List<String> {
        return prefs().getString(USSD_KEY_STEPS, null)
            .orEmpty()
            .split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun submitStep(root: AccessibilityNodeInfo, step: String): Boolean {
        val input = findEditable(root) ?: return false

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, step)
        }
        val textSet = input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!textSet) return false

        handler.postDelayed({
            val freshRoot = findCurrentUssdRoot() ?: root
            clickButton(freshRoot, listOf("send", "envoyer", "repondre", "répondre", "reply", "ok"))
        }, 120L)
        handler.postDelayed({ hideKeyboardIfVisible() }, 320L)
        handler.postDelayed({ hideKeyboardIfVisible() }, 650L)
        return true
    }

    private fun hideKeyboardIfVisible() {
        if (isKeyboardVisible()) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun isKeyboardVisible(): Boolean {
        return windows.any { window ->
            val root = window.root ?: return@any false
            val packageName = root.packageName?.toString().orEmpty().lowercase()
            val className = root.className?.toString().orEmpty().lowercase()
            packageName.contains("inputmethod") ||
                packageName.contains("keyboard") ||
                packageName.contains("latin") ||
                packageName.contains("ime") ||
                className.contains("inputmethod")
        }
    }
    private fun finishSession(message: String, isError: Boolean, root: AccessibilityNodeInfo) {
        prefs().edit()
            .putBoolean(USSD_KEY_RUNNING, false)
            .putString(USSD_KEY_RESULT_MESSAGE, cleanResultMessage(message))
            .putString(USSD_KEY_RESULT_STATUS, if (isError) "error" else "success")
            .remove(USSD_KEY_STEPS)
            .remove(USSD_KEY_STEP_INDEX)
            .apply()

        val closeLabels = listOf("ok", "fermer", "close", "annuler", "cancel", "dismiss", "quitter", "terminer")
        clickButton(root, closeLabels)
        handler.postDelayed({
            clickButton(findCurrentUssdRoot() ?: root, closeLabels)
        }, 250L)
        handler.postDelayed({
            clickButton(findCurrentUssdRoot() ?: root, closeLabels)
        }, 700L)
        handler.postDelayed({
            if (findCurrentUssdRoot() != null) performGlobalAction(GLOBAL_ACTION_BACK)
        }, 1150L)
        handler.postDelayed({ bringMRezoToFront() }, 1450L)
        handler.postDelayed({ MRezoOverlayController.hide(this) }, 2300L)
    }
    private fun bringMRezoToFront() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(launchIntent)
    }

    private fun cleanResultMessage(message: String): String {
        val cleaned = message
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifBlank { "Operation terminee par le reseau." }
    }

    private fun isNetworkRefusal(message: String): Boolean {
        val keywords = listOf(
            "pas eligible",
            "non eligible",
            "n est pas eligible",
            "n' est pas eligible",
            "vous n etes pas eligible",
            "not eligible",
            "fond insuffisant",
            "fonds insuffisant",
            "solde insuffisant",
            "solde est insuffisant",
            "votre solde est insuffisant",
            "credit insuffisant",
            "credit insuffisamment",
            "pas suffisamment de credit",
            "pas suffisament de credit",
            "vous n avez pas suffisamment de credit",
            "vous n'avez pas suffisamment de credit",
            "vous n avez pas suffisament de credit",
            "vous n'avez pas suffisament de credit",
            "montant insuffisant",
            "balance insuffisante",
            "insuffisant",
            "insuffisance",
            "pas assez",
            "insufficient",
            "desole",
            "desoler",
            "sorry",
            "echec",
            "echoue",
            "operation echouee",
            "impossible",
            "refuse",
            "refusee",
            "transaction refusee",
            "operation refusee",
            "rejete",
            "rejetee",
            "indisponible",
            "non autorise",
            "vous n etes pas autorise",
            "requete invalide",
            "request failed",
            "not allowed"
        )
        return keywords.any { message.contains(it) }
    }
    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{Mn}+"), "").lowercase()
    }

    private fun findCurrentUssdRoot(): AccessibilityNodeInfo? {
        val activeRoot = rootInActiveWindow
        val activePackageName = activeRoot?.packageName?.toString().orEmpty().lowercase()
        if (activeRoot != null) {
            findUssdRoot(activeRoot, activePackageName)?.let { return it }
        }

        windows.forEach { window ->
            val root = window.root ?: return@forEach
            val packageName = root.packageName?.toString().orEmpty().lowercase()
            if (!packageName.contains("m_rezo") && isLikelyUssdWindow(root, packageName)) {
                return root
            }
        }
        return null
    }

    private fun findUssdRoot(activeRoot: AccessibilityNodeInfo, activePackageName: String): AccessibilityNodeInfo? {
        if (!activePackageName.contains("m_rezo") && isLikelyUssdWindow(activeRoot, activePackageName)) {
            return activeRoot
        }

        windows.forEach { window ->
            val root = window.root ?: return@forEach
            val packageName = root.packageName?.toString().orEmpty().lowercase()
            if (!packageName.contains("m_rezo") && isLikelyUssdWindow(root, packageName)) {
                return root
            }
        }
        return null
    }

    private fun isLikelyUssdWindow(root: AccessibilityNodeInfo, packageName: String): Boolean {
        if (packageName.contains("phone") || packageName.contains("telecom") || packageName.contains("systemui")) {
            return findEditable(root) != null || collectText(root).any { it.contains("ussd", ignoreCase = true) }
        }
        return collectText(root).any { text ->
            text.contains("ussd", ignoreCase = true) || text.contains("mtn", ignoreCase = true)
        }
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
            return node
        }
        for (index in 0 until node.childCount) {
            val found = findEditable(node.getChild(index))
            if (found != null) return found
        }
        return null
    }

    private fun clickButton(node: AccessibilityNodeInfo?, labels: List<String>): Boolean {
        if (node == null) return false
        val text = normalize(
            listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
                .joinToString(" ")
        )
        if (node.isEnabled && node.isClickable && labels.any { text == it || text.contains(it) }) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (index in 0 until node.childCount) {
            if (clickButton(node.getChild(index), labels)) return true
        }
        return false
    }

    private fun collectText(node: AccessibilityNodeInfo?): List<String> {
        if (node == null) return emptyList()
        val ownText = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .filter { it.isNotBlank() }
        val childText = (0 until node.childCount).flatMap { collectText(node.getChild(it)) }
        return ownText + childText
    }
}
