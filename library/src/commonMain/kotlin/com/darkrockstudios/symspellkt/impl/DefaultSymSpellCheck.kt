package com.darkrockstudios.symspellkt.impl

import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.common.stringdistance.DamerauLevenshteinDistance

fun createSymSpellChecker(
	settings: SpellCheckSettings? = null,
): SpellChecker {
	val spellCheckSettings = settings ?: SpellCheckSettings(
		countThreshold = 1,
		maxEditDistance = 2.0,
		topK = 5,
		prefixLength = 10,
		verbosity = Verbosity.ALL,
	)

	val damerauLevenshteinDistance = DamerauLevenshteinDistance()
	val dataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())

	val symSpellCheck = SymSpellCheck(
		dataHolder,
		damerauLevenshteinDistance,
		spellCheckSettings
	)

	return symSpellCheck
}