package com.darkrockstudios.symspell.fdic

import com.darkrockstudios.fdic.FrequencyDictionary
import com.darkrockstudios.fdic.FrequencyDictionaryIO
import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.common.DictionaryItem
import okio.Path

suspend fun DictionaryHolder.loadFdicFile(path: Path) {
	val encoder = FrequencyDictionaryIO()
	val dictionary = encoder.readFdic(path.toString())
	addAllToDictionary(dictionary)
}

suspend fun DictionaryHolder.loadFdicFile(byteArray: ByteArray) {
	val encoder = FrequencyDictionaryIO()
	val dictionary = encoder.readFdic(byteArray)
	addAllToDictionary(dictionary)
}

private fun DictionaryHolder.addAllToDictionary(dictionary: FrequencyDictionary) {
	dictionary.terms.entries.forEach { (term, frequency) ->
		addItem(DictionaryItem(term, frequency.toDouble(), -1.0))
	}
}