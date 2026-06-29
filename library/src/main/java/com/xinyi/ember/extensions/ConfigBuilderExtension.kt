@file:JvmName("ConfigBuilderExtension")

package com.xinyi.ember.extensions

import com.xinyi.ember.collector.CollectorConfig
import com.xinyi.ember.config.LogConfig
import com.xinyi.ember.file.FileLogger
import com.xinyi.ember.printer.FileLogPrinter

/**
 * 日志配置构建器扩展函数
 *
 * @author 杨耿雷
 * @date 2026/6/26 14:47
 */

/**
 * 追加文件日志输出器
 *
 * 默认开启自动清理：保留 [FileLogPrinter.DEFAULT_RETAIN_MONTHS] 个月，
 * 每 [FileLogPrinter.DEFAULT_SCAN_INTERVAL_MS] 毫秒扫描一次。
 *
 * @param logDirPath 日志根目录
 * @param maxFileSize 单文件大小上限（字节）；小于 64 KB 时回退 [FileLogger.DEFAULT_MAX_FILE_SIZE]
 * @param retainMonthCount 自动清理保留月数
 * @param clearScanIntervalMillis 自动清理扫描间隔（毫秒）
 */
fun LogConfig.Builder.addFilePrinter(
    logDirPath: String,
    maxFileSize: Long = FileLogger.DEFAULT_MAX_FILE_SIZE,
    retainMonthCount: Int = FileLogPrinter.DEFAULT_RETAIN_MONTHS,
    clearScanIntervalMillis: Long = FileLogPrinter.DEFAULT_SCAN_INTERVAL_MS
): LogConfig.Builder = addPrinter(
    FileLogPrinter(
        logDirPath = logDirPath,
        maxFileSize = maxFileSize,
        retainMonthCount = retainMonthCount,
        clearScanIntervalMillis = clearScanIntervalMillis
    )
)

/**
 * 为收集器配置添加文件日志输出器
 *
 * @param logDirPath 日志根目录
 * @param maxFileSize 单文件大小上限（字节）；小于 64 KB 时回退 [FileLogger.DEFAULT_MAX_FILE_SIZE]
 * @param retainMonthCount 自动清理保留月数
 * @param clearScanIntervalMillis 自动清理扫描间隔（毫秒）
 */
fun CollectorConfig.Builder.addFilePrinter(
    logDirPath: String,
    maxFileSize: Long = FileLogger.DEFAULT_MAX_FILE_SIZE,
    retainMonthCount: Int = FileLogPrinter.DEFAULT_RETAIN_MONTHS,
    clearScanIntervalMillis: Long = FileLogPrinter.DEFAULT_SCAN_INTERVAL_MS
): CollectorConfig.Builder = addPrinter(
    FileLogPrinter(
        logDirPath = logDirPath,
        maxFileSize = maxFileSize,
        retainMonthCount = retainMonthCount,
        clearScanIntervalMillis = clearScanIntervalMillis
    )
)