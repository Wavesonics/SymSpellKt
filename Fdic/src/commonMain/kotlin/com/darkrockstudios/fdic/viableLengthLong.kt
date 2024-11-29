package com.darkrockstudios.fdic

import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.Source
import kotlin.experimental.or

//internal fun BufferedSink.writeVariableLong(value: Long) {
//	var v = value
//	do {
//		var b = (v and 0x7F).toByte()
//		v = v ushr 7
//		if (v != 0L) {
//			b = (b.toInt() or 0x80).toByte()
//		}
//		writeByte(b.toInt())
//	} while (v != 0L)
//}
//
//internal fun BufferedSource.readVariableLong(): Long {
//	var value = 0L
//	var shift = 0
//	while (true) {
//		val b = readByte().toInt() and 0xFF
//		value = value or ((b.toLong() and 0x7F) shl shift)
//		if (b and 0x80 == 0) break
//		shift += 7
//	}
//	return value
//}

fun encodeVariableLengthLong(value: Long, sink: Sink) {
	var remainingValue = value
	do {
		var byte = (remainingValue and 0x7F).toByte()
		remainingValue = remainingValue ushr 7
		if (remainingValue != 0L) {
			byte = byte or 0x80.toByte()
		}
		sink.writeByte(byte)
	} while (remainingValue != 0L)
}

fun Source.decodeVariableLengthLong(): Long {
	var value = 0L
	var shift = 0
	var n = 0
	while (true) {
		++n
		val byte = readByte().toInt()
		val data = ((byte.toLong() and 0x7F) shl shift)
		value = value or data
		if (byte and 0x80 == 0) {
			break
		}
		shift += 7
	}
	return value
}