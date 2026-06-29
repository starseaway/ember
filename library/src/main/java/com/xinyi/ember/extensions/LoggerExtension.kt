@file:JvmName("LoggerExtension")

package com.xinyi.ember.extensions

import com.xinyi.ember.BuildConfig
import com.xinyi.ember.logger.Logger

/**
 * 日志打印接口扩展函数
 *
 * @author 新一
 * @date 2026/6/26 14:59
 */

/**
 * Debug 构建变体下输出 Debug 日志
 *
 * @param tag 本次日志标签
 * @param message 日志内容
 */
fun Logger.debugOnly(tag: String? = null, message: String) {
    if (BuildConfig.DEBUG) {
        d(tag, message)
    }
}

/**
 * Debug 构建变体下输出 Debug 日志
 * 
 * @param tag 本次日志标签
 * @param message 日志内容
 */
inline fun Logger.debugOnly(tag: String? = null, message: () -> Any?) {
    if (BuildConfig.DEBUG) {
        d(tag, message())
    }
}

/**
 * 执行 [block] 并在 Debug 级别输出耗时
 *
 * @param tag 本次日志标签
 * @param label 耗时描述前缀，默认 "done"
 */
inline fun <T> Logger.timed(tag: String? = null, label: String = "done", block: () -> T): T {
    val start = System.currentTimeMillis()
    return try {
        block()
    } finally {
        d(tag, "$label in ${System.currentTimeMillis() - start}ms")
    }
}