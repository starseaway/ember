package com.xinyi.ember.printer

import android.util.Log
import com.xinyi.ember.file.FileLogger
import com.xinyi.ember.formatter.LogFormatter
import com.xinyi.ember.formatter.TextLogFormatter
import com.xinyi.ember.model.LogRecord
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 日志文件输出器
 *
 * 日志写入操作只入队不阻塞，内部线程会异步执行日志信息格式化和落盘。
 *
 * 经 [com.xinyi.ember.Ember] 注册时，会在配置替换与进程正常退出时自动 [shutdown]；
 * 脱离框架独立使用时，不再使用后请务必调用 [shutdown]。
 *
 * > 本类可安全用于多线程环境，也可以脱离框架独立使用；
 * > 如果需要更细粒度的控制，可以考虑直接使用 [FileLogger]。
 *
 * @param logDirPath 日志根目录
 * @param maxFileSize 单文件大小上限（字节）；推荐使用 [FileLogger.DEFAULT_MAX_FILE_SIZE]
 * @param formatter 文件日志格式化器
 * @param fileLogger 文件写入器，默认根据目录和大小自动创建
 * @param queueCapacity 待写队列容量，满时丢弃新日志
 * @param retainMonthCount 自动清理时保留最近几个月日志；为 0 时不自动清理。推荐 [DEFAULT_RETAIN_MONTHS]
 * @param clearScanIntervalMillis 自动清理扫描间隔（毫秒）；为 0 时不扫描。推荐 [DEFAULT_SCAN_INTERVAL_MS]
 *
 * @author 新一
 * @date 2026/6/26 11:21
 */
class FileLogPrinter @JvmOverloads constructor(
    logDirPath: String,
    maxFileSize: Long,
    private val formatter: LogFormatter = TextLogFormatter(
        includeTime = true,
        includeThread = true,
        includeCaller = true
    ),
    val fileLogger: FileLogger = FileLogger(logDirPath, maxFileSize),
    private val queueCapacity: Int = DEFAULT_QUEUE_CAPACITY,
    private val retainMonthCount: Int = 0,
    private val clearScanIntervalMillis: Long = 0L
) : LogPrinter {

    companion object {
        /** 默认待写队列容量 */
        const val DEFAULT_QUEUE_CAPACITY = 1024

        /** 推荐保留最近 3 个月日志 */
        const val DEFAULT_RETAIN_MONTHS = 3

        /** 推荐每 24 小时扫描一次过期日志目录（毫秒） */
        const val DEFAULT_SCAN_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }

    /**
     * 是否头部换行
     */
    var isHeadLine: Boolean
        get() = fileLogger.isHeadLine
        set(value) {
            fileLogger.isHeadLine = value
        }

    /**
     * 是否尾部换行
     */
    var isEndLine: Boolean
        get() = fileLogger.isEndLine
        set(value) {
            fileLogger.isEndLine = value
        }

    /**
     * 待写队列
     */
    private val queue = LinkedBlockingQueue<PendingLog>(queueCapacity)

    /**
     * 是否已关闭
     */
    private val isShutdown = AtomicBoolean(false)

    /**
     * 后台写入线程池
     */
    private val worker = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        FileLogWorkerThreadFactory()
    )

    /**
     * 过期日志扫描调度器；[retainMonthCount] 与 [clearScanIntervalMillis] 均大于 0 时启用
     */
    private val clearScheduler: ScheduledExecutorService? =
        if (retainMonthCount > 0 && clearScanIntervalMillis > 0) {
            Executors.newSingleThreadScheduledExecutor(ClearScanThreadFactory())
        } else {
            null
        }

    init {
        // 启动后台写入线程
        worker.execute { drainLoop() }
        // 启动过期日志扫描调度器
        clearScheduler?.scheduleWithFixedDelay(
            { clearLogFiles(retainMonthCount) },
            clearScanIntervalMillis,
            clearScanIntervalMillis,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * 输出日志记录
     *
     * @param record 标准化日志记录
     */
    override fun print(record: LogRecord) {
        enqueue(PendingLog.Record(record))
    }

    /**
     * 将内容写入文件
     *
     * @param content 要记录的内容
     */
    fun writeLog(content: String) {
        enqueue(PendingLog.Raw(content))
    }

    /**
     * 清除几个月前的日志目录
     *
     * @param monthCount 保留最近几个月日志
     */
    fun clearLogFiles(monthCount: Int) {
        if (isShutdown.get()) {
            return
        }
        enqueue(PendingLog.Clear(monthCount))
    }

    /**
     * 停止后台写入并关闭文件流
     *
     * 会尽量写完队列中已有的日志。
     */
    fun shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            return
        }
        clearScheduler?.shutdownNow()
        enqueue(PendingLog.Shutdown)
        worker.shutdown()
        runCatching {
            // 等待后台写入线程结束，最多等待 3 秒
            if (!worker.awaitTermination(3, TimeUnit.SECONDS)) {
                worker.shutdownNow()
            }
        }
    }

    /**
     * 获取当前日志文件路径
     *
     * @return 当前正在写入的日志文件绝对路径；文件不可用时返回空字符串
     */
    fun getLogFilePath(): String {
        return fileLogger.getLogFilePath()
    }

    /**
     * 将日志项入队
     *
     * @param item 日志项
     */
    private fun enqueue(item: PendingLog) {
        if (isShutdown.get()) {
            return
        }

        if (!queue.offer(item)) {
            Log.e("FileLogPrinter", "文件日志队列已满，丢弃了一条日志")
        }
    }

    /**
     * 循环读取队列中的日志项并写入文件
     */
    private fun drainLoop() {
        while (true) {
            val item = runCatching { queue.take() }.getOrNull() ?: break
            when (item) {
                is PendingLog.Shutdown -> break
                is PendingLog.Record -> fileLogger.write(formatter.format(item.record))
                is PendingLog.Raw -> fileLogger.write(item.content)
                is PendingLog.Clear -> fileLogger.clearLogFiles(item.monthCount)
            }
        }
        fileLogger.close()
    }

    /**
     * 日志项
     */
    private sealed class PendingLog {

        data class Record(val record: LogRecord) : PendingLog()

        data class Raw(val content: String) : PendingLog()

        /**
         * @param monthCount 保留最近几个月日志
         */
        data class Clear(val monthCount: Int) : PendingLog()

        object Shutdown : PendingLog()
    }

    /**
     * 文件写入线程工厂
     */
    private class FileLogWorkerThreadFactory : ThreadFactory {

        private val threadIndex = AtomicInteger(0)

        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, "Ember-FileLog-${threadIndex.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
    }

    /**
     * 过期日志扫描线程工厂
     */
    private class ClearScanThreadFactory : ThreadFactory {

        private val threadIndex = AtomicInteger(0)

        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, "Ember-FileLog-Clear-${threadIndex.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
    }
}