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
		var newKey = key
		if (key.length <= maxEditDistance) {
			deletedWords.add("")
		}
		if (key.length > maxEditDistance) {
			newKey = key.substring(0, if (prefixLength < key.length) prefixLength else key.length)
			deletedWords.add(newKey)
		}
		return edits(newKey, 0.0, deletedWords, getEdistance(maxEditDistance, newKey.length, editFactor))
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
		topK: Int,
		ignoreUnknown: Boolean,
	): MutableList<SuggestionItem> {
		if (suggestionItems.isEmpty() && !ignoreUnknown && phrase != null) {
			val si = SuggestionItem(phrase, maxEditDistance + 1.0, 0.0)
			suggestionItems.addItemSorted(si, topK)
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

	const val EPSILON = 0.011
}

/**
 * Add $item to the proper position in the list, such that the list
 * remains sorted. Removing an item from the list if necessary in order
 * to stay within the $maxSize.
 * 	 * @param item The item to add to the list
 * 	 * @param maxSize The maximum size of the list to not exceed
 * 	 * @return this list for chaining
 */
fun <T: Comparable<T>> MutableList<T>.addItemSorted(item: T, maxSize: Int): MutableList<T> {
	if (size >= maxSize) {
		val lastItem = this[maxSize - 1]
		if (lastItem <= item) {
			return this
		} else {
			removeAt(maxSize - 1)
		}
	}
	var index = binarySearch(item)
	if (index < 0) index = -index - 1
	add(index, item)
	return this
}