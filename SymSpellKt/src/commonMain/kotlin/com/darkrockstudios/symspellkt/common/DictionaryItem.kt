package com.darkrockstudios.symspellkt.common

/**
 * Dictionary Item class which holds the term, frequency and the edit distance
 */
data class DictionaryItem(
	var term: String,
	var frequency: Double,
	var distance: Double = -1.0,
)
