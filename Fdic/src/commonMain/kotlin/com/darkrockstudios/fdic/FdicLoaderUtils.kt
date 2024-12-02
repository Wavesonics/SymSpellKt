package com.darkrockstudios.fdic

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.common.DictionaryItem
import okio.Path

fun DictionaryHolder.loadUniGramFile(path: Path) {
	val encoder = FrequencyDictionaryEncoder()
	val dictionary = encoder.readFdic2Kmp(path)
	dictionary.terms.forEach { (term, frequency) ->
		addItem(DictionaryItem(term, frequency.toDouble(), -1.0))
	}
}

fun DictionaryHolder.loadBiGramFile(path: Path) {
	val encoder = FrequencyDictionaryEncoder()
	val dictionary = encoder.readFdic2Kmp(path)
	dictionary.terms.forEach { (term, frequency) ->
		addItem(DictionaryItem(term, frequency.toDouble(), -1.0))
	}
}