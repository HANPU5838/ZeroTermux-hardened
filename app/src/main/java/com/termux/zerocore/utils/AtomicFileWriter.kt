package com.termux.zerocore.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * ZeroTermux 编辑器保存链路的数据安全工具。
 *
 * 解决三类历史缺陷：
 * 1. 保存失败被静默吞掉（PrintWriter 不 checkError / 只捕 FileNotFoundException）——这里所有失败都抛
 *    [IOException]，或由 [saveResult] 显式返回 [Result.failure]。
 * 2. 直接覆盖目标文件导致写一半断电即截断——这里先在目标目录写临时文件、fsync，再原子 rename。
 * 3. 硬编码 UTF-8 打开 GBK 文件再保存导致永久乱码——这里读取时做 BOM/UTF-8 合法性启发式探测，
 *    保存时沿用目标文件原有编码与 BOM。
 *
 * 该类不改变任何对外接口、端口或组件导出属性，仅提供本地文件读写能力。
 */
object AtomicFileWriter {

    /** 探测编码时最多读取的字节数，避免对超大文件做全量二次读取。 */
    private const val DETECT_SAMPLE_LIMIT = 4L * 1024 * 1024

    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val UTF16LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val UTF16BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

    /** 中文环境优先 GB18030（GBK 超集），运行时不支持时回退 GBK，再回退 UTF-8。 */
    private val GB18030: Charset = runCatching { Charset.forName("GB18030") }
        .recoverCatching { Charset.forName("GBK") }
        .getOrDefault(StandardCharsets.UTF_8)

    /** 读取结果：解码后的文本 + 真实编码 + 是否带 BOM。 */
    data class DecodedText(
        val content: String,
        val charset: Charset,
        val hasBom: Boolean
    )

    private data class EncodingInfo(val charset: Charset, val bomLength: Int)

    // ------------------------------------------------------------------
    // 读取（编码探测）
    // ------------------------------------------------------------------

    /** 读取文件内容，并按 BOM + UTF-8 合法性启发式自动解码。失败抛 [IOException]。 */
    @Throws(IOException::class)
    @JvmStatic
    fun read(file: File): DecodedText {
        if (!file.isFile) {
            throw IOException("Not a file: " + file.absolutePath)
        }
        val bytes = file.readBytes()
        val info = detect(bytes)
        val body = if (info.bomLength > 0 && info.bomLength <= bytes.size) {
            bytes.copyOfRange(info.bomLength, bytes.size)
        } else {
            bytes
        }
        return DecodedText(String(body, info.charset), info.charset, info.bomLength > 0)
    }

    /** 读取文件文本内容（自动探测编码）。失败抛 [IOException]。 */
    @Throws(IOException::class)
    @JvmStatic
    fun readText(file: File): String = read(file).content

    /** 探测指定文件使用的字符集（不含 BOM 信息）。读取失败时回退 UTF-8，不抛异常。 */
    @JvmStatic
    fun detectCharset(file: File): Charset {
        return try {
            detect(readSample(file)).charset
        } catch (e: IOException) {
            StandardCharsets.UTF_8
        }
    }

    /** 探测字节内容使用的字符集。 */
    @JvmStatic
    fun detectCharset(bytes: ByteArray): Charset = detect(bytes).charset

    private fun detect(bytes: ByteArray): EncodingInfo {
        if (startsWith(bytes, UTF8_BOM)) {
            return EncodingInfo(StandardCharsets.UTF_8, UTF8_BOM.size)
        }
        if (startsWith(bytes, UTF16LE_BOM)) {
            return EncodingInfo(StandardCharsets.UTF_16LE, UTF16LE_BOM.size)
        }
        if (startsWith(bytes, UTF16BE_BOM)) {
            return EncodingInfo(StandardCharsets.UTF_16BE, UTF16BE_BOM.size)
        }
        return EncodingInfo(if (isValidUtf8(bytes)) StandardCharsets.UTF_8 else GB18030, 0)
    }

    private fun startsWith(bytes: ByteArray, prefix: ByteArray): Boolean {
        if (bytes.size < prefix.size) return false
        for (index in prefix.indices) {
            if (bytes[index] != prefix[index]) return false
        }
        return true
    }

