package com.xinyi.ember.dispatcher

import com.xinyi.ember.collector.CollectorConfig
import com.xinyi.ember.config.LogConfig
import com.xinyi.ember.formatter.JsonLogFormatter
import com.xinyi.ember.model.LogLevel
import com.xinyi.ember.model.LogRecord
import com.xinyi.ember.printer.LogPrinter
import com.xinyi.ember.stacktrace.StackTraceResolver

/**
 * 日志调度器
 *
 * 负责根据配置过滤日志，并把标准化后的日志记录分发给所有输出器。
 *
 * 该类不直接关心日志最终写到哪里，只负责统一生成 [LogRecord] 并分发，
 * 从而可以让各种日志输出目标，共享同一套日志上下文。
 *
 * @property config 日志框架运行配置
 *
 * @author 新一
 * @date 2026/6/25 17:01
 */
internal class LogDispatcher(private val config: LogConfig) {

    private companion object {

        /**
         * 当日志内容为 null 时，输出默认空对象提示
         */
        private const val NULL_MESSAGE = "Log with null Object"
    }

    /**
     * 输出普通日志
     *
     * @param level 日志等级
     * @param tag 本次日志标签，传入 null 时优先使用调用位置文件名，解析失败时使用配置中的默认标签
     * @param message 日志内容，传入 null 时会输出 `Log with null Object`
     * @param throwable 异常信息，主要用于 Error 和 Assert 级别输出堆栈
     * @param collectorName 收集器名称，普通全局日志传入 null
     */
    fun log(level: LogLevel, tag: String?, message: Any?, throwable: Throwable? = null, collectorName: String? = null) {
        if (!isLoggable(level, collectorName)) {
            return
        }
        dispatch(
            level = level,
            tag = tag,
            message = message?.toString() ?: NULL_MESSAGE,
            throwable = throwable,
            collectorName = collectorName
        )
    }

    /**
     * 输出 JSON 日志
     *
     * @param tag 本次日志标签，传入 null 时优先使用调用位置文件名，解析失败时使用配置中的默认标签
     * @param json JSON 字符串
     * @param isFormatJson 是否展开格式化 JSON；为 true 时每层嵌套缩进 [JsonLogFormatter.JSON_PRETTY_INDENT] 个空格
     * @param collectorName 收集器名称，普通全局日志传入 null
     */
    fun json(tag: String?, json: String?, isFormatJson: Boolean, collectorName: String? = null) {
        if (!isLoggable(LogLevel.DEBUG, collectorName)) {
            return
        }
        val indent = if (isFormatJson) {
            JsonLogFormatter.JSON_PRETTY_INDENT
        } else {
            0
        }
        dispatch(
            level = LogLevel.DEBUG,
            tag = tag,
            message = JsonLogFormatter.format(json.orEmpty(), indent),
            throwable = null,
            collectorName = collectorName
        )
    }

    /**
     * 是否允许输出当前等级日志
     *
     * @param level 当前日志等级
     * @param collectorName 收集器名称，普通全局日志传入 null
     * @return true 表示日志开关已开启、等级满足要求且至少存在一个输出器；输出器列表为空时返回 false，日志会被丢弃
     */
    private fun isLoggable(level: LogLevel, collectorName: String?): Boolean {
        if (!config.isEnabled) {
            return false
        }
        val collector = getCollectorConfig(collectorName)
        if (collector != null && !collector.isEnabled) {
            return false
        }
        val minLevel = collector?.minLevel ?: config.minLevel
        return level.isLoggable(minLevel) && getPrinters(collector).isNotEmpty()
    }

    /**
     * 分发日志到所有输出器
     *
     * @param level 日志等级
     * @param tag 本次日志标签
     * @param message 已经转换为字符串的日志内容
     * @param throwable 异常信息
     * @param collectorName 收集器名称，普通全局日志传入 null
     */
    private fun dispatch(level: LogLevel, tag: String?, message: String, throwable: Throwable?, collectorName: String?) {
        val caller = resolveCaller(tag)
        val collector = getCollectorConfig(collectorName)
        val record = LogRecord(
            level = level,
            tag = tag ?: caller?.fileName ?: config.defaultTag,
            message = message,
            throwable = throwable,
            timeMillis = System.currentTimeMillis(),
            threadName = Thread.currentThread().name,
            caller = if (config.isIncludeCaller) caller else null,
            productTypeTag = config.productTypeTag,
            collectorName = collectorName
        )

        getPrinters(collector).forEach { printer ->
            runCatching {
                printer.print(record)
            }
        }
    }

    /**
     * 获取收集器配置
     *
     * @param collectorName 收集器名称
     */
    private fun getCollectorConfig(collectorName: String?): CollectorConfig? {
        return if (collectorName == null) {
            null
        } else {
            config.collectors[collectorName]
        }
    }

    /**
     * 获取本次日志需要使用的输出器
     *
     * @param collector 收集器配置
     */
    private fun getPrinters(collector: CollectorConfig?): List<LogPrinter> {
        if (collector == null) {
            return config.printers
        }
        val printers = mutableListOf<LogPrinter>()
        if (collector.isInheritGlobalPrinters) {
            printers.addAll(config.printers)
        }
        printers.addAll(collector.printers)
        return printers
    }

    /**
     * 解析业务代码调用栈，供 Logcat tag 推导与正文出处共用
     * 
     * - 已传 tag 且不需要正文出处时可跳过解析
     * - 未传 tag 时始终解析，因为还要以文件名作为 Logcat tag
     *
     * @param tag 本次日志标签
     * @return 调用栈元素；跳过或失败时返回 null
     */
    private fun resolveCaller(tag: String?): StackTraceElement? {
        if (tag != null && !config.isIncludeCaller) {
            return null
        }
        return StackTraceResolver.resolve()
    }
}