package com.darkrockstudios.symspellkt.sample

import java.nio.charset.Charset

actual fun ByteArray.decodeToString(): String = this.toString(charset = Charset.defaultCharset())
actual fun String.splitLines(): List<String> {
	return split("\r\n", "\n", "\r")
}