package com.xinyi.ember.logger

/**
 * 日志能力接口
 *
 * 业务模块建议依赖该接口，而不是直接依赖全局入口。
 * 这样模块只知道“可以打印日志”，不需要关心日志最终输出到 Logcat、文件还是其它位置。
 *
 * @author 新一
 * @date 2026/6/25 15:12
 */
interface Logger {

    /**
     * 输出 Verbose 级别日志
     */
    fun v()

    /**
     * 输出 Verbose 级别日志
     *
     * @param msg 日志内容
     */
    fun v(msg: Any?)

    /**
     * 输出 Verbose 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    fun v(tag: String?, msg: Any?)

    /**
     * 输出 Debug 级别日志
     */
    fun d()

    /**
     * 输出 Debug 级别日志
     *
     * @param msg 日志内容
     */
    fun d(msg: Any?)

    /**
     * 输出 Debug 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    fun d(tag: String?, msg: Any?)

    /**
     * 输出 Info 级别日志
     */
    fun i()

    /**
     * 输出 Info 级别日志
     *
     * @param msg 日志内容
     */
    fun i(msg: Any?)

    /**
     * 输出 Info 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    fun i(tag: String?, msg: Any?)

    /**
     * 输出 Warning 级别日志
     */
    fun w()

    /**
     * 输出 Warning 级别日志
     *
     * @param msg 日志内容
     */
    fun w(msg: Any?)

    /**
     * 输出 Warning 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    fun w(tag: String?, msg: Any?)

    /**
     * 输出 Error 级别日志
     */
    fun e()

    /**
     * 输出 Error 级别日志
     *
     * @param msg 日志内容
     */
    fun e(msg: Any?)

    /**
     * 输出 Error 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    fun e(tag: String?, msg: Any?)

    /**
     * 输出带异常堆栈的 Error 级别日志
     *
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    fun e(msg: Any?, throwable: Throwable?)

    /**
     * 输出带异常堆栈的 Error 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    fun e(tag: String?, msg: Any?, throwable: Throwable?)

    /**
     * 输出 Assert 级别日志
     */
    fun wtf()

    /**
     * 输出 Assert 级别日志
     *
     * @param msg 日志内容
     */
    fun wtf(msg: Any?)

    /**
     * 输出 Assert 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     */
    fun wtf(tag: String?, msg: Any?)

    /**
     * 输出带异常堆栈的 Assert 级别日志
     *
     * @param tag 本次日志标签
     * @param msg 日志内容
     * @param throwable 异常信息
     */
    fun wtf(tag: String?, msg: Any?, throwable: Throwable?)

    /**
     * 输出 JSON 日志
     *
     * @param jsonFormat JSON 字符串
     */
    fun json(jsonFormat: String?)

    /**
     * 输出 JSON 日志
     *
     * @param jsonFormat JSON 字符串
     * @param isFormatJson 是否展开格式化 JSON
     */
    fun json(jsonFormat: String?, isFormatJson: Boolean)

    /**
     * 输出 JSON 日志
     *
     * @param tag 本次日志标签
     * @param jsonFormat JSON 字符串
     */
    fun json(tag: String?, jsonFormat: String?)

    /**
     * 输出 JSON 日志
     *
     * @param tag 本次日志标签
     * @param jsonFormat JSON 字符串
     * @param isFormatJson 是否展开格式化 JSON
     */
    fun json(tag: String?, jsonFormat: String?, isFormatJson: Boolean)
}