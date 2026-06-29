package com.xinyi.ember.config

import com.xinyi.ember.collector.CollectorConfig
import com.xinyi.ember.model.LogLevel
import com.xinyi.ember.printer.AndroidLogPrinter
import com.xinyi.ember.printer.LogPrinter

/**
 * Ember 日志框架的全局运行配置
 * 
 * 使用 [Builder] 创建不可变快照；运行时，局部变更应基于已有配置，可 copy 后再 build 得到新的配置。
 * 
 * @author 新一
 * @date 2026/6/25 14:40
 */
class LogConfig private constructor(
    /** 是否启用日志；为 false 时所有日志在调度层直接丢弃。*/
    val isEnabled: Boolean,
    /** 最低输出等级；低于该等级的日志不会被输出。*/
    val minLevel: LogLevel,
    /** 默认日志标签；未传 tag 且调用栈解析失败时作为兜底 tag。*/
    val defaultTag: String,
    /** 产品类型标签；会拼接在日志正文前方，可用于区分应用不同版本的输出日志。*/
    val productTypeTag: String?,
    /** 是否在日志正文前拼接调用出处，格式为 `(File.kt:line)#method`；不影响 Logcat tag 推导。*/
    val isIncludeCaller: Boolean,
    /** 日志输出器列表；同一条日志会分发给列表中的每个输出器，列表为空时不会产生任何输出。*/
    val printers: List<LogPrinter>,
    /** 收集器配置表，key 为收集器名称；未配置的收集器默认跟随全局日志策略。*/
    val collectors: Map<String, CollectorConfig>
) {

    companion object {

        /**
         * 默认的全局兜底 tag 日志标签
         */
        private const val DEFAULT_TAG = "Ember"

        /**
         * 创建默认配置
         */
        @JvmStatic
        fun defaultConfig(): LogConfig {
            return Builder().build()
        }
    }

    /**
     * 配置构建器
     *
     * 默认配置会开启日志、允许所有等级输出，并内置一个 [AndroidLogPrinter]。
     */
    class Builder {

        private var isEnabled: Boolean = true
        private var minLevel: LogLevel = LogLevel.VERBOSE
        private var defaultTag: String = DEFAULT_TAG
        private var productTypeTag: String? = null
        private var isIncludeCaller: Boolean = true
        private val printers: MutableList<LogPrinter> = mutableListOf(AndroidLogPrinter())
        private val collectors: MutableMap<String, CollectorConfig> = linkedMapOf()

        /**
         * 根据已有配置创建构建器
         *
         * @param config 已有日志配置
         */
        constructor(config: LogConfig) {
            this.isEnabled = config.isEnabled
            this.minLevel = config.minLevel
            this.defaultTag = config.defaultTag
            this.productTypeTag = config.productTypeTag
            this.isIncludeCaller = config.isIncludeCaller
            this.printers.clear()
            this.printers.addAll(config.printers)
            this.collectors.clear()
            this.collectors.putAll(config.collectors)
        }

        /**
         * 创建默认配置构建器
         */
        constructor()

        fun setMinLevel(minLevel: LogLevel): Builder {
            this.minLevel = minLevel
            return this
        }

        fun setEnabled(enabled: Boolean): Builder {
            this.isEnabled = enabled
            return this
        }

        fun setDefaultTag(defaultTag: String): Builder {
            this.defaultTag = defaultTag
            return this
        }

        fun setProductTypeTag(productTypeTag: String?): Builder {
            this.productTypeTag = productTypeTag
            return this
        }

        fun setIncludeCaller(includeCaller: Boolean): Builder {
            this.isIncludeCaller = includeCaller
            return this
        }

        /**
         * 添加日志输出器
         *
         * @param printer 需要追加的日志输出器
         */
        fun addPrinter(printer: LogPrinter): Builder {
            printers.add(printer)
            return this
        }

        /**
         * 清空日志输出器
         *
         * 清空后如果没有重新添加输出器，日志不会输出到任何地方。
         */
        fun clearPrinters(): Builder {
            printers.clear()
            return this
        }

        /**
         * 设置日志输出器
         *
         * @param printers 新的日志输出器列表
         */
        fun setPrinters(vararg printers: LogPrinter): Builder {
            this.printers.clear()
            this.printers.addAll(printers)
            return this
        }

        /**
         * 注册收集器配置
         *
         * @param collector 收集器配置
         */
        fun addCollector(collector: CollectorConfig): Builder {
            collectors[collector.name] = collector
            return this
        }

        /**
         * 移除收集器配置
         *
         * 移除后，同名收集器不会消失，但会回到默认行为。
         *
         * @param name 收集器名称
         */
        fun removeCollector(name: String): Builder {
            collectors.remove(name)
            return this
        }

        /**
         * 清空所有收集器配置
         *
         * 清空后，所有收集器都会回到默认行为。
         */
        fun clearCollectors(): Builder {
            collectors.clear()
            return this
        }

        /**
         * 设置收集器配置列表
         *
         * 会先清空已有收集器配置，再按 [collectors] 注册新的配置。
         *
         * @param collectors 新的收集器配置列表
         */
        fun setCollectors(vararg collectors: CollectorConfig): Builder {
            this.collectors.clear()
            collectors.forEach {
                this.collectors[it.name] = it
            }
            return this
        }

        /**
         * 构建配置
         *
         * @return 不可变的日志配置对象
         */
        fun build(): LogConfig {
            return LogConfig(
                isEnabled = isEnabled,
                minLevel = minLevel,
                defaultTag = defaultTag,
                productTypeTag = productTypeTag,
                isIncludeCaller = isIncludeCaller,
                printers = printers.toList(),
                collectors = collectors.toMap()
            )
        }
    }
}