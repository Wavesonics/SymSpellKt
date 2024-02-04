package com.darkrockstudios.symspellkt.common

data class Composition(
	var segmentedString: String? = null,
	var correctedString: String? = null,
	var distanceSum: Double = 0.0,
	var logProbSum: Double = 0.0,
)
