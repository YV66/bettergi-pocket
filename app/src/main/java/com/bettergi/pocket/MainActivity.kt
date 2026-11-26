package com.bettergi.pocket

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle    //Bundle 本质是一个 键值对容器
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bettergi.pocket.analytics.BaiduAnalytics
import com.bettergi.pocket.service.TriggerForegroundService

class MainActivity : AppCompatActivity() {
    private var permissionUiShown = false
    private var launchReady = false

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    //onCreate 是 Activity 生命周期的第一个回调
    override fun onCreate(savedInstanceState: Bundle?) {
        //canDrawOverlays：是否拥有“在其他应用上层显示”（即悬浮窗）的权限
        if (!Settings.canDrawOverlays(this)) setTheme(R.style.Theme_BetterGIPocket_Overlay)
        super.onCreate(savedInstanceState)
        launchReady = true
        BaiduAnalytics.start()
        continueLaunch()
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing || !launchReady) return
        if (Settings.canDrawOverlays(this)) {
            launchOverlayAndExit()
        } else if (!permissionUiShown) {
            showOverlayPermissionUi()
        }
    }

    private fun continueLaunch() {
        if (Settings.canDrawOverlays(this)) {
            launchOverlayAndExit()
            return
        }
        showOverlayPermissionUi()
    }

    private fun showOverlayPermissionUi() {
        if (permissionUiShown) return
        permissionUiShown = true
        setContentView(R.layout.activity_main) // res/layout/activity_main.xml
        // 应用全屏时，处理系统栏 / 刘海屏遮挡
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        //找到 id 为 btn_request_overlay 的按钮
        //<Button> 是泛型参数，表示把它当作 Button 类型返回，省去强转
        findViewById<Button>(R.id.btn_request_overlay).setOnClickListener {
            startActivity( //把 Intent 交给系统，打开对应的设置页面
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION, //系统内置 action，表示“打开悬浮窗权限管理页面”
                    //packageName 是当前应用的包名（如 com.example.app），拼成 package:com.example.app。
                    //带上这个 Uri，系统会直接跳到当前应用的悬浮窗权限开关页，而不是让用户在列表里自己找。
                    Uri.parse("package:$packageName"), //指定包名
                ),
            )
        }
        requestNotificationPermissionIfNeeded()
    }



    // Intent = 目标 + 动作 + 数据
    private fun launchOverlayAndExit() {    //启动悬浮窗并退出
        if (isFinishing) return
        val intent = Intent(this, TriggerForegroundService::class.java).apply {
            action = TriggerForegroundService.ACTION_START //启动 TriggerForegroundService，并告诉它“执行 START 动作”
        }
        ContextCompat.startForegroundService(this, intent) //启动一个前台服务
        finish() //关闭当前 Activity
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED   //发送通知的权限
        ) {
            return
        }
        requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
