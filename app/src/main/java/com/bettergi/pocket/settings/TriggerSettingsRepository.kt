package com.bettergi.pocket.settings

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

class TriggerSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val listeners = CopyOnWriteArrayList<(TriggerSettings) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper()) //任务投到主线程的消息队列 //mainHandler 可以有多个，但都指向同一个 Looper

    @Volatile
    private var current: TriggerSettings = readFromPrefs()

    fun get(): TriggerSettings = current

    fun addListener(listener: (TriggerSettings) -> Unit) {
        listeners.add(listener) //监听器操作实现是在listener这个函数参数
        mainHandler.post { listener(current) } // 立即推当前值
    }

    fun removeListener(listener: (TriggerSettings) -> Unit) {
        listeners.remove(listener)
    }

    fun setScreenShareEnabled(enabled: Boolean) {
        update { it.copy(screenShareEnabled = enabled) }//最后一个参数是函数类型，可以把它写到圆括号外面 //copy 是 data class 自动生成的，参数对应所有字段，作用是复制并改这个字段
    }

    fun setAutoPickEnabled(enabled: Boolean) {
        update { it.copy(autoPickEnabled = enabled) }
    }

    fun setAutoSkipEnabled(enabled: Boolean) {
        update { it.copy(autoSkipEnabled = enabled) }
    }

    fun setQuickSkipDialogueEnabled(enabled: Boolean) {
        update { it.copy(quickSkipDialogueEnabled = enabled) }
    }

    fun setAutoLaunchGenshinEnabled(enabled: Boolean) {
        update { it.copy(autoLaunchGenshinEnabled = enabled) }
    }

    //transform 是参数名。它的类型是 (TriggerSettings) -> TriggerSettings，也就是一个函数
    private fun update(transform: (TriggerSettings) -> TriggerSettings) {
        val newValue: TriggerSettings
        synchronized(lock) {
            val old = current
            val updated = transform(old)
            if (updated == old) return
            current = updated
            newValue = updated
            prefs.edit() //prefs 是磁盘持久化，下次启动时读回来
                .putBoolean(KEY_SCREEN_SHARE, updated.screenShareEnabled)
                .putBoolean(KEY_AUTO_PICK, updated.autoPickEnabled)
                .putBoolean(KEY_AUTO_SKIP, updated.autoSkipEnabled)
                .putBoolean(KEY_QUICK_SKIP, updated.quickSkipDialogueEnabled)
                .putBoolean(KEY_AUTO_LAUNCH_GENSHIN, updated.autoLaunchGenshinEnabled)
                .apply() //.apply() 是异步提交
        }
        listeners.forEach { listener ->
            mainHandler.post { listener(newValue) } // 向主线程投递的是listener监听操作，newValue是操作需要的数据 //listener在函数addListener中注册
        }
    }

    private fun readFromPrefs(): TriggerSettings = TriggerSettings(
        screenShareEnabled = false,
        autoPickEnabled = prefs.getBoolean(KEY_AUTO_PICK, false),
        autoSkipEnabled = prefs.getBoolean(KEY_AUTO_SKIP, false),
        quickSkipDialogueEnabled = prefs.getBoolean(KEY_QUICK_SKIP, true),
        autoLaunchGenshinEnabled = prefs.getBoolean(KEY_AUTO_LAUNCH_GENSHIN, false),
    )

    private companion object {
        const val PREFS_NAME = "trigger_settings"
        const val KEY_SCREEN_SHARE = "screenShareEnabled"
        const val KEY_AUTO_PICK = "autoPickEnabled"
        const val KEY_AUTO_SKIP = "autoSkipEnabled"
        const val KEY_QUICK_SKIP = "quickSkipDialogueEnabled"
        const val KEY_AUTO_LAUNCH_GENSHIN = "autoLaunchGenshinEnabled"
    }
}
