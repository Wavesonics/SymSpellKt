package com.darkrockstudios.symspellkt.impl

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.common.DictionaryItem

private val splitRegex = "\\s+".toRegex()

fun DictionaryHolder.loadUniGramLine(line: String) {
	val arr = line.split(splitRegex)
	if (arr.size > 1) {
		addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
	}
}

fun DictionaryHolder.loadBiGramLine(line: String) {
	val arr = line.split(splitRegex)
	if (arr.size > 2) {
		addItem(DictionaryItem(arr[0] + " " + arr[1], arr[2].toDouble(), -1.0))
	}
}