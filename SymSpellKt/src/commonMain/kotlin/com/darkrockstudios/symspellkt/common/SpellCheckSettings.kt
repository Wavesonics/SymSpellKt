package com.darkrockstudios.symspellkt.common

/**
 * SpellCheckSetting contains all the setting used by [SpellChecker]
 */
data class SpellCheckSettings(
	/**
	 * Default verbosity [Verbosity]
	 * (default Closest)
	 */
	val verbosity: Verbosity = Verbosity.Closest,

	/**
	 * limit suggestion list to topK entries
	 * limits result to n entries
	 */
	val topK: Int = 10,

	/**
	 * true if the spellchecker should lowercase terms
	 */
	val lowerCaseTerms: Boolean = true,

	/**
	 * Maximum edit distance for doing lookups.
	 * (default 2.0)
	 */
	val maxEditDistance: Double = 2.0,

	/**
	 * The length of word prefixes used for spell checking.
	 * (default 7)
	 */
	val prefixLength: Int = 7,

	/**
	 * The minimum frequency count for dictionary words to be considered correct spellings.
	 * (default 1)
	 */
	val countThreshold: Long = 1,

	/**
	 * Max keywordLength;
	 */
	var maxLength: Int = Int.MAX_VALUE,

	var bigramCountMin: Double = Double.MAX_VALUE,

	/**
	 * Ignore the word in result set, if suggestions are empty
	 */
	val ignoreUnknown: Boolean = true,

	/**
	 * Edit Factor to compute max edit distance possible for a word.
	 */
	val editFactor: Double = 0.3,

	/**
	 * As a performance optimization we prefer a quick distance calculation for small maxEditDistances.
	 * This could lead in some edgecases to a wrong result, but in most cases it leads to better performance.
	 */
	val preferQuickDistanceOnSmallEditDistance: Boolean = true,

	val doKeySplit: Boolean = true,

	val keySplitRegex: Regex = "\\s+".toRegex(),
)
