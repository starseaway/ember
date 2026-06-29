package com.xinyi.ember.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期格式化工具（线程安全）
 * 
 * 主要用于日志文件日期目录名和时间格式化。
 * 
 * > [SimpleDateFormat] 非线程安全，通过 [ThreadLocal] 为每个线程维护独立实例
 *
 * @author 杨耿雷
 * @date 2026/6/26 13:41
 */
internal object DateFormats {

    /**
     * 日志文件日期目录名格式化器
     */
    private val logFileDate by lazy { createHolder("yyyy-MM-dd") }
    
    /**
     * 日志文件时间格式化器
     */
    private val logDateTime by lazy { createHolder("yyyy-MM-dd HH:mm:ss:SSS") }

    /**
     * 格式化为日志文件日期目录名，默认当天
     * 
     * @param date 日期
     */
    fun formatLogFileDate(date: Date = Date()): String {
        val formatter = requireNotNull(logFileDate.get())
        return formatter.format(date)
    }

    /**
     * 解析日志文件日期目录名
     * 
     * @param text 日志文件日期目录名
     */
    fun parseLogFileDate(text: String): Date? {
        return runCatching {
            val formatter = requireNotNull(logFileDate.get())
            formatter.parse(text)
        }.getOrNull()
    }

    /**
     * 格式化为日志文本时间
     */
    fun formatLogDateTime(timeMillis: Long): String {
        val formatter = requireNotNull(logDateTime.get())
        return formatter.format(Date(timeMillis))
    }

    /**
     * 创建线程本地格式化器
     *
     * @param pattern 日期格式
     */
    private fun createHolder(pattern: String): ThreadLocal<SimpleDateFormat> {
        return object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat(pattern, Locale.CHINA)
            }
        }
    }
}