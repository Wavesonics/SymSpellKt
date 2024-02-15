package com.darkrockstudios.symspellkt.sample

actual fun ByteArray.decodeToString(): String = this.decodeToString(
	startIndex = 0
)