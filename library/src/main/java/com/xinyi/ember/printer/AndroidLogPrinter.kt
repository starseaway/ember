package com.xinyi.ember.printer

import android.util.Log
import com.xinyi.ember.formatter.LogFormatter
import com.xinyi.ember.formatter.TextLogFormatter
import com.xinyi.ember.model.LogLevel
import com.xinyi.ember.model.LogRecord

/**
 * Android Logcat 输出器
 *
 * 将日志输出到系统 Logcat，并在内容过长时自动分块，避免单条日志被截断。
 *
 * @param formatter Logcat 日志格式化器，决定最终输出到 Logcat 的正文内容
 * @param maxChunkSize 单段日志最大长度，超过该长度会拆分为多条日志输出
 *
 * @author 新一
 * @date 2026/6/25 16:36
 */
class AndroidLogPrinter @JvmOverloads constructor(
    private val formatter: LogFormatter = TextLogFormatter(),
    private val maxChunkSize: Int = DEFAULT_CHUNK_SIZE
) : LogPrinter {

    private companion object {

        /**
         * 默认单段日志最大长度
         */
        private const val DEFAULT_CHUNK_SIZE = 3200
    }

    /**
     * 输出日志记录
     *
     * @param record 标准化日志记录
     */
    override fun print(record: LogRecord) {
        val message = formatter.format(record)
        if (message.length <= maxChunkSize) {
            printMessage(record, message)
            return
        }

        // 分段输出日志
        var start = 0
        while (start < message.length) {
            val end = (start + maxChunkSize).coerceAtMost(message.length)
            printMessage(record, message.substring(start, end))
            start = end
        }
    }

    /**
     * 输出单段日志
     *
     * @param record 标准化日志记录，用于获取日志等级、tag 和异常信息
     * @param message 已经格式化并完成分块的日志内容
     */
    private fun printMessage(record: LogRecord, message: String) {
        when (record.level) {
            LogLevel.VERBOSE -> Log.v(record.tag, message)
            LogLevel.DEBUG -> Log.d(record.tag, message)
            LogLevel.INFO -> Log.i(record.tag, message)
            LogLevel.WARN -> Log.w(record.tag, message)
            LogLevel.ERROR -> {
                if (record.throwable == null) {
                    Log.e(record.tag, message)
                } else {
                    Log.e(record.tag, message, record.throwable)
                }
            }

            LogLevel.ASSERT -> {
                if (record.throwable == null) {
                    Log.wtf(record.tag, message)
                } else {
                    Log.wtf(record.tag, message, record.throwable)
                }
            }
        }
    }
}