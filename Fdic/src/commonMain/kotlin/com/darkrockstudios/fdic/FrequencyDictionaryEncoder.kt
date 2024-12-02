package com.darkrockstudios.fdic

import okio.Path

expect class FrequencyDictionaryEncoder() {
	fun writeFdic2Kmp(dictionary: FrequencyDictionary, path: Path)
	fun readFdic2Kmp(path: Path): FrequencyDictionary
}