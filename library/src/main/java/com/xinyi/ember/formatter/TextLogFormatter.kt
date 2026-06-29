package com.xinyi.ember.formatter

import com.xinyi.ember.model.LogRecord
import com.xinyi.ember.util.DateFormats

/**
 * 文本日志格式化器
 *
 * 主要负责将 [LogRecord] 格式化为纯文本。
 *
 * @author 新一
 * @date 2026/6/25 17:18
 */
class TextLogFormatter @JvmOverloads constructor(
    /** 是否输出日志产生时间；开启后会追加 `yyyy-MM-dd HH:mm:ss:SSS` */
    private val includeTime: Boolean = false,
    /** 是否输出线程名称；开启后会追加 `threadName` */
    private val includeThread: Boolean = false,
    /** 是否输出收集器名称；开启后会追加 `collectorName` */
    private val includeCollector: Boolean = true,
    /** 是否输出调用位置；开启后会追加出处 */
    private val includeCaller: Boolean = true
) : LogFormatter {

    companion object {
        /** 单行输出调用位置后缀 */
        private const val CALLER_INLINE = " ⇨ "
        /** 多行输出调用位置后缀 */
        private const val CALLER_MULTILINE = " ⇩\n"
    }

    /**
     * 格式化日志记录
     *
     * @param record 标准化日志记录
     * @return 拼接后的日志文本
     */
    override fun format(record: LogRecord): String {
        val builder = StringBuilder()

        if (record.productTypeTag != null) {
            builder.append("《").append(record.productTypeTag).append("》")
        }
        if (includeTime) {
            builder.append(formatTime(record.timeMillis)).append(" ")
        }
        if (includeThread) {
            builder.append("TID:[").append(record.threadName).append("] ")
        }
        if (includeCollector && record.collectorName != null) {
            builder.append("CID:[").append(record.collectorName).append("] ")
        }
        if (includeCaller && record.caller != null) {
            // 如果消息包含换行符，则换行输出
            val suffix = if (record.message.contains('\n')) {
                CALLER_MULTILINE
            } else {
                CALLER_INLINE
            }
            builder.append(formatCaller(record.caller)).append(suffix)
        }
        builder.append(record.message)
        return builder.toString()
    }

    /**
     * 格式化时间
     *
     * @param timeMillis 日志产生时间戳，单位毫秒
     */
    private fun formatTime(timeMillis: Long): String {
        return DateFormats.formatLogDateTime(timeMillis)
    }

    /**
     * 格式化调用位置
     *
     * @param caller 调用日志 API 的堆栈元素
     * @return 文件名、行号和方法名组成的调用位置
     */
    private fun formatCaller(caller: StackTraceElement): String {
        return "(${caller.fileName}:${caller.lineNumber})#${caller.methodName}"
    }
}