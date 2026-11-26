package com.bettergi.pocket.genshin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class GenshinLauncher(private val context: Context) {
    fun resolveInstalledPackage(): String? {
        return GenshinPackages.pickPreferred(findInstalledPackages())
    }

    fun launch(): GenshinLaunchResult {
        val packageName = resolveInstalledPackage() ?: return GenshinLaunchResult.NotInstalled
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return GenshinLaunchResult.Failed(packageName)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
        )
        return try {
            context.startActivity(intent)
            GenshinLaunchResult.Started(packageName)
        } catch (_: Throwable) {
            GenshinLaunchResult.Failed(packageName)
        }
    }

    private fun findInstalledPackages(): List<String> {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(launchIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launchIntent, 0)
            }
        } catch (_: Throwable) {
            emptyList()
        }
        val fromLauncher = resolved.mapNotNull { it.activityInfo?.packageName }
            .filter { GenshinPackages.isGenshinPackage(it) }
        if (fromLauncher.isNotEmpty()) {
            return fromLauncher.distinct()
        }
        return GenshinPackages.CANDIDATES.filter { isPackageInstalled(pm, it) }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}

sealed class GenshinLaunchResult {//密封类
    //data class：数据类，自动生成 equals、hashCode、toString、copy 等
    data class Started(val packageName: String) : GenshinLaunchResult() // 携带一个 packageName: String：启动成功时，记录启动的是哪个包名（原神可能有国服、国际服等不同包名）
    data object NotInstalled : GenshinLaunchResult()
    data class Failed(val packageName: String?) : GenshinLaunchResult()
}