    /**
     * 严格校验字节序列是否为合法 UTF-8。若末尾存在被采样截断的多字节序列则先剔除该残段，
     * 避免因读取上限造成的误判。空序列视为合法 UTF-8。
     */
    private fun isValidUtf8(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val end = safeUtf8End(bytes)
        if (end <= 0) return true
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes, 0, end))
            true
        } catch (e: CharacterCodingException) {
            false
        }
    }

    /** 返回可安全解码的 UTF-8 长度：遇到末尾不完整的多字节序列时回退到该序列起始位置。 */
    private fun safeUtf8End(bytes: ByteArray): Int {
        var index = bytes.size - 1
        var continuation = 0
        while (index >= 0 && (bytes[index].toInt() and 0xC0) == 0x80) {
            index--
            continuation++
        }
        if (index < 0) return bytes.size
        val lead = bytes[index].toInt() and 0xFF
        val expected = when {
            lead and 0x80 == 0 -> 1
            lead and 0xE0 == 0xC0 -> 2
            lead and 0xF0 == 0xE0 -> 3
            lead and 0xF8 == 0xF0 -> 4
            else -> 0
        }
        if (expected > 1 && continuation < expected - 1) {
            return index
        }
        return bytes.size
    }

    private fun readSample(file: File): ByteArray {
        val length = file.length()
        if (length <= 0) return ByteArray(0)
        val limit = if (length > DETECT_SAMPLE_LIMIT) DETECT_SAMPLE_LIMIT.toInt() else length.toInt()
        val buffer = ByteArray(limit)
        FileInputStream(file).use { input ->
            var offset = 0
            while (offset < limit) {
                val read = input.read(buffer, offset, limit - offset)
                if (read < 0) break
                offset += read
            }
            return if (offset == limit) buffer else buffer.copyOf(offset)
        }
    }

    // ------------------------------------------------------------------
    // 写入（原子写 + 编码沿用）
    // ------------------------------------------------------------------

    /**
     * 原子保存文本，沿用目标文件原有编码与 BOM（文件不存在或无法探测时使用 UTF-8）。
     * 失败抛 [IOException]，绝不静默吞掉。
     */
    @Throws(IOException::class)
    @JvmStatic
    fun save(file: File, content: String) {
        val existing = if (file.isFile) {
            try {
                detect(readSample(file))
            } catch (e: IOException) {
                null
            }
        } else {
            null
        }
        save(file, content, existing?.charset ?: StandardCharsets.UTF_8, existing?.bomLength ?: 0)
    }

    /** 原子保存文本，显式指定编码（不额外写 BOM）。失败抛 [IOException]。 */
    @Throws(IOException::class)
    @JvmStatic
    fun save(file: File, content: String, charset: Charset) {
        save(file, content, charset, 0)
    }

    /**
     * 原子保存文本的 Result 版本，供不希望处理受检异常的调用方使用。
     * 失败返回 [Result.failure]，携带真实的 [IOException]。
     */
    @JvmStatic
    fun saveResult(file: File, content: String): Result<Unit> = runCatching { save(file, content) }

    /** 原子保存文本的 Result 版本，显式编码。 */
    @JvmStatic
    fun saveResult(file: File, content: String, charset: Charset): Result<Unit> =
        runCatching { save(file, content, charset) }

    /**
     * 原子写核心：编码 -> 临时文件 -> flush + fsync -> 原子 rename。
     * rename 不支持原子语义时回退普通替换，并在此前保留原文件备份；替换失败则回滚备份。
     */
    @Throws(IOException::class)
    @JvmStatic
    fun save(file: File, content: String, charset: Charset, bomLength: Int) {
        val target = file.absoluteFile
        val parent = target.parentFile
            ?: throw IOException("No parent directory for " + target.absolutePath)
        if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("Cannot create directory " + parent.absolutePath)
        }
        if (parent.isDirectory.not()) {
            throw IOException("Parent is not a directory: " + parent.absolutePath)
        }

        val payload = encode(content, charset, bomLength)
        val temp = File.createTempFile("." + target.name + ".", ".tmp", parent)
        var backup: File? = null
        try {
            FileOutputStream(temp).use { output ->
                output.write(payload)
                output.flush()
                output.fd.sync()
            }

            if (moveIntoPlace(temp, target)) {
                return
            }

            // 回退路径：先备份原文件，普通替换；替换失败则回滚。
            backup = if (target.isFile) {
                val copy = File.createTempFile("." + target.name + ".", ".bak", parent)
                target.copyTo(copy, overwrite = true)
                copy
            } else {
                null
            }
            val replaced = try {
                replaceNonAtomic(temp, target)
            } catch (e: IOException) {
                restore(backup, target)
                throw IOException("写入失败且已回滚: " + target.absolutePath, e)
            }
            if (!replaced) {
                restore(backup, target)
                throw IOException("写入失败且已回滚: " + target.absolutePath)
            }
            backup?.delete()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("写入失败: " + target.absolutePath, e)
        } finally {
            if (temp.exists()) {
                temp.delete()
            }
        }
    }

    private fun encode(content: String, charset: Charset, bomLength: Int): ByteArray {
        val body = content.toByteArray(charset)
        if (bomLength <= 0) return body
        val bom = when {
            bomLength == UTF8_BOM.size && charset == StandardCharsets.UTF_8 -> UTF8_BOM
            bomLength == UTF16LE_BOM.size && charset == StandardCharsets.UTF_16LE -> UTF16LE_BOM
            bomLength == UTF16BE_BOM.size && charset == StandardCharsets.UTF_16BE -> UTF16BE_BOM
            else -> null
        } ?: return body
        val result = ByteArray(bom.size + body.size)
        System.arraycopy(bom, 0, result, 0, bom.size)
        System.arraycopy(body, 0, result, bom.size, body.size)
        return result
    }

    /** 优先 API 26+ 的 Files.move(ATOMIC_MOVE)；返回 false 表示需要回退。 */
    private fun moveIntoPlace(source: File, target: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return moveAtomicOrFallback(source, target)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun moveAtomicOrFallback(source: File, target: File): Boolean {
        return try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
            true
        } catch (e: AtomicMoveNotSupportedException) {
            false
        } catch (e: UnsupportedOperationException) {
            false
        }
    }

    private fun replaceNonAtomic(source: File, target: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return try {
                moveReplace(source, target)
                true
            } catch (e: IOException) {
                false
            } catch (e: UnsupportedOperationException) {
                false
            }
        }
        return source.renameTo(target)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun moveReplace(source: File, target: File) {
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun restore(backup: File?, target: File) {
        if (backup == null || !backup.isFile) return
        try {
            backup.copyTo(target, overwrite = true)
            backup.delete()
        } catch (e: Exception) {
            // 回滚失败无法再补救，保留备份文件供手工恢复，并把信息交给上层。
            throw IOException("回滚失败，原文件备份保留在: " + backup.absolutePath, e)
        }
    }
}
