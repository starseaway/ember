package com.xinyi.ember.stacktrace

/**
 * 调用栈解析器
 *
 * 主要用于定位日志调用的位置。
 *
 * 原理：
 * - 扫描整个调用栈
 * - 找到最后一个 Ember 内部类
 * - 返回其后的第一个非 Ember 类
 *
 * @author 新一
 * @date 2026/6/26 9:47
 */
internal object StackTraceResolver {

    /**
     * Ember 内部包名前缀
     */
    private const val EMBER_PACKAGE = "com.xinyi.ember."

    /**
     * 获取日志调用位置
     *
     * @return 用户代码中的调用栈元素；无法定位时返回 null
     */
    fun resolve(): StackTraceElement? {
        val stack = Thread.currentThread().stackTrace

        // 记录最后一个 Ember 内部调用所在的位置
        var lastInternalIndex = -1
        for (i in stack.indices) {
            val className = stack[i].className
            // 跳过系统调用
            if (isSystemClass(className)) {
                continue
            }
            // 记录最新出现的 Ember 内部调用位置
            if (isEmberClass(className)) {
                lastInternalIndex = i
            }
        }

        // 调用栈中不存在 Ember 内部调用，无法继续解析
        if (lastInternalIndex == -1) {
            return null
        }

        // 寻找 Ember 内部调用之后的第一个外部调用
        for (i in lastInternalIndex + 1 until stack.size) {
            val className = stack[i].className
            if (!isSystemClass(className) && !isEmberClass(className)) {
                return stack[i]
            }
        }

        // 未找到调用位置
        return null
    }

    /**
     * 是否属于 JVM/Android 系统调用栈
     *
     * @param className 堆栈元素中的完整类名
     */
    private fun isSystemClass(className: String): Boolean {
        return className == Thread::class.java.name
    }

    /**
     * 是否属于 Ember 日志框架内部调用栈
     *
     * @param className 堆栈元素中的完整类名
     */
    private fun isEmberClass(className: String): Boolean {
        return className.startsWith(EMBER_PACKAGE)
    }
}