package com.darkrockstudios.symspellkt.common

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

object SpellHelper {
	/**
	 * Generate delete string set  for the Key.
	 */
	fun getEditDeletes(
		key: String,
		maxEditDistance: Double,
		prefixLength: Int,
		editFactor: Double,
	): Set<String> {
		val deletedWords: MutableSet<String> = mutableSetOf()
		if (key.length <= maxEditDistance) {
			deletedWords.add("")
		}
		if (key.length > maxEditDistance) {
			deletedWords
				.add(
					key.substring(
						0,
						if (prefixLength < key.length) prefixLength else key.length
					)
				)
		}
		return edits(key, 0.0, deletedWords, getEdistance(maxEditDistance, key.length, editFactor))
	}

	private fun getEdistance(maxEditDistance: Double, length: Int, factor: Double): Double {
		val computedEd: Double = (factor * length).roundToLong().toDouble()
		if (min(maxEditDistance, computedEd) == maxEditDistance) {
			return maxEditDistance
		}
		return computedEd
	}

	/**
	 * Inexpensive and language independent: only deletes, no transposes + replaces + inserts replaces
	 * and inserts are expensive and language dependent
	 */
	fun edits(
		word: String,
		editDistance: Double,
		deletedWords: MutableSet<String>,
		maxEd: Double,
	): Set<String> {
		var runningEditDistance = editDistance
		runningEditDistance++
		if (word.isEmpty()) {
			return deletedWords
		}

		for (i in word.indices) {
			val delete = word.substring(0, i) + word.substring(i + 1, word.length)
			if (deletedWords.add(delete) && runningEditDistance < maxEd) {
				edits(delete, runningEditDistance, deletedWords, maxEd)
			}
		}
		deletedWords.add(word)
		return deletedWords
	}

	/**
	 * Early exit method
	 */
	fun earlyExit(
		suggestionItems: MutableList<SuggestionItem>,
		phrase: String?,
		maxEditDistance: Double,
		ignoreUnknown: Boolean
	): MutableList<SuggestionItem> {
		if (suggestionItems.isEmpty() && !ignoreUnknown && phrase != null) {
			suggestionItems.add(SuggestionItem(phrase, maxEditDistance + 1.0, 0.0))
		}
		return suggestionItems
	}

	fun tokenizeOnWhiteSpace(word: String): Array<String> {
		return word.split("\\s+".toRegex()).toTypedArray()
	}

	fun isLessOrEqualDouble(d1: Double, d2: Double, threshold: Double = EPSILON): Boolean {
		return abs(d1 - d2) < threshold || d1 < d2
	}

	fun isLessDouble(d1: Double, d2: Double, threshold: Double = EPSILON): Boolean {
		return !isEqualDouble(d1, d2, threshold) && d1 < d2
	}


	fun isEqualDouble(d1: Double, d2: Double, threshold: Double = EPSILON): Boolean {
		return abs(d1 - d2) < threshold
	}

	/**
	 * Check if heads  are same
	 * @param suggestions
	 * @param suggestions1
	 * @return boolean
	 */
	fun continueConditionIfHeadIsSame(
		suggestions: List<SuggestionItem>,
		suggestions1: List<SuggestionItem?>
	): Boolean {
		return (
				suggestions1.isEmpty()
						|| (suggestions.isNotEmpty()
						&& suggestions1.isNotEmpty()
						&& suggestions[0] == suggestions1[0])
				)
	}

	private const val EPSILON = 0.02
}
