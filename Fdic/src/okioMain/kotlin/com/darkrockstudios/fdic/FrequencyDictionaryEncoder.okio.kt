package com.darkrockstudios.fdic

import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.FORMAT_VERSION
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.FORMAT_VERSION_SIZE
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.MAGIC_WORD
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.MAGIC_WORD_SIZE
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.UNCOMPRESSED_HEADER_SIZE
import okio.*
import okio.ByteString.Companion.toByteString
import okio.Path.Companion.toPath

actual object FrequencyDictionaryIO {

	private val fileSystem: FileSystem = FileSystem.SYSTEM

	actual suspend fun writeFdic(dictionary: FrequencyDictionary, path: String) {
		val okPath = path.toPath()
		writeFdic(dictionary, okPath)
	}

	suspend fun writeFdic(dictionary: FrequencyDictionary, path: Path) {
		// Delete existing file if it exists
		fileSystem.delete(path, mustExist = false)

		// Write header outside of compressed area
		val uncompressedSink = fileSystem.sink(path).buffer()
		uncompressedSink.write(FrequencyDictionaryFileFormat.magicWordBytes())
		uncompressedSink.write(FrequencyDictionaryFileFormat.formatVersion())

		GzipSink(uncompressedSink).buffer().use { byteStream ->
			byteStream.writeVariableLong(dictionary.ngrams.toLong())
			byteStream.writeVariableLong(dictionary.termCount.toLong())
			byteStream.writeString(dictionary.locale)

			dictionary.terms.forEach { (term, frequency) ->
				byteStream.writeVariableLong(frequency)
				byteStream.writeString(term)
			}
		}
	}

	actual suspend fun readFdic(path: String): FrequencyDictionary {
		return readFdic(path.toPath())
	}

	suspend fun readFdic(path: Path): FrequencyDictionary {
		return readFdic2Kmp(fileSystem.source(path))
	}

	actual suspend fun readFdic(bytes: ByteArray): FrequencyDictionary {
		val buffer = Buffer().apply {
			write(bytes)
		}
		return readFdic2Kmp(buffer)
	}

	fun readFdic2Kmp(inputSource: Source): FrequencyDictionary {
		val frequencyDict = mutableMapOf<String, Long>()

		// Read the uncompressed header
		val headerBuffer = Buffer()
		inputSource.read(headerBuffer, UNCOMPRESSED_HEADER_SIZE)

		// Validate the magic word
		val magicWordBytes = headerBuffer.readByteArray(MAGIC_WORD_SIZE)
		val magicWord = magicWordBytes.toInt()
		if (magicWord != MAGIC_WORD) {
			throw FdicFormatException(
				"Magic word mismatch: was ${
					magicWordBytes.toByteString().hex()
				} expected ${
					FrequencyDictionaryFileFormat.magicWordBytes().toByteString().hex()
				}. This is probably not an FDIC file."
			)
		}

		// Validate the format version
		val formatVersionBytes = headerBuffer.readByteArray(FORMAT_VERSION_SIZE)
		val formatVersion = formatVersionBytes[0]
		if (formatVersion != FORMAT_VERSION) {
			throw FdicFormatException("Format Version mismatch: was $formatVersion expected $FORMAT_VERSION")
		}

		var ngrams: Int = -1
		var termCount: Int = -1
		var locale: String = ""

		// Read compressed file
		GzipSource(inputSource).buffer().use { buffer ->
			ngrams = buffer.readVariableLong().toInt()
			termCount = buffer.readVariableLong().toInt()
			locale = buffer.readString()

			// Read dictionary entries
			while (!buffer.exhausted()) {
				val frequency = buffer.readVariableLong()
				val term = buffer.readString()
				frequencyDict[term] = frequency
			}
		}

		val dictionary = FrequencyDictionary(
			formatVersion = formatVersion.toInt(),
			ngrams = ngrams,
			locale = locale,
			termCount = frequencyDict.size,
			terms = frequencyDict,
		)
		dictionary.validate()
		return dictionary
	}
}

private fun ByteArray.toInt(): Int =
	(this[0].toInt() and 0xFF shl 24) or
			(this[1].toInt() and 0xFF shl 16) or
			(this[2].toInt() and 0xFF shl 8) or
			(this[3].toInt() and 0xFF)