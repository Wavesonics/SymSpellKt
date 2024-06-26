package com.darkrockstudios.symspellkt.api

import com.darkrockstudios.symspellkt.common.Composition
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.exception.SpellCheckException

/**
 * Abstract class for the Spell Correction
 */
abstract class SpellChecker(
	val dictionary: DictionaryHolder,
	protected val stringDistance: StringDistance,
	protected val spellCheckSettings: SpellCheckSettings,
) {
	@Throws(SpellCheckException::class)
	abstract fun lookup(
		word: String,
		verbosity: Verbosity = spellCheckSettings.verbosity,
		editDistance: Double = spellCheckSettings.maxEditDistance,
	): List<SuggestionItem>

	@Throws(SpellCheckException::class)
	abstract fun lookupCompound(
		word: String,
		editDistance: Double = spellCheckSettings.maxEditDistance,
		tokenizeOnWhiteSpace: Boolean = true,
	): List<SuggestionItem>

	@Throws(SpellCheckException::class)
	abstract fun wordBreakSegmentation(
		phrase: String,
		maxSegmentationWordLength: Int = spellCheckSettings.prefixLength,
		maxEditDistance: Double = spellCheckSettings.maxEditDistance,
	): Composition

	abstract fun createDictionaryEntry(word: String, frequency: Int): Boolean
	abstract fun createDictionaryEntry(word: String, frequency: Double): Boolean
}
