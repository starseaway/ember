package com.xinyi.ember.formatter

import com.xinyi.ember.model.LogRecord

/**
 * 日志格式化器
 *
 * 输出器可以按自己的场景选择不同格式化方式。
 *
 * > 例如 Logcat 更适合简短内容，文件更适合带时间与线程信息。
 *
 * @author 新一
 * @date 2026/6/25 16:52
 */
interface LogFormatter {

    /**
     * 格式化日志内容
     *
     * @param record 标准化日志记录
     * @return 最终输出内容
     */
    fun format(record: LogRecord): String
}