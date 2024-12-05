package performance

import com.darkrockstudios.fdic.FrequencyDictionary
import com.darkrockstudios.fdic.FrequencyDictionaryIO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.junit.Before
import org.junit.Test
import performance.strategies.fdicGzFile
import performance.strategies.loadGzDict
import performance.strategies.loadTxtDict
import performance.strategies.readAndStoreGzDict
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class PerformanceTests {

	// Install the test dictionary
	private fun copyTestDictionary() {
		val input = FileSystem.RESOURCES.source("en-80k.txt".toPath()).buffer()
		FileSystem.SYSTEM.sink("en-80k.txt".toPath()).buffer().writeUtf8(input.readUtf8())
	}

	// Delete the test articles that were generated
	private fun cleanupTestDictionaries() {
		FileSystem.SYSTEM.apply {
			delete("en-80k.txt".toPath())
			delete("en-80k.gz".toPath())
			delete("en-80k.fdic".toPath())
		}
	}

	@BeforeTest
	fun setup() {
		copyTestDictionary()
	}

	@AfterTest
	fun teardown() {
		cleanupTestDictionaries()
	}

	@Test
	fun comparisonTest() = runTest {
		////////////////////////////////////////////////////////////////
		// Txt
		// Load the frequency dictionary from the file "en-80.txt".
		val fdicTextFile = File("en-80k.txt")

		val frequencyDict: Map<String, Long>
		val textMs = measureTimeMillis {
			frequencyDict = loadTxtDict()
		}

		////////////////////////////////////////////////////////////////
		// GZ
		readAndStoreGzDict()
		val frequencyDictGz: Map<String, Long>
		val gzMs = measureTimeMillis {
			frequencyDictGz = loadGzDict()
		}

		////////////////////////////////////////////////////////////////
		// FDIC
		val fdicPath = "en-80k.fdic"
		val fdicFile = File(fdicPath)
		val freqDic = FrequencyDictionary(
			ngrams = 1,
			termCount = frequencyDict.size,
			locale = Locale.ENGLISH.toLanguageTag(),
		)
		frequencyDict.forEach { (term, frequency) -> freqDic.terms[term] = frequency }
		FrequencyDictionaryIO.writeFdic(freqDic, fdicPath)

		val fdictLoaded: Map<String, Long>
		val fdic2Ms = measureTimeMillis {
			fdictLoaded = FrequencyDictionaryIO.readFdic(fdicPath).terms
		}

		////////////////////////////////////////////////////////////////
		// Results

		println("Binary Frequency Dictionary Compression")
		println("---------------------------------------")
		println()
		println("Speed:")
		println("------")
		println(" txt took ${textMs}ms to load")
		println("  gz took ${gzMs}ms to load")
		println("fdi2 took ${fdic2Ms}ms to load")

		println()
		println("Summary:")
		if(textMs < fdic2Ms) {
			val speedPercent = 1.0 - (textMs.toDouble() / fdic2Ms)
			println("fdic was ${fdic2Ms - textMs}ms (${percent(speedPercent)}) slower")
		} else {
			val speedPercent = 1.0 - (fdic2Ms / textMs.toDouble())
			println("fdic was ${textMs - fdic2Ms}ms (${percent(speedPercent)}) faster!")
		}

		println()
		println("Compression:")
		println("------------")

		println(" txt size: ${fdicTextFile.length() / 1024} KB")
		println("  gz size: ${fdicGzFile.length() / 1024} KB")
		println("fdic size: ${fdicFile.length() / 1024} KB")

		println()
		println("Summary:")
		val percentOfOriginalSize = (fdicFile.length().toDouble() / fdicTextFile.length().toDouble())
		println("fdic was " + percent(percentOfOriginalSize) + " of the original size")

		val reduction = 1.0 - percentOfOriginalSize
		println("a reduction of ${percent(reduction)} (${fdicTextFile.length() - fdicFile.length()} B)")

		if(fdictLoaded.size != frequencyDict.size) error("Wrong size ${frequencyDict.size} vs ${fdictLoaded.size}")
		frequencyDict.forEach { (term, frequency) ->
			if(fdictLoaded[term] != frequency) error("Wrong frequency for ${term}: $frequency vs ${fdictLoaded[term]}")
		}

		println()
		println("----")
		println("fdict is valid")
	}

	private fun percent(percent: Double): String {
		return "%.2f%%".format(percent * 100)
	}
}