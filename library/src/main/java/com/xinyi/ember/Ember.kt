package com.xinyi.ember

import com.xinyi.ember.collector.CollectorConfig
import com.xinyi.ember.collector.LogCollector
import com.xinyi.ember.collector.ScopedLogCollector
import com.xinyi.ember.config.LogConfig
import com.xinyi.ember.dispatcher.LogDispatcher
import com.xinyi.ember.model.LogLevel
import com.xinyi.ember.printer.FileLogPrinter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ember 日志框架入口
 *
 * - 使用 [init] 初始化日志配置
 * - 使用 [v]、[d]、[i]、[w]、[e]、[wtf] 输出全局日志
 * - 使用 [collector] 创建日志收集器
 * - 使用 [json] 输出 JSON 内容
 * - 经本类注册的 [FileLogPrinter] 会在配置替换与进程退出时自动 [shutdown]
 *
 * 无参的 [v]、[d]、[i]、[w]、[e]、[wtf] 在未传内容时，正文为 `Log with null Object`。
 *
 * @author 新一
 * @date 2026/6/25 14:21
 */
object Ember {

    /**
     * 当前正在使用的日志配置
     */
    @Volatile
    private var config: LogConfig = LogConfig.defaultConfig()

    /**
     * 当前正在使用的日志调度器
     */
    @Volatile
    private var dispatcher: LogDispatcher = LogDispatcher(config)

    /**
     * 当前正在使用的收集器集合
     */
    private val collectors = ConcurrentHashMap<String, LogCollector>()

    /**
     * 是否已注册进程退出时的文件日志收尾钩子
     */
    private val shutdownHookRegistered = AtomicBoolean(false)

    /**
     * 创建日志配置构建器
     *
     * @return 默认的日志配置构建器
     */
    @JvmStatic
    fun builder(): LogConfig.Builder {
        return LogConfig.Builder()
    }

    /**
     * 初始化日志框架
     *
     * 参数会替换当前所有配置和输出器
     *
     * @param config 完整日志配置
     */
    @JvmStatic
    fun init(config: LogConfig) {
        replaceConfig(config)
    }

    /**
     * 停止当前配置中所有 [FileLogPrinter] 的后台线程并关闭文件流
     * 
     * 经 [init] 注册的文件日志会在配置替换与进程正常退出时自动收尾，一般无需手动调用。
     */
    @JvmStatic
    @Synchronized
    fun shutdown() {
        shutdownFilePrinters(collectFileLogPrinters(config))
    }

    /**
     * 设置是否启用日志
     * 
     * 当 [isEnabled] 为 false 时，日志会在调度层直接返回，不会解析调用栈，也不会触发任何输出器。
     *
     * @param isEnabled 是否启用日志
     */
    @JvmStatic
    fun setEnabled(isEnabled: Boolean) {
        updateConfig {
            setEnabled(isEnabled)
        }   
    }

    /**
     * 获取当前配置
     *
     * @return 当前正在使用的日志配置
     */
    @JvmStatic
    fun getConfig(): LogConfig {
        return config
    }

    /**
     * 设置默认日志标签
     *
     * 当没有显式传入 tag，且调用栈解析失败时，才会使用该值作为兜底 tag。
     *
     * @param tag 兜底日志标签
     */
    @JvmStatic
    fun setDefaultTag(tag: String) {
        updateConfig {
            setDefaultTag(tag)
        }
    }

    /**
     * 设置最低输出等级
     *
     * 设置后，低于 [level] 的日志会被直接丢弃。
     *
     * @param level 最低允许输出的日志等级
     */
    @JvmStatic
    fun setMinLevel(level: LogLevel) {
        updateConfig {
            setMinLevel(level)
        }
    }

    /**
     * 设置产品类型标签
     *
     * 该标签会被拼接在每条日志消息的前方，用于区分不同应用变体输出的日志。
     *
     * @param tag 产品类型标签，传入 null 表示清除
     */
    @JvmStatic
    fun setProductTypeTag(tag: String?) {
        updateConfig {
            setProductTypeTag(tag)
        }
    }

    /**
     * 设置是否在日志正文前拼接调用出处
     *
     * @param includeCaller true 表示拼接出处，false 表示不拼接
     */
    @JvmStatic
    fun setIncludeCaller(includeCaller: Boolean) {
        updateConfig {
            setIncludeCaller(includeCaller)
        }
    }

    /**
     * 获取产品类型标签
     *
     * @return 当前产品类型标签，未设置时返回 null
     */
    @JvmStatic
    fun getProductTypeTag(): String? {
        return config.productTypeTag
    }

