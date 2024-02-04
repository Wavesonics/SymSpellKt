package com.darkrockstudios.symspellkt.common

/**
 * Controls the closeness/quantity of returned spelling suggestion
 */
enum class Verbosity {
	/**
	 * Top suggestion with the highest term frequency of the suggestions of smallest edit distance
	 * found.
	 */
	TOP,

	/**
	 * All suggestions of smallest edit distance found, suggestions ordered by term frequency.
	 */
	CLOSEST,

	/**
	 * All suggestions within maxEditDistance, suggestions ordered by edit distance,
	 * then by term frequency (slower, no early termination).
	 */
	ALL
}
