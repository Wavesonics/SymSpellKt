package com.darkrockstudios.fdic

import korlibs.io.async.useIt
import korlibs.io.compression.compress
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.stream.openFastStream
import korlibs.memory.ByteArrayBuilder

actual object FrequencyDictionaryIO {

	actual suspend fun writeFdic(dictionary: FrequencyDictionary, path: String) {

		// Delete existing file if it exists
		val file = localCurrentDirVfs[path.toString()]
		file.delete()

		val builder = ByteArrayBuilder()
		dictionary.terms.forEach { (term, frequency) ->
			builder.writeVariableLong(frequency)
			builder.writeStringKmp(term)
		}

		val uncompressedBytes = builder.toByteArray()
		val compressedBytes = uncompressedBytes.compress(GZIP)

		// Write the compressed bytes to file
		file.open(VfsOpenMode.CREATE_OR_TRUNCATE).useIt { outStream ->
			outStream.write(compressedBytes)
		}
	}

	actual suspend fun readFdic(path: String): FrequencyDictionary {
		val file = localCurrentDirVfs[path.toString()]
		val bytes = file.read()
		return readFdic(bytes)
	}

	actual suspend fun readFdic(bytes: ByteArray): FrequencyDictionary {
		val frequencyDict = mutableMapOf<String, Long>()

		val inputStream = bytes.uncompress(GZIP).openFastStream()
		// Read dictionary entries
		while (inputStream.available > 0) {
			val frequency = inputStream.readVariableLong()
			val term = inputStream.readDelimitedString()
			frequencyDict[term] = frequency
		}

		return FrequencyDictionary(frequencyDict)
	}
}