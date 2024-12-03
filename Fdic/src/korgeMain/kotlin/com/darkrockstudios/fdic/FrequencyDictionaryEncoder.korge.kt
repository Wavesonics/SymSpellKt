package com.darkrockstudios.fdic

import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.FORMAT_VERSION
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.FORMAT_VERSION_SIZE
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.MAGIC_WORD
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.MAGIC_WORD_SIZE
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.UNCOMPRESSED_HEADER_SIZE
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.formatVersion
import com.darkrockstudios.fdic.FrequencyDictionaryFileFormat.magicWordBytes
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

		val uncompressedSection = ByteArrayBuilder()
		uncompressedSection.append(magicWordBytes())
		uncompressedSection.append(formatVersion())

		val bodySection = ByteArrayBuilder()
		bodySection.writeVariableLong(dictionary.ngrams.toLong())
		bodySection.writeVariableLong(dictionary.termCount.toLong())
		bodySection.writeDelimitedString(dictionary.locale)

		dictionary.terms.forEach { (term, frequency) ->
			bodySection.writeVariableLong(frequency)
			bodySection.writeDelimitedString(term)
		}

		val compressedBodyBytes = bodySection.toByteArray().compress(GZIP)

		file.open(VfsOpenMode.CREATE_OR_TRUNCATE).useIt { outStream ->
			outStream.write(uncompressedSection.toByteArray())
			outStream.write(compressedBodyBytes)
		}
	}

	actual suspend fun readFdic(path: String): FrequencyDictionary {
		val file = localCurrentDirVfs[path.toString()]
		val bytes = file.read()
		return readFdic(bytes)
	}

	actual suspend fun readFdic(bytes: ByteArray): FrequencyDictionary {
		val magicWordBytes = bytes.copyOfRange(0, MAGIC_WORD_SIZE.toInt())
		val magicWord = magicWordBytes.toInt()
		if (magicWord != MAGIC_WORD) {
			throw FdicFormatException(
				"Magic word mismatch: was ${
					magicWordBytes.toHex()
				} expected ${
					FrequencyDictionaryFileFormat.magicWordBytes().toHex()
				}. This is probably not an FDIC file."
			)
		}

		// Validate the format version
		val formatVersionBytes = bytes.copyOfRange(MAGIC_WORD_SIZE.toInt(), FORMAT_VERSION_SIZE.toInt())
		val formatVersion = formatVersionBytes[0]
		if (formatVersion != FORMAT_VERSION) {
			throw FdicFormatException("Format Version mismatch: was $formatVersion expected $FORMAT_VERSION")
		}

		val inputStream = bytes.uncompress(GZIP).openFastStream(offset = UNCOMPRESSED_HEADER_SIZE.toInt())

		var ngrams = inputStream.readVariableLong().toInt()
		var termCount = inputStream.readVariableLong().toInt()
		var locale = inputStream.readDelimitedString()

		val frequencyDict = HashMap<String, Long>(termCount)
		// Read dictionary entries
		while (inputStream.available > 0) {
			val frequency = inputStream.readVariableLong()
			val term = inputStream.readDelimitedString()
			frequencyDict[term] = frequency
		}

		return FrequencyDictionary(
			formatVersion = formatVersion.toInt(),
			ngrams = ngrams,
			locale = locale,
			termCount = termCount,
			terms = frequencyDict,
		)
	}
}
