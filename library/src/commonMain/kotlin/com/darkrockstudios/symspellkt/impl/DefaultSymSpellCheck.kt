package com.darkrockstudios.symspellkt.impl

import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.common.WeightedDamerauLevenshteinDistance

fun createSymSpellChecker(
	settings: SpellCheckSettings? = null,
): SpellChecker {
	val spellCheckSettings = settings ?: SpellCheckSettings(
		countThreshold = 1,
		deletionWeight = 1.0,
		insertionWeight = 1.0,
		replaceWeight = 1.0,
		maxEditDistance = 2.0,
		transpositionWeight = 1.0,
		topK = 5,
		prefixLength = 10,
		verbosity = Verbosity.ALL,
	)

	val weightedDamerauLevenshteinDistance =
		WeightedDamerauLevenshteinDistance(
			spellCheckSettings.deletionWeight,
			spellCheckSettings.insertionWeight,
			spellCheckSettings.replaceWeight,
			spellCheckSettings.transpositionWeight,
		)
	val dataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())

	val symSpellCheck = SymSpellCheck(
		dataHolder,
		weightedDamerauLevenshteinDistance,
		spellCheckSettings
	)

	return symSpellCheck
}