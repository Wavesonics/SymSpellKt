package com.darkrockstudios.symspellkt.common.stringdistance

import com.darkrockstudios.symspellkt.api.StringDistance
import kotlin.math.min

/**
 * A pure Levenshtein Distance implementation.
 */
class LevenshteinDistance : StringDistance {
	override fun getDistance(lhs: String, rhs: String): Double {
		if (lhs == rhs) {
			return 0.0
		}
		if (lhs.isEmpty()) {
			return rhs.length.toDouble()
		}
		if (rhs.isEmpty()) {
			return lhs.length.toDouble()
		}

		val lhsLength = lhs.length + 1
		val rhsLength = rhs.length + 1

		var cost = Array(lhsLength) { it.toDouble() }
		var newCost = Array(lhsLength) { 0.0 }

		for (i in 1..<rhsLength) {
			newCost[0] = i.toDouble()

			for (j in 1..<lhsLength) {
				val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

				val costReplace = cost[j - 1] + match
				val costInsert = cost[j] + 1
				val costDelete = newCost[j - 1] + 1

				newCost[j] = min(min(costInsert, costDelete), costReplace)
			}

			val swap = cost
			cost = newCost
			newCost = swap
		}

		return cost[lhsLength - 1]
	}

	override fun getDistance(w1: String, w2: String, maxEditDistance: Double): Double {
		val distance = getDistance(w1, w2)
		if (distance > maxEditDistance) {
			return -1.0
		}
		return distance
	}
}