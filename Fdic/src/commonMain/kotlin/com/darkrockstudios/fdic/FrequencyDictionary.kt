package com.darkrockstudios.fdic

data class FrequencyDictionary(
	val terms: MutableMap<String, Long> = mutableMapOf<String, Long>()
)
