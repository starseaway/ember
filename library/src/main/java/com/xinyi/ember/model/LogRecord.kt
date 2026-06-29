package com.xinyi.ember.model

/**
 * 调度层产出的一条标准化日志，供各个日志输出器消费。
 *
 * @property level 日志等级
 * @property tag 日志标签
 * @property message 日志正文
 * @property throwable 异常
 * @property timeMillis 产生时间（毫秒）
 * @property threadName 线程名称
 * @property caller 调用出处
 * @property productTypeTag 产品类型标签
 * @property collectorName 收集器名称
 * 
 * @author 新一
 * @date 2026/6/26 9:11
 */
data class LogRecord(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable?,
    val timeMillis: Long,
    val threadName: String,
    val caller: StackTraceElement?,
    val productTypeTag: String?,
    val collectorName: String? = null
)