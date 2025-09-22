package com.example.kiosk_sensors_plugin

import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import androidx.annotation.RequiresApi
import com.pos.poslibusb.PosLog
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.Arrays
import java.util.LinkedList
import kotlin.experimental.inv


object SiUtils {
    private const val TAG = "SiDemo"
    const val TEXT_SERIAL_COM = "SerialCOM"
    const val TEXT_LEGACY_SERIAL_COM = "/dev/ttyS"
    const val TEXT_USB_SERIAL_COM = "/dev/ttyUSB"
    const val TEXT_VIRTUAL_COM = "/dev/ttyACM"
    const val TEXT_HIDRAW = "/dev/hidraw"
    const val TEXT_PRINTER_CLASS_OLD = "/dev/usblp"
    const val TEXT_PRINTER_CLASS = "/dev/usb/lp"
    const val TEXT_NETWORK = "Network"
    fun loge(msg: String?) {
        PosLog.loge(TAG, msg)
    }

    fun logw(msg: String?) {
        PosLog.logw(TAG, msg)
    }

    fun logi(msg: String?) {
        PosLog.logw(TAG, msg)
    }

    fun logd(msg: String?) {
        PosLog.logd(TAG, msg)
    }

    fun logv(msg: String?) {
        PosLog.logv(TAG, msg)
    }

    fun loge(tag: String?, msg: String?) {
        PosLog.loge(tag, msg)
    }

    fun logw(tag: String?, msg: String?) {
        PosLog.logw(tag, msg)
    }

    fun logi(tag: String?, msg: String?) {
        PosLog.logw(tag, msg)
    }

    fun logd(tag: String?, msg: String?) {
        PosLog.logd(tag, msg)
    }

    fun logv(tag: String?, msg: String?) {
        PosLog.logv(tag, msg)
    }

    fun getResultString(error: Int): String {
        var strRes = "Unknown: result $error"
        when (error) {
            RESULT_TOKEN_DEF.RESULT_SUCCESS -> strRes = "RESULT_SUCCESS"
            RESULT_TOKEN_DEF.RESULT_ERROR -> strRes = "RESULT_ERROR"
            RESULT_TOKEN_DEF.RESULT_ERROR_INVALID_PARAM -> strRes = "RESULT_ERROR_INVALID_PARAM"
            RESULT_TOKEN_DEF.RESULT_ERROR_ACCESS -> strRes = "RESULT_ERROR_ACCESS"
            RESULT_TOKEN_DEF.RESULT_ERROR_NO_DEVICE -> strRes = "RESULT_ERROR_NO_DEVICE"
            RESULT_TOKEN_DEF.RESULT_ERROR_NOT_FOUND -> strRes = "RESULT_ERROR_NOT_FOUND"
            RESULT_TOKEN_DEF.RESULT_ERROR_BUSY -> strRes = "RESULT_ERROR_BUSY"
            RESULT_TOKEN_DEF.RESULT_ERROR_TIMEOUT -> strRes = "RESULT_ERROR_TIMEOUT"
            RESULT_TOKEN_DEF.RESULT_ERROR_OVERFLOW -> strRes = "RESULT_ERROR_OVERFLOW"
            RESULT_TOKEN_DEF.RESULT_ERROR_PIPE -> strRes = "RESULT_ERROR_PIPE"
            RESULT_TOKEN_DEF.RESULT_ERROR_INTERRUPTED -> strRes = "RESULT_ERROR_INTERRUPTED"
            RESULT_TOKEN_DEF.RESULT_ERROR_NO_MEM -> strRes = "RESULT_ERROR_NO_MEM"
            RESULT_TOKEN_DEF.RESULT_ERROR_NOT_SUPPORTED -> strRes = "RESULT_ERROR_NOT_SUPPORTED"
            RESULT_TOKEN_DEF.RESULT_ERROR_IO -> strRes = "RESULT_ERROR_IO"
            RESULT_TOKEN_DEF.RESULT_ERROR_OTHER -> strRes = "RESULT_ERROR_OTHER"
        }
        return strRes
    }

    fun GetLightString(type: Int, firmwareVersion: Int): String {
        if (firmwareVersion == 12) {
            when (type) {
                LIGHT_TYPE_V12_TOKEN_DEF.LIGHT_TYPE_V12_NONE -> return "Light Dark"
                LIGHT_TYPE_V12_TOKEN_DEF.LIGHT_TYPE_V12_NORMAL -> return "Light Normal"
                LIGHT_TYPE_V12_TOKEN_DEF.LIGHT_TYPE_V12_SMOOTH -> return "Smooth"
                LIGHT_TYPE_V12_TOKEN_DEF.LIGHT_TYPE_V12_FLASH -> return "Flash"
                LIGHT_TYPE_V12_TOKEN_DEF.LIGHT_TYPE_V12_BREATHE -> return "Breath"
            }
        } else {
            when (type) {
                LIGHT_TYPE_TOKEN_DEF.LIGHT_TYPE_NONE -> return "Light Dark"
                LIGHT_TYPE_TOKEN_DEF.LIGHT_TYPE_NORMAL -> return "Light Normal"
                LIGHT_TYPE_TOKEN_DEF.LIGHT_TYPE_SINGLE_FLASH -> return "Single Light Flash"
                LIGHT_TYPE_TOKEN_DEF.LIGHT_TYPE_SMOOTH -> return "Smooth"
                LIGHT_TYPE_TOKEN_DEF.LIGHT_TYPE_FLASH -> return "Flash"
                LIGHT_TYPE_TOKEN_DEF.LIGHT_TYPE_BREATHE -> return "Breath"
            }
        }
        return "Undefine"
    }

