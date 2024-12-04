package com.darkrockstudios.fdic

internal fun ByteArray.toInt(): Int =
	(this[0].toInt() and 0xFF shl 24) or
			(this[1].toInt() and 0xFF shl 16) or
			(this[2].toInt() and 0xFF shl 8) or
			(this[3].toInt() and 0xFF)

internal fun ByteArray.toHex(): String {
	val hexChars = "0123456789ABCDEF"
	val result = StringBuilder(size * 2)
	forEach { byte ->
		val i = byte.toInt() and 0xFF
		result.append(hexChars[i shr 4])
		result.append(hexChars[i and 0x0F])
	}
	return result.toString()
}

internal fun Int.toByteArray(): ByteArray = byteArrayOf(
	(this shr 24).toByte(),
	(this shr 16).toByte(),
	(this shr 8).toByte(),
	this.toByte()
)