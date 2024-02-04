package com.darkrockstudios.symspellkt.api

interface CharDistance {
	/**
	 * returns a value between 0 and 1 (both inclusive). 0 for no distance and 1 for maximum
	 * distance.
	 *
	 * @return distance value
	 */
	fun distance(a: Char, b: Char): Double
}
