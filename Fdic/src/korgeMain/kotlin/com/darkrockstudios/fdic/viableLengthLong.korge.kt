package com.darkrockstudios.fdic

import korlibs.io.stream.FastByteArrayInputStream
import korlibs.memory.ByteArrayBuilder


internal fun ByteArrayBuilder.writeVariableLong(value: Long) {
    var v = value
    do {
        var b = (v and 0x7F).toByte()
        v = v ushr 7
        if (v != 0L) {
            b = (b.toInt() or 0x80).toByte()
        }
        append(b)
    } while (v != 0L)
}

internal fun FastByteArrayInputStream.readVariableLong(): Long {
    var value = 0L
    var shift = 0
    while (true) {
        val b = readS8() and 0xFF
        value = value or ((b.toLong() and 0x7F) shl shift)
        if (b and 0x80 == 0) break
        shift += 7
    }
    return value
}