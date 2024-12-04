package com.darkrockstudios.symspellkt.api

import com.darkrockstudios.symspellkt.impl.loadBiGramLine
import com.darkrockstudios.symspellkt.impl.loadUniGramLine

suspend fun DictionaryHolder.loadUnigramTxtFile(dictionaryContents: String) {
	dictionaryContents
		.lineSequence()
		.forEachAsync { line ->
			loadUniGramLine(line)
		}
}

suspend fun DictionaryHolder.loadUnigramTxtFile(byteArray: ByteArray) {
	loadUnigramTxtFile(byteArray.decodeToString())
}

suspend fun DictionaryHolder.loadBigramTxtFile(dictionaryContents: String) {
	dictionaryContents
		.lineSequence()
		.forEachAsync { line ->
			loadBiGramLine(line)
		}
}

suspend fun DictionaryHolder.loadBigramTxtFile(byteArray: ByteArray) {
	loadBigramTxtFile(byteArray.decodeToString())
}

/**
 * This is here so we can yield, without this
 * WASM builds were deadlocking the UI thread.
 */
suspend fun <T> Sequence<T>.forEachAsync(
	action: suspend (T) -> Unit
) {
	for (item in this) {
		action(item)
	}
}
