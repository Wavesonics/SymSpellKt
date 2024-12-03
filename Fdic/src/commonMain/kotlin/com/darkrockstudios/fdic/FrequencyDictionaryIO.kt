package com.darkrockstudios.fdic

expect object FrequencyDictionaryIO {
	suspend fun writeFdic(dictionary: FrequencyDictionary, path: String)
	suspend fun readFdic(path: String): FrequencyDictionary
	suspend fun readFdic(bytes: ByteArray): FrequencyDictionary
}