    /**
     * 创建或获取日志收集器
     *
     * @param name 收集器名称
     */
    @JvmStatic
    fun collector(name: String): LogCollector {
        require(name.isNotBlank()) { "Collector name must not be blank" }
        return collectors.getOrPut(name) {
            ScopedLogCollector(name) {
                dispatcher
            }
        }
    }

    /**
     * 创建日志收集器配置构建器
     *
     * @param name 收集器名称
     */
    @JvmStatic
    fun collectorBuilder(name: String): CollectorConfig.Builder {
        return CollectorConfig.Builder(name)
    }

    /**
     * 注册日志收集器配置
     *
     * @param collectorConfig 收集器配置
     */
    @JvmStatic
    fun registerCollector(collectorConfig: CollectorConfig) {
        updateConfig {
            addCollector(collectorConfig)
        }
    }

    /**
     * 移除日志收集器配置
     *
     * 移除配置不会销毁已经创建的 [LogCollector]，只会让同名收集器回到默认输出策略。
     *
     * @param name 收集器名称
     */
    @JvmStatic
    fun removeCollector(name: String) {
        updateConfig {
            removeCollector(name)
        }
    }

    /**
     * 输出 Verbose 级别日志
     */
    @JvmStatic
    fun v() {
        printLog(LogLevel.VERBOSE, null, null)
    }

    /**
     * 输出 Verbose 级别日志
     *
     * @param msg 日志内容
     */
    @JvmStatic
    fun v(msg: Any?) {
        printLog(LogLevel.VERBOSE, null, msg)
    }

    /**
     * 输出 Verbose 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    @JvmStatic
    fun v(tag: String?, msg: Any?) {
        printLog(LogLevel.VERBOSE, tag, msg)
    }

    /**
     * 输出 Debug 级别日志
     */
    @JvmStatic
    fun d() {
        printLog(LogLevel.DEBUG, null, null)
    }

    /**
     * 输出 Debug 级别日志
     *
     * @param msg 日志内容
     */
    @JvmStatic
    fun d(msg: Any?) {
        printLog(LogLevel.DEBUG, null, msg)
    }

    /**
     * 输出 Debug 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    @JvmStatic
    fun d(tag: String?, msg: Any?) {
        printLog(LogLevel.DEBUG, tag, msg)
    }

    /**
     * 输出 Info 级别日志
     */
    @JvmStatic
    fun i() {
        printLog(LogLevel.INFO, null, null)
    }

    /**
     * 输出 Info 级别日志
     *
     * @param msg 日志内容
     */
    @JvmStatic
    fun i(msg: Any?) {
        printLog(LogLevel.INFO, null, msg)
    }

    /**
     * 输出 Info 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    @JvmStatic
    fun i(tag: String?, msg: Any?) {
        printLog(LogLevel.INFO, tag, msg)
    }

    /**
     * 输出 Warning 级别日志
     *
     * 不传内容时会输出 `Log with null Object`。
     */
    @JvmStatic
    fun w() {
        printLog(LogLevel.WARN, null, null)
    }

    /**
     * 输出 Warning 级别日志
     *
     * @param msg 日志内容
     */
    @JvmStatic
    fun w(msg: Any?) {
        printLog(LogLevel.WARN, null, msg)
    }

    /**
     * 输出 Warning 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    @JvmStatic
    fun w(tag: String?, msg: Any?) {
        printLog(LogLevel.WARN, tag, msg)
    }

    /**
     * 输出 Error 级别日志
     */
    @JvmStatic
    fun e() {
        printLog(LogLevel.ERROR, null, null)
    }

    /**
     * 输出 Error 级别日志
     *
     * @param msg 日志内容
     */
    @JvmStatic
    fun e(msg: Any?) {
        printLog(LogLevel.ERROR, null, msg)
    }

    /**
     * 输出 Error 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    @JvmStatic
    fun e(tag: String?, msg: Any?) {
        printLog(LogLevel.ERROR, tag, msg)
    }

    /**
     * 输出带异常堆栈的 Error 级别日志
     *
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    @JvmStatic
    fun e(msg: Any?, throwable: Throwable?) {
        printLog(LogLevel.ERROR, null, msg, throwable)
    }

    /**
     * 输出带异常堆栈的 Error 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    @JvmStatic
    fun e(tag: String?, msg: Any?, throwable: Throwable?) {
        printLog(LogLevel.ERROR, tag, msg, throwable)
    }

    /**
     * 输出 Assert 级别日志
     */
    @JvmStatic
    fun wtf() {
        printLog(LogLevel.ASSERT, null, null)
    }

