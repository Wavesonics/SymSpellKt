package com.darkrockstudios.symspellkt.common

data class SuggestionItem(
	/**
	 * The suggested correctly spelled word.
	 */
	val term: String,
	/**
	 * Edit distance between searched for word and suggestion.
	 */
	var distance: Double,
	/**
	 * Frequency of suggestion in the dictionary (a measure of how common the word is).
	 */
	val frequency: Double
) : Comparator<SuggestionItem?>, Comparable<SuggestionItem?> {
	/**
	 * Comparison to use in Sorting: Prefernce given to distance, and if distance is same then count
	 */
	override fun compareTo(other: SuggestionItem?): Int {
		other ?: error("TODO how to handle null?")
		if (SpellHelper.isEqualDouble(this.distance, other.distance)) {
			return doubleCompare(other.frequency, this.frequency)
		}
		return doubleCompare(this.distance, other.distance)
	}

	override fun compare(a: SuggestionItem?, b: SuggestionItem?): Int {
		return a?.compareTo(b) ?: error("TODO how to handle null?")
	}

	private fun doubleCompare(x: Double, y: Double): Int {
		if (x.isNaN())
			return if(y.isNaN()) 0 else 1
		if (y.isNaN())
			return -1
		if (x == 0.0 && y == 0.0)
			return (1 / x - 1 / y).toInt()
		if (x == y)
			return 0

		return if(x > y) 1 else -1
	}
}
