package com.xinyi.ember.file

import com.xinyi.ember.util.DateFormats
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale

/**
 * 文件日志写入器
 *
 * 主要负责日志文件的目录管理、按日分目录、按大小轮转和文件写入操作。
 *
 * > 需注意写入操作应在后台单线程中调用
 *
 * @param logDirPath 日志根目录路径，输出器会在该目录下按日期创建子目录
 * @param maxFileSize 单个日志文件最大大小（字节）；小于 [MIN_MAX_FILE_SIZE] 时使用 [DEFAULT_MAX_FILE_SIZE]
 *
 * @author 新一
 * @date 2026/6/26 10:42
 */
class FileLogger(private val logDirPath: String, maxFileSize: Long) {

    /**
     * 单个日志文件最大大小
     */
    private val maxFileSize: Long = normalizeMaxFileSize(maxFileSize)

    companion object {

        /** 单文件大小下限 64 KB；低于该值时回退为 [DEFAULT_MAX_FILE_SIZE] */
        const val MIN_MAX_FILE_SIZE = 64L * 1024

        /** 默认单文件大小上限 2 MB */
        const val DEFAULT_MAX_FILE_SIZE = 2L * 1024 * 1024

        /** 日期目录正则表达式 */
        private val DATE_DIR_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")

        /** 一个月的毫秒数 */
        private const val MONTH_MILLIS = 30L * 24L * 60L * 60L * 1000L

        /**
         * 规范化单文件大小上限
         *
         * @param maxFileSize 文件大小上限（字节）
         */
        @JvmStatic
        fun normalizeMaxFileSize(maxFileSize: Long): Long {
            return if (maxFileSize < MIN_MAX_FILE_SIZE) {
                DEFAULT_MAX_FILE_SIZE
            } else {
                maxFileSize
            }
        }

        private fun today(): String {
            return DateFormats.formatLogFileDate()
        }
    }

    /**
     * 是否头部换行
     *
     * 为 true 时，每次写入日志前会先写入一个空行。
     */
    var isHeadLine: Boolean = false

    /**
     * 是否尾部换行
     *
     * 为 true 时，每次写入日志后会自动换行。
     */
    var isEndLine: Boolean = true

    /**
     * 当前日志文件
     */ 
    private var currentLogFile: File? = null

    /**
     * 当前日期
     */
    private var currentDate: String = today()

    /**
     * 当前文件索引
     */
    private var currentFileIndex: Int = 0

    /**
     * 当前字符流写入器
     */
    private var writer: BufferedWriter? = null

    /**
     * 当前已经被打开文件流的文件对象
     */
    private var writerFile: File? = null

    init {
        // 确保日志目录存在
        if (ensureLogDirExists(currentDate)) {
            currentLogFile = getAvailableLogFile(currentDate)
        }
    }

    /**
     * 将内容写入日志文件
     *
     * @param content 要记录的内容
     */
    @Synchronized
    fun write(content: String) {
        val logFile = ensureCurrentLogFile() ?: return
        try {
            val bufferedWriter = openWriter(logFile)
            if (isHeadLine) {
                bufferedWriter.newLine()
            }
            bufferedWriter.write(content)
            if (isEndLine) {
                bufferedWriter.newLine()
            }
            bufferedWriter.flush()
        } catch (exception: IOException) {
            exception.printStackTrace()
            resetWriter()
        }
    }

    /**
     * 刷新并关闭当前文件写入流
     */
    @Synchronized
    fun close() {
        resetWriter()
    }

    /**
     * 获取当前日志文件路径
     *
     * @return 当前正在写入的日志文件绝对路径；文件不可用时返回空字符串
     */
    @Synchronized
    fun getLogFilePath(): String {
        return currentLogFile?.absolutePath.orEmpty()
    }

    /**
     * 清除几个月前的日志目录
     * 
     * @param monthCount 保留最近几个月日志，超过该时间范围的日期目录会被删除
     */
    @Synchronized
    fun clearLogFiles(monthCount: Int) {
        resetWriter()
        val logDir = File(logDirPath)
        if (!logDir.exists() || !logDir.isDirectory) {
            return
        }

        val expireTime = System.currentTimeMillis() - monthCount * MONTH_MILLIS
        logDir.listFiles()?.forEach { file ->
            // 跳过非日期目录
            if (!file.isDirectory || !file.name.matches(DATE_DIR_REGEX)) {
                return@forEach
            }

            val date = DateFormats.parseLogFileDate(file.name) ?: return@forEach

            // 删除过期日期目录
            if (date.time < expireTime) {
                deleteFileOrDirectory(file)
            }
        }

        // 重新获取当前日志文件
        currentLogFile = null
        if (ensureLogDirExists(currentDate)) {
            currentLogFile = getAvailableLogFile(currentDate)
        }
    }

    /**
     * 打开日志文件写入流
     *
     * @param logFile 日志文件对象
     * @return 字符流写入器
     * @throws IOException 如果无法打开日志文件
     */
    @Throws(IOException::class)
    private fun openWriter(logFile: File): BufferedWriter {
        if (writerFile != logFile) {
            resetWriter()
            writer = BufferedWriter(FileWriter(logFile, true))
            writerFile = logFile
        }
        return writer ?: throw IOException("无法打开日志文件")
    }

    /**
     * 重置字符流写入器
     */
    private fun resetWriter() {
        runCatching {
            writer?.flush()
            writer?.close()
        }
        writer = null
        writerFile = null
    }

    /**
     * 确保当前日志文件存在
     *
     * @return 当前日志文件对象
     */
    private fun ensureCurrentLogFile(): File? {
        val date = today()
        if (date != currentDate) {
            resetWriter()
            currentDate = date
            currentFileIndex = 0
            currentLogFile = null
        }

        if (!ensureLogDirExists(currentDate)) {
            return null
        }

        // 检查当前日志文件是否存在 || 大小是否超过最大值
        val logFile = currentLogFile
        if (logFile == null || logFile.length() > maxFileSize) {
            resetWriter()
            currentLogFile = getAvailableLogFile(currentDate)
        }
        return currentLogFile
    }

    /**
     * 确保日期目录存在
     *
     * @param date 日期
     * @return 是否成功创建日期目录
     */
    private fun ensureLogDirExists(date: String): Boolean {
        val rootDir = File(logDirPath)
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            return false
        }

        val dateDir = File(rootDir, date)
        if (dateDir.exists()) {
            return true
        }
        return dateDir.mkdirs()
    }

    /**
     * 获取可用的日志文件对象
     *
     * @param date 日期
     */
    private fun getAvailableLogFile(date: String): File? {
        val dateDir = File(logDirPath, date)
        var logFile = File(dateDir, createFileName(date, currentFileIndex))

        while (logFile.exists() && logFile.length() > maxFileSize) {
            currentFileIndex++
            logFile = File(dateDir, createFileName(date, currentFileIndex))
        }

        return runCatching {
            if (!logFile.exists() && !logFile.createNewFile()) {
                throw IOException("无法创建日志文件")
            }
            logFile
        }.getOrNull()
    }

    /**
     * 创建日志文件名
     *
     * @param date 日期
     * @param index 文件索引
     * @return 日志文件名
     */
    private fun createFileName(date: String, index: Int): String {
        return String.format(Locale.CHINA, "log_%s_%d.log", date, index)
    }

    /**
     * 删除文件或目录
     *
     * @param file 文件或目录对象
     */
    private fun deleteFileOrDirectory(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                deleteFileOrDirectory(it)
            }
        }
        file.delete()
    }
}