package com.darkrockstudios.symspellkt.sample

actual fun ByteArray.decodeToString(): String = this.decodeToString(
	startIndex = 0
)
actual fun String.splitLines(): List<String> {
	return split("\r\n", "\n", "\r")
}