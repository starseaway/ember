package com.xinyi.ember.collector

import com.xinyi.ember.dispatcher.LogDispatcher
import com.xinyi.ember.model.LogLevel

/**
 * 范围性日志收集器
 *
 * @property name 收集器名称
 * @property dispatcherProvider 日志调度器提供者，用于在 [LogDispatcher] 更新后仍然拿到最新调度器
 *
 * @author 新一
 * @date 2026/6/26 8:46
 */
internal class ScopedLogCollector(
    override val name: String,
    private val dispatcherProvider: () -> LogDispatcher
) : LogCollector {

    override fun v() {
        printLog(LogLevel.VERBOSE, null, null)
    }

    override fun v(msg: Any?) {
        printLog(LogLevel.VERBOSE, null, msg)
    }

    override fun v(tag: String?, msg: Any?) {
        printLog(LogLevel.VERBOSE, tag, msg)
    }

    override fun d() {
        printLog(LogLevel.DEBUG, null, null)
    }

    override fun d(msg: Any?) {
        printLog(LogLevel.DEBUG, null, msg)
    }

    override fun d(tag: String?, msg: Any?) {
        printLog(LogLevel.DEBUG, tag, msg)
    }

    override fun i() {
        printLog(LogLevel.INFO, null, null)
    }

    override fun i(msg: Any?) {
        printLog(LogLevel.INFO, null, msg)
    }

    override fun i(tag: String?, msg: Any?) {
        printLog(LogLevel.INFO, tag, msg)
    }

    override fun w() {
        printLog(LogLevel.WARN, null, null)
    }

    override fun w(msg: Any?) {
        printLog(LogLevel.WARN, null, msg)
    }

    override fun w(tag: String?, msg: Any?) {
        printLog(LogLevel.WARN, tag, msg)
    }

    override fun e() {
        printLog(LogLevel.ERROR, null, null)
    }

    override fun e(msg: Any?) {
        printLog(LogLevel.ERROR, null, msg)
    }

    override fun e(tag: String?, msg: Any?) {
        printLog(LogLevel.ERROR, tag, msg)
    }

    override fun e(msg: Any?, throwable: Throwable?) {
        printLog(LogLevel.ERROR, null, msg, throwable)
    }

    override fun e(tag: String?, msg: Any?, throwable: Throwable?) {
        printLog(LogLevel.ERROR, tag, msg, throwable)
    }

    override fun wtf() {
        printLog(LogLevel.ASSERT, null, null)
    }

    override fun wtf(msg: Any?) {
        printLog(LogLevel.ASSERT, null, msg)
    }

    override fun wtf(tag: String?, msg: Any?) {
        printLog(LogLevel.ASSERT, tag, msg)
    }

    override fun wtf(tag: String?, msg: Any?, throwable: Throwable?) {
        printLog(LogLevel.ASSERT, tag, msg, throwable)
    }

    override fun json(jsonFormat: String?) {
        dispatcherProvider().json(null, jsonFormat, false, name)
    }

    override fun json(jsonFormat: String?, isFormatJson: Boolean) {
        dispatcherProvider().json(null, jsonFormat, isFormatJson, name)
    }

    override fun json(tag: String?, jsonFormat: String?) {
        dispatcherProvider().json(tag, jsonFormat, false, name)
    }

    override fun json(tag: String?, jsonFormat: String?, isFormatJson: Boolean) {
        dispatcherProvider().json(tag, jsonFormat, isFormatJson, name)
    }

    /**
     * 打印收集器日志
     *
     * @param level 日志等级
     * @param tag 本次日志标签
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    private fun printLog(level: LogLevel, tag: String?, msg: Any?, throwable: Throwable? = null) {
        dispatcherProvider().log(level, tag, msg, throwable, name)
    }
}