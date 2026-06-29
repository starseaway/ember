package com.xinyi.ember.collector

import com.xinyi.ember.model.LogLevel
import com.xinyi.ember.printer.LogPrinter

/**
 * 日志收集器配置
 *
 * 此配置不会影响普通全局日志，只对同名 [LogCollector] 输出的日志生效。
 *
 * @author 新一
 * @date 2026/6/25 15:00
 */
class CollectorConfig private constructor(
    /** 收集器名称，用于标识一类被特定指派的日志。 */
    val name: String,
    /** 是否启用该收集器；为 false 时，同名收集器的日志会被丢弃 */
    val isEnabled: Boolean,
    /** 收集器最低输出等级；为 null 时跟随全局最低输出等级 */
    val minLevel: LogLevel?,
    /** 收集器专属输出器列表，可用于把某类日志单独写入文件或上报 */
    val printers: List<LogPrinter>,
    /** 是否继承全局输出器；为 true 时会同时输出到全局配置的日志输出器 */
    val isInheritGlobalPrinters: Boolean
) {

    /**
     * 收集器配置构建器
     *
     * 默认启用收集器、继承全局输出器，并且不设置专属最低等级。
     *
     * @param name 收集器名称
     */
    class Builder(private val name: String) {

        private var isEnabled: Boolean = true
        private var minLevel: LogLevel? = null
        private var isInheritGlobalPrinters: Boolean = true
        private val printers: MutableList<LogPrinter> = mutableListOf()

        fun setEnabled(enabled: Boolean): Builder {
            this.isEnabled = enabled
            return this
        }

        fun setMinLevel(minLevel: LogLevel?): Builder {
            this.minLevel = minLevel
            return this
        }

        /**
         * 如果设置 `false` 且未添加专属输出器时，该收集器的日志不会输出内容。
         */
        fun setInheritGlobalPrinters(inheritGlobalPrinters: Boolean): Builder {
            this.isInheritGlobalPrinters = inheritGlobalPrinters
            return this
        }

        /**
         * 添加收集器专属输出器
         *
         * @param printer 日志输出器
         */
        fun addPrinter(printer: LogPrinter): Builder {
            printers.add(printer)
            return this
        }

        /**
         * 设置收集器专属输出器
         *
         * 会先清空已有输出器，再使用 [printers] 作为专属输出器列表。
         *
         * @param printers 新的专属输出器列表
         */
        fun setPrinters(vararg printers: LogPrinter): Builder {
            this.printers.clear()
            this.printers.addAll(printers)
            return this
        }

        /**
         * 清空收集器专属输出器
         *
         * 清空后，收集器不会输出到任何地方；若同时关闭了继承全局输出器，该收集器的日志会被丢弃。
         */
        fun clearPrinters(): Builder {
            this.printers.clear()
            return this
        }

        /**
         * 构建收集器配置
         *
         * @return 不可变的收集器配置
         */
        fun build(): CollectorConfig {
            require(name.isNotBlank()) { "Collector name must not be blank" }
            return CollectorConfig(
                name = name,
                isEnabled = isEnabled,
                minLevel = minLevel,
                printers = printers.toList(),
                isInheritGlobalPrinters = isInheritGlobalPrinters
            )
        }
    }
}