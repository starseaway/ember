package com.xinyi.ember.model

import android.util.Log

/**
 * 日志等级
 *
 * 等级顺序与 Android [Log] 保持一致，数值越大表示日志越重要。
 *
 * 该枚举既用于日志过滤，也用于输出器选择对应的 Android Log 方法。
 *
 * @property priority Android Log 对应的优先级
 * @property shortName 日志等级简写
 *
 * @author 新一
 * @date 2026/6/25 14:10
 */
enum class LogLevel(val priority: Int, val shortName: String) {

    /**
     * Verbose 级别，输出最详细的调试信息
     */
    VERBOSE(Log.VERBOSE, "V"),

    /**
     * Debug 级别，输出开发调试信息
     */
    DEBUG(Log.DEBUG, "D"),

    /**
     * Info 级别，输出提示性信息
     */
    INFO(Log.INFO, "I"),

    /**
     * Warn 级别，输出警告信息
     */
    WARN(Log.WARN, "W"),

    /**
     * Error 级别，输出错误信息
     */
    ERROR(Log.ERROR, "E"),

    /**
     * Assert 级别，表示正常情况下不应该发生的问题
     */
    ASSERT(Log.ASSERT, "A");

    /**
     * 当前等级是否允许输出
     *
     * @param minLevel 最低输出等级
     * @return true 表示当前等级不低于最低输出等级
     */
    fun isLoggable(minLevel: LogLevel): Boolean {
        return priority >= minLevel.priority
    }
}