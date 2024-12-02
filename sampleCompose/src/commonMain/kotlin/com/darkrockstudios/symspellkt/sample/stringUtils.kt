package com.darkrockstudios.symspellkt.sample

expect fun ByteArray.decodeToString(): String

/*
suspend fun <T> Sequence<T>.parallelForEach(block: (T) -> Unit) {
	coroutineScope {
		map { item ->
			async { block(item) }
		}.forEach { it.join() }
	}
}
*/

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
