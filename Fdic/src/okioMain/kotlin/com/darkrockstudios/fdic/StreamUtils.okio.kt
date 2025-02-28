package com.darkrockstudios.fdic

import okio.BufferedSink
import okio.BufferedSource

internal fun BufferedSink.writeDelimitedString(str: String) {
	write(str.encodeToByteArray())
	writeByte(0) // null terminator
}

internal fun BufferedSource.readDelimitedString(): String {
	val bytes = mutableListOf<Byte>()
	while (true) {
		if (exhausted()) break
		val byte = readByte()
		if (byte == 0.toByte()) break
		bytes.add(byte)
	}
	return bytes.toByteArray().decodeToString()
}