    fun fileSize(size: Float): Array<String?> {
        var size = size
        val str = arrayOf("Byte", "KiB", "MiB")
        var index = 0
        while (size >= 1024) {
            size /= 1024f
            ++index
            if (index >= 2) break
        }
        val formatter = DecimalFormat("#.##")
        formatter.groupingSize = 3
        val result = arrayOfNulls<String>(2)
        result[0] = formatter.format(size.toDouble())
        result[1] = str[index]
        return result
    }

    fun compareVersions(version1: String, version2: String): Int {
        var version1 = version1
        var version2 = version2
        if (version1.isEmpty()) version1 = "0"
        if (version2.isEmpty()) version2 = "0"
        val levels1 = version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val levels2 = version2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val length = Math.max(levels1.size, levels2.size)
        for (i in 0 until length) {
            val v1 = if (i < levels1.size) levels1[i].toInt() else 0
            val v2 = if (i < levels2.size) levels2[i].toInt() else 0
            val compare = v1.compareTo(v2)
            if (compare != 0) return compare
        }
        return 0
    }

    fun isMatch(source: ByteArray, pattern: ByteArray, pos: Int): Boolean {
        for (i in pattern.indices) {
            if (pattern[i] != source[pos + i]) {
                return false
            }
        }
        return true
    }

    fun split(source: ByteArray, pattern: ByteArray): List<ByteArray> {
        val l: MutableList<ByteArray> = LinkedList()
        var blockStart = 0
        var i = 0
        while (i < source.size) {
            if (isMatch(source, pattern, i)) {
                l.add(Arrays.copyOfRange(source, blockStart, i))
                blockStart = i + pattern.size
                i = blockStart
            }
            i++
        }
        l.add(Arrays.copyOfRange(source, blockStart, source.size))
        return l
    }

    fun getArrayIndex(arr: ByteArray, value: Byte): Int {
        for (i in arr.indices) if (arr[i] == value) return i
        return -1
    }

    fun getArrayLastIndex(arr: ByteArray, value: Byte): Int {
        for (i in arr.indices.reversed()) if (arr[i] == value) return i
        return -1
    }

    // Java, Keil C51(TW100) is BIG_ENDIAN
    // Jni C++, C# is LITTLE_ENDIAN
    fun byteArray2Float(b: ByteArray?): Float {
        if (b == null) return 0f
        val buf = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN)
        return buf.float
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun longToBytes(x: Long): ByteArray {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putLong(x)
        return buffer.array()
    }

    fun byteArray2Int(b: ByteArray?): Int {
        if (b == null) return 0
        val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        return buf.int
    }

    fun byteArray2IntArray(b: ByteArray): IntArray {
        val res = IntArray(b.size)
        for (i in res.indices) res[i] = b[i].toInt() and 0xFF
        return res
    }

    fun byte2String(b: Byte): String {
        val bArr = ByteArray(2)
        Arrays.fill(bArr, 0x00.toByte())
        bArr[0] = b
        return kotlin.String()
    }

    fun setBit(value: Byte, bit: Int): Byte {
        var value = value
        value = (value.toInt() or (1 shl bit).toByte().toInt()).toByte()
        return value
    }

    fun clearBit(value: Byte, bit: Int): Byte {
        var value = value
        value = (value.toInt() and (1 shl bit).toByte().inv().toInt()).toByte()
        return value
    }

    fun checkDuplicated_withSet(sValueTemp: IntArray): Boolean {
        val sValueSet: MutableSet<Int> = HashSet()
        for (tempValueSet in sValueTemp) {
            if (sValueSet.contains(tempValueSet)) return true else if (tempValueSet != 0) sValueSet.add(
                tempValueSet
            )
        }
        return false
    }

    object RESULT_TOKEN_DEF {
        const val RESULT_SUCCESS = 0
        const val RESULT_ERROR = -1
        const val RESULT_ERROR_INVALID_PARAM = -2
        const val RESULT_ERROR_ACCESS = -3
        const val RESULT_ERROR_NO_DEVICE = -4
        const val RESULT_ERROR_NOT_FOUND = -5
        const val RESULT_ERROR_BUSY = -6
        const val RESULT_ERROR_TIMEOUT = -7
        const val RESULT_ERROR_OVERFLOW = -8
        const val RESULT_ERROR_PIPE = -9
        const val RESULT_ERROR_INTERRUPTED = -10
        const val RESULT_ERROR_NO_MEM = -11
        const val RESULT_ERROR_NOT_SUPPORTED = -12
        const val RESULT_ERROR_IO = -13
        const val RESULT_ERROR_OTHER = -99
    }

    object LIGHT_TYPE_V12_TOKEN_DEF {
        const val LIGHT_TYPE_V12_NONE = 0
        const val LIGHT_TYPE_V12_NORMAL = 0xFF
        const val LIGHT_TYPE_V12_SMOOTH = 1
        const val LIGHT_TYPE_V12_FLASH = 2
        const val LIGHT_TYPE_V12_BREATHE = 3
    }

    object LIGHT_TYPE_TOKEN_DEF {
        const val LIGHT_TYPE_NONE = 0
        const val LIGHT_TYPE_NORMAL = 1
        const val LIGHT_TYPE_SINGLE_FLASH = 2
        const val LIGHT_TYPE_SMOOTH = 3
        const val LIGHT_TYPE_FLASH = 4
        const val LIGHT_TYPE_BREATHE = 5
    }

    object TIMER_TOKEN_DEF {
        const val TIMER_SECOND = 1
        const val TIMER_MINUTE = 60
        const val TIMER_HOUR = 3600
    }

    object ALERT_BUTTON_TOKEN_DEF {
        const val OK = 1
        const val OKCANCEL = 2
    }
}
