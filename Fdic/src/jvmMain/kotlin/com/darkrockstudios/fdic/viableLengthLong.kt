package com.darkrockstudios.fdic

import okio.BufferedSink
import okio.BufferedSource

internal fun BufferedSink.writeVariableLong(value: Long) {
	var v = value
	do {
		var b = (v and 0x7F).toByte()
		v = v ushr 7
		if (v != 0L) {
			b = (b.toInt() or 0x80).toByte()
		}
		writeByte(b.toInt())
	} while (v != 0L)
}

internal fun BufferedSource.readVariableLong(): Long {
	var value = 0L
	var shift = 0
	while (true) {
		val b = readByte().toInt() and 0xFF
		value = value or ((b.toLong() and 0x7F) shl shift)
		if (b and 0x80 == 0) break
		shift += 7
	}
	return value
}