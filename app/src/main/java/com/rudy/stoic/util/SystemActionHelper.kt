package com.rudy.stoic.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings

object SystemActionHelper {

    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openDialer(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openMessages(context: Context) {
        // Try the default SMS app
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open SMS with Sms intent
            val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "vnd.android-dir/mms-sms"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(smsIntent)
            } catch (_: Exception) { }
        }
    }

    @SuppressLint("WrongConstant")
    fun expandNotificationPanel(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = statusBarService.javaClass
            val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (_: Exception) {
            // Fallback: some OEMs block this, silently fail
        }
    }

    @SuppressLint("WrongConstant")
    fun expandQuickSettings(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = statusBarService.javaClass
            val expandMethod = statusBarManager.getMethod("expandSettingsPanel")
            expandMethod.invoke(statusBarService)
        } catch (_: Exception) {
            // Try alternative method signature
            try {
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManager = statusBarService.javaClass
                val expandMethod = statusBarManager.getMethod("expandSettingsPanel", String::class.java)
                expandMethod.invoke(statusBarService, null as String?)
            } catch (_: Exception) { }
        }
    }
}
