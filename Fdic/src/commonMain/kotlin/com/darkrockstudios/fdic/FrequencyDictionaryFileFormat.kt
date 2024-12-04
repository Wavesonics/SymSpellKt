package com.darkrockstudios.fdic

object FrequencyDictionaryFileFormat {
	const val MAGIC_WORD = 0x0F0D010C
	const val FORMAT_VERSION: Byte = 0x01

	const val MAGIC_WORD_SIZE = 4L
	const val FORMAT_VERSION_SIZE = 1L
	const val UNCOMPRESSED_HEADER_SIZE: Long = MAGIC_WORD_SIZE + FORMAT_VERSION_SIZE

	fun magicWordBytes(): ByteArray = MAGIC_WORD.toByteArray()
	fun formatVersion(): ByteArray {
		val array = ByteArray(1)
		array[0] = FORMAT_VERSION
		return array
	}
}