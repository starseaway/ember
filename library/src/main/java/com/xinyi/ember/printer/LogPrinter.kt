package com.xinyi.ember.printer

import com.xinyi.ember.model.LogRecord

/**
 * 日志输出器
 *
 * 每个输出器实现类，只需要关心一种输出目标。
 *
 * @author 新一
 * @date 2026/6/25 16:05
 */
interface LogPrinter {

    /**
     * 输出日志
     *
     * @param record 标准化日志记录
     */
    fun print(record: LogRecord)
}