package com.darkrockstudios.symspellkt.api

interface StringDistance {
	/**
	 * Get edit distance between 2 words;
	 *
	 * @param w1 String
	 * @param w2 String
	 * @return edit distance
	 */
	fun getDistance(w1: String, w2: String): Double

	/**
	 * Get edit distance between 2 words, if the calculated distance exceeds max provided then return
	 * maxEditDistance;
	 *
	 * @param w1 String
	 * @param w2 String
	 * @param maxEditDistance max edit distance possible
	 * @return edit distance
	 */
	fun getDistance(w1: String, w2: String, maxEditDistance: Double): Double
}
