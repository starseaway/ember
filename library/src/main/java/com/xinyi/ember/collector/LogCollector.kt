package com.xinyi.ember.collector

import com.xinyi.ember.logger.Logger

/**
 * 日志收集器接口
 *
 * 收集器本质上也是一个 [Logger]，只是它额外携带 [name]。
 *
 * 业务模块可以持有收集器，并像普通日志对象一样调用打印日志的方法；
 * 收集器主要是方便将日志归属到某个模块、业务链路或一次临时排查会话，用于区分不同来源输出的日志。
 *
 * @author 新一
 * @date 2026/6/25 15:16
 */
interface LogCollector : Logger {

    /**
     * 收集器名称
     */
    val name: String
}