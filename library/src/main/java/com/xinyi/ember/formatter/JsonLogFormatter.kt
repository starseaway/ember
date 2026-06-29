package com.xinyi.ember.formatter

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON 日志格式化工具
 * 
 * 当 JSON 内容为对象或数组时，会先使用 `org.json` 做一次合法性校验。
 * 
 * @author 新一
 * @date 2026/6/26 14:42
 */
internal object JsonLogFormatter {

    /**
     * JSON 展开格式化时，每一层嵌套使用的空格数量
     */
    const val JSON_PRETTY_INDENT = 4

    /**
     * 上下边框
     */
    private const val LINE_TOP = "╔═══════════════════════════════════════════════════════════════════════════════════════"
    private const val LINE_BOTTOM = "╚═══════════════════════════════════════════════════════════════════════════════════════"

    /**
     * 格式化 JSON 内容
     *
     * @param json JSON 字符串
     * @param indent 缩进空格数量；大于 0 时展开 JSON，等于 0 时保持紧凑格式
     * @return 带边框的 JSON 日志内容；解析失败时返回错误信息和原始内容
     */
    fun format(json: String, indent: Int): String {
        val jsonContent = parseJson(json, indent)
        if (jsonContent.isEmpty()) {
            return "Empty or Null json content"
        }

        val builder = StringBuilder()
        builder.append(LINE_TOP).append("\n")
        jsonContent.lines().forEach { line ->
            builder.append("║ ").append(line).append("\n")
        }
        builder.append(LINE_BOTTOM)
        return builder.toString()
    }

    /**
     * 解析 JSON 字符串
     *
     * @param json JSON 原始字符串
     * @param indent 缩进空格数量
     * @return 解析后的 JSON 文本；如果不是 JSON 对象或数组，则返回原始文本
     */
    private fun parseJson(json: String, indent: Int): String {
        val message = json.trim()
        return try {
            when {
                // 对象
                message.startsWith("{") -> {
                    val jsonObject = JSONObject(message)
                    if (indent > 0) {
                        jsonObject.toString(indent)
                    } else {
                        jsonObject.toString()
                    }
                }
                // 数组
                message.startsWith("[") -> {
                    val jsonArray = JSONArray(message)
                    if (indent > 0) {
                        jsonArray.toString(indent)
                    } else {
                        jsonArray.toString()
                    }
                }

                else -> message
            }
        } catch (exception: JSONException) {
            "${exception.message}\n$message"
        }
    }
}