package com.darkrockstudios.symspellkt.sample

import kotlinx.coroutines.*

expect fun ByteArray.decodeToString(): String

suspend fun <T> Sequence<T>.parallelForEach(block: (T) -> Unit) {
	coroutineScope {
		map { item ->
			async { block(item) }
		}
	}.forEach { it.join() }
}