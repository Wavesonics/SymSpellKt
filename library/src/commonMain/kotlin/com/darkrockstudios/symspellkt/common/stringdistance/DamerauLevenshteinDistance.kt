package com.darkrockstudios.symspellkt.common.stringdistance

import com.darkrockstudios.symspellkt.api.StringDistance

/**
 * DamerauLevenshteinDistance is a string metric for measuring the edit distance between two
 * sequences. Informally, the Damerau–Levenshtein distance between two words is the minimum number
 * of operations (consisting of insertions, deletions or substitutions of a single character, or
 * transposition of two adjacent characters) required to change one word into the other.
 */
class DamerauLevenshteinDistance : StringDistance {

	override fun getDistance(w1: String, w2: String): Double {
		if (w1 == w2) {
			return 0.0
		}

		if (w1.isEmpty()) {
			return w2.length.toDouble()
		}

		if (w2.isEmpty()) {
			return w1.length.toDouble()
		}

		val d = Array(w2.length + 1) {
			DoubleArray(
				w1.length + 1
			)
		} // 2d matrix

		// Step 2
		for (i in w2.length downTo 0) {
			d[i][0] = i * INSERTION // Add insertion weight
		}
		for (j in w1.length downTo 0) {
			d[0][j] = j * DELETION
		}

		for (i in 1..w2.length) {
			val target_i = w2[i - 1]
			for (j in 1..w1.length) {
				val source_j = w1[j - 1]

				val cost = getReplaceCost(target_i, source_j)

				var min = min(
					d[i - 1][j] + INSERTION,  //Insertion
					d[i][j - 1] + DELETION,  //Deltion
					d[i - 1][j - 1] + cost
				) //Replacement
				if (isTransposition(i, j, w1, w2)) {
					min = kotlin.math.min(min, d[i - 2][j - 2] + TRANSPORTATION) // transpose
				}
				d[i][j] = min
			}
		}
		return d[w2.length][w1.length]
	}

	override fun getDistance(w1: String, w2: String, maxEditDistance: Double): Double {
		val distance = getDistance(w1, w2)
		if (distance > maxEditDistance) {
			return -1.0
		}
		return distance
	}

	private fun min(a: Double, b: Double, c: Double): Double {
		return kotlin.math.min(a, kotlin.math.min(b, c))
	}

	private fun isTransposition(i: Int, j: Int, source: String?, target: String?): Boolean {
		return i > 2
				&& j > 2
				&& source!![j - 2] == target!![i - 1]
				&& target[i - 2] == source[j - 1]
	}

	private fun getReplaceCost(aI: Char, bJ: Char): Double {
		return if (aI != bJ) {
			REPLACEMENT
		} else {
			0.0
		}
	}

	companion object {
		private const val DELETION: Double = 1.0
		private const val INSERTION: Double = 1.0
		private const val REPLACEMENT: Double = 1.0
		private const val TRANSPORTATION: Double = 1.0
	}
}
