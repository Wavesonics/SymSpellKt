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
	val count: Double
) : Comparator<SuggestionItem?>, Comparable<SuggestionItem?> {
	/**
	 * final similarity
	 */
	private val score = 0.0

	/**
	 * Comparison to use in Sorting: Prefernce given to distance, and if distance is same then count
	 */
	override fun compareTo(other: SuggestionItem?): Int {
		// TODO had to deal with nullability here, returning -1 and Double.NaN in
		// null cases, not sure if that is correct, or if it matters at all
		return if (SpellHelper.isEqualDouble(this.distance, other?.distance ?: Double.NaN, 0.001)) {
			other?.count?.compareTo(this.count) ?: -1
		} else {
			this.distance.compareTo(other?.distance ?: Double.NaN)
		}
	}

	override fun compare(a: SuggestionItem?, b: SuggestionItem?): Int {
		return a?.compareTo(b) ?: -1
	}
}
