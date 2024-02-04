package com.darkrockstudios.symspellkt.common

import com.darkrockstudios.symspellkt.api.CharDistance
import com.darkrockstudios.symspellkt.api.StringDistance

/**
 * DamerauLevenshteinDistance is a string metric for measuring the edit distance between two
 * sequences. Informally, the Damerau–Levenshtein distance between two words is the minimum number
 * of operations (consisting of insertions, deletions or substitutions of a single character, or
 * transposition of two adjacent characters) required to change one word into the other.
 *
 * In this variant of DamerauLevenshteinDistance, it has different weights associated to each
 * action.
 */
class WeightedDamerauLevenshteinDistance(
	deletionWeight: Double,
	insertionWeight: Double,
	replaceWeight: Double,
	transpositionWeight: Double,
	charDistance: CharDistance?
) : StringDistance {
	// Damerau function variables
	private var deletionWeight = 0.8

	private var insertionWeight = 1.01

	private var replaceWeight = 0.9

	private var transpositionWeight = 0.7

	private val charDistance: CharDistance?

	/**
	 * Constructor for Weighted Damerau Levenshtein
	 * @param deletionWeight
	 * @param insertionWeight
	 * @param replaceWeight
	 * @param transpositionWeight
	 * @param charDistance
	 */
	init {
		this.deletionWeight = deletionWeight
		this.insertionWeight = insertionWeight
		this.replaceWeight = replaceWeight
		this.transpositionWeight = transpositionWeight
		this.charDistance = charDistance
	}

	override fun getDistance(source: String, target: String): Double {
		if (source == target) {
			return 0.0
		}

		if (source.isEmpty()) {
			return target.length.toDouble()
		}

		if (target.isEmpty()) {
			return source.length.toDouble()
		}

		val useCharDistance = (charDistance != null && source.length == target.length)
		val d = Array(target.length + 1) {
			DoubleArray(
				source.length + 1
			)
		} // 2d matrix

		// Step 2
		for (i in target.length downTo 0) {
			d[i][0] = i * insertionWeight // Add insertion weight
		}
		for (j in source.length downTo 0) {
			d[0][j] = j * deletionWeight
		}

		for (i in 1..target.length) {
			val target_i = target[i - 1]
			for (j in 1..source.length) {
				val source_j = source[j - 1]

				val cost = getReplaceCost(target_i, source_j, useCharDistance)

				var min = min(
					d[i - 1][j] + insertionWeight,  //Insertion
					d[i][j - 1] + deletionWeight,  //Deltion
					d[i - 1][j - 1] + cost
				) //Replacement
				if (isTransposition(i, j, source, target)) {
					min = kotlin.math.min(min, d[i - 2][j - 2] + transpositionWeight) // transpose
				}
				d[i][j] = min
			}
		}
		return d[target.length][source.length]
	}

	override fun getDistance(w1: String, w2: String, maxEditDistance: Double): Double {
		val distance = getDistance(w1, w2)
		if (distance > maxEditDistance) {
			return (-1).toDouble()
		}
		return distance
	}

	private fun min(a: Double, b: Double, c: Double): Double {
		return kotlin.math.min(a, kotlin.math.min(b, c))
	}

	private fun min(a: Double, b: Double, c: Double, d: Double): Double {
		return kotlin.math.min(a, kotlin.math.min(b, kotlin.math.min(c, d)))
	}


	private fun isTransposition(i: Int, j: Int, source: String?, target: String?): Boolean {
		return i > 2 && j > 2 && source!![j - 2] == target!![i - 1] && target[i - 2] == source[j - 1]
	}

	private fun getReplaceCost(aI: Char, bJ: Char, useCharDistance: Boolean): Double {
		return if (aI != bJ && useCharDistance) {
			replaceWeight * charDistance!!.distance(aI, bJ)
		} else if (aI != bJ) {
			replaceWeight
		} else {
			0.0
		}
	}
}
