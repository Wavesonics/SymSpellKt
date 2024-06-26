package com.darkrockstudios.symspellkt.common

/**
 * SpellCheckSetting contians all the setting used by [SpellChecker]
 */
data class SpellCheckSettings(
	/**
	 * Default verbosity [Verbosity]
	 */
	val verbosity: Verbosity = Verbosity.Closest,

	/**
	 * limit suggestion list to topK entries
	 */
	val topK: Int = 10, // limits result to n entries

	/**
	 * true if the spellchecker should lowercase terms
	 */
	val lowerCaseTerms: Boolean = true,

	/**
	 * Maximum edit distance for doing lookups. (default 2.0)
	 */
	val maxEditDistance: Double = 2.0,

	/**
	 * The length of word prefixes used for spell checking. (default 7)
	 */
	val prefixLength: Int = 7,

	/**
	 * The minimum frequency count for dictionary words to be considered correct spellings. (default
	 * 1)
	 */
	val countThreshold: Long = 1,

	/**
	 * Max keywordLength;
	 */
	var maxLength: Int = Int.MAX_VALUE,

	var bigramCountMin: Double = Double.MAX_VALUE,

	/**
	 * Ignore the word in resultset, if suggestions are empty
	 */
	val ignoreUnknown: Boolean = true,

	/**
	 * Edit Factor to compute max edit distance possible for a word.
	 */
	val editFactor: Double = 0.3,

	val doKeySplit: Boolean = true,

	val keySplitRegex: Regex = "\\s+".toRegex(),
)
