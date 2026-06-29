package com.xinyi.app.ember

import android.app.Application
import android.util.Log
import com.xinyi.ember.Ember
import com.xinyi.ember.extensions.addFilePrinter
import java.io.File

class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = Ember.builder()
            .addFilePrinter(File(filesDir, "logs").absolutePath)
            .build()
        Ember.init(config)

        // 运行演示日志
        Ember.v("Verbose：详细调试信息")
        Ember.d("Debug：应用启动检查通过")
        Ember.i("Info：用户已进入首页")
        Ember.w("Warn：缓存即将过期")
        Ember.e("Error：接口返回业务错误码 500")

        val json = """{"name":"ember","version":1,"features":["logcat","file","collector"]}"""
        Ember.json(json, isFormatJson = true)

        try {
            throw RuntimeException("模拟网络超时")
        } catch (exception: Exception) {
            Ember.e(Log.getStackTraceString(exception))
        }
    }
}