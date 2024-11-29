package com.darkrockstudios.fdic

import okio.FileSystem
import okio.Path
import okio.GzipSink
import okio.GzipSource

class FrequencyDictionaryEncoder(private val fileSystem: FileSystem) {

	fun writeFdic2Kmp(dictionary: FrequencyDictionary, path: Path) {
		// Delete existing file if it exists
		fileSystem.delete(path, mustExist = false)

		GzipSink(fileSystem.sink(path)).buffer().use { byteStream ->
			dictionary.terms.forEach { (term, frequency) ->
				byteStream.writeVariableLong(frequency)
				byteStream.writeString(term)
			}
		}
	}

	fun readFdic2Kmp(path: Path): FrequencyDictionary {
		val frequencyDict = mutableMapOf<String, Long>()

		// Read compressed file
		GzipSource(fileSystem.source(path)).buffer().use { buffer ->
			// Read dictionary entries
			while (!buffer.exhausted()) {
				val frequency = buffer.readVariableLong()
				val term = buffer.readString()
				frequencyDict[term] = frequency
			}
		}

		return FrequencyDictionary(frequencyDict)
	}

}