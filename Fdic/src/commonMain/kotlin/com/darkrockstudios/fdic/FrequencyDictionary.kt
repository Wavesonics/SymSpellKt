package com.darkrockstudios.fdic

import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.FORMAT_VERSION

data class FrequencyDictionary(
	val formatVersion: Int = FORMAT_VERSION.toInt(),
	val ngrams: Int,
	val locale: String,
	val termCount: Int,
	val terms: MutableMap<String, Long> = mutableMapOf<String, Long>()
) {
	fun getNgramName(): String {
		return when (ngrams) {
			1 -> "Unigram"
			2 -> "Bigram"
			else -> error("Illegal ngram count: $ngrams")
		}
	}

	fun validate() {
		when {
			(ngrams != 1 && ngrams != 2) -> throw FdicValidationException("Invalid ngram size: $ngrams")
			(locale.isBlank() || locale.length > 32) -> throw FdicValidationException("Invalid locale length: $locale")
			termCount < 1 -> throw FdicValidationException("Invalid term count: $termCount")
		}
	}
}
