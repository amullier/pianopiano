package fr.antmu.pianopiano.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object PackageHelper {

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getLaunchIntent(context: Context, packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val intent = getLaunchIntent(context, packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