    /**
     * 输出 Assert 级别日志
     *
     * @param msg 日志内容
     */
    @JvmStatic
    fun wtf(msg: Any?) {
        printLog(LogLevel.ASSERT, null, msg)
    }

    /**
     * 输出 Assert 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    @JvmStatic
    fun wtf(tag: String?, msg: Any?) {
        printLog(LogLevel.ASSERT, tag, msg)
    }

    /**
     * 输出带异常堆栈的 Assert 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    @JvmStatic
    fun wtf(tag: String?, msg: Any?, throwable: Throwable?) {
        printLog(LogLevel.ASSERT, tag, msg, throwable)
    }

    /**
     * 输出 JSON 日志
     *
     * 默认使用配置中的默认 tag，且不展开格式化 JSON。
     *
     * @param jsonFormat JSON 字符串
     */
    @JvmStatic
    fun json(jsonFormat: String?) {
        dispatcher.json(null, jsonFormat, false)
    }

    /**
     * 输出 JSON 日志
     *
     * @param jsonFormat JSON 字符串
     * @param isFormatJson 是否展开格式化 JSON
     */
    @JvmStatic
    fun json(jsonFormat: String?, isFormatJson: Boolean) {
        dispatcher.json(null, jsonFormat, isFormatJson)
    }

    /**
     * 输出 JSON 日志
     *
     * @param tag 本次日志标签
     * @param jsonFormat JSON 字符串
     */
    @JvmStatic
    fun json(tag: String?, jsonFormat: String?) {
        dispatcher.json(tag, jsonFormat, false)
    }

    /**
     * 输出 JSON 日志
     *
     * @param tag 本次日志标签
     * @param jsonFormat JSON 字符串
     * @param isFormatJson 是否展开格式化 JSON
     */
    @JvmStatic
    fun json(tag: String?, jsonFormat: String?, isFormatJson: Boolean) {
        dispatcher.json(tag, jsonFormat, isFormatJson)
    }

    /**
     * 打印日志
     *
     * @param level 日志等级
     * @param tag 本次日志标签
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    private fun printLog(level: LogLevel, tag: String?, msg: Any?, throwable: Throwable? = null) {
        dispatcher.log(level, tag, msg, throwable)
    }

    /**
     * 更新配置
     *
     * @param block 基于当前配置生成新配置的修改逻辑
     */
    @Synchronized
    private fun updateConfig(block: LogConfig.Builder.() -> Unit) {
        val builder = LogConfig.Builder(config)
        builder.block()
        replaceConfig(builder.build())
    }

    /**
     * 替换配置
     *
     * @param newConfig 新的日志配置
     */
    @Synchronized
    private fun replaceConfig(newConfig: LogConfig) {
        shutdownRemovedFilePrinters(config, newConfig)
        config = newConfig
        dispatcher = LogDispatcher(newConfig)
        registerProcessShutdownHookIfNeeded()
    }

    /**
     * 收集配置中所有的文件日志输出器
     * 
     * @param config 日志配置
     */
    private fun collectFileLogPrinters(config: LogConfig): List<FileLogPrinter> {
        val printers = mutableListOf<FileLogPrinter>()
        config.printers.filterIsInstanceTo(printers, FileLogPrinter::class.java)
        config.collectors.values.forEach { collectorConfig ->
            collectorConfig.printers.filterIsInstanceTo(printers, FileLogPrinter::class.java)
        }
        return printers
    }

    /**
     * 关闭已从配置中移除的文件日志输出器
     * 
     * @param oldConfig 旧配置
     * @param newConfig 新配置
     */
    private fun shutdownRemovedFilePrinters(oldConfig: LogConfig, newConfig: LogConfig) {
        val retained = collectFileLogPrinters(newConfig).toHashSet()
        collectFileLogPrinters(oldConfig)
            .filter { it !in retained }
            .forEach { it.shutdown() }
    }

    /**
     * 关闭指定的文件日志输出器
     * 
     * @param printers 文件日志输出器列表
     */
    private fun shutdownFilePrinters(printers: List<FileLogPrinter>) {
        printers.forEach { it.shutdown() }
    }

    /**
     * 在进程正常退出时收尾当前配置中的文件日志
     */
    private fun registerProcessShutdownHookIfNeeded() {
        if (collectFileLogPrinters(config).isEmpty()) {
            return
        }
        if (!shutdownHookRegistered.compareAndSet(false, true)) {
            return
        }
        // 注册进程退出时的文件日志收尾钩子
        Runtime.getRuntime().addShutdownHook(
            Thread(
                {
                    synchronized(Ember) {
                        shutdownFilePrinters(collectFileLogPrinters(config))
                    }
                },
                "Ember-FileLog-Shutdown"
            )
        )
    }
}