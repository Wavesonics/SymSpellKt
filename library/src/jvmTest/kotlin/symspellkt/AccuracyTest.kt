package symspellkt

import com.darkrockstudios.symspellkt.api.CharDistance
import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import com.darkrockstudios.symspellkt.impl.SymSpellCheck
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import org.apache.commons.csv.*
import symspellkt.benchmark.StopWatch
import kotlin.test.Test


class AccuracyTest {
	@Throws(IOException::class, SpellCheckException::class)
	fun run(spellChecker: SpellChecker) {
		val queryResourceUrl = this.javaClass.classLoader.getResource(fullTestData)
		val parser: CSVParser = CSVParser
			.parse(
				queryResourceUrl, Charset.forName("UTF-8"),
				CSVFormat.DEFAULT.withDelimiter(':')
			)

		val tpCandidates: MutableMap<String, String> = HashMap()
		val fpCandidates: MutableMap<String, String> = HashMap()

		// index
		val stopWatch = StopWatch()
		stopWatch.start()
		var indexCount = 0
		val csvIterator: Iterator<CSVRecord> = parser.iterator()
		while (csvIterator.hasNext()) {
			// 0 = correct word
			// 1 = true if this is a desired match,
			// false if this is a false-positive match
			// 2 = comma separated list of similar word
			val csvRecord: CSVRecord = csvIterator.next()
			val match: Boolean = java.lang.Boolean.valueOf(csvRecord.get(1))
			if (match) {
				appendToList(tpCandidates, csvRecord)
			} else {
				if (csvRecord.get(1).equals(csvRecord.get(0))) {
					System.out.println((("WRONG: " + csvRecord.get(1)) + "," + csvRecord.get(0)).toString() + ",false")
				}
				appendToList(fpCandidates, csvRecord)
			}

			spellChecker.dataHolder.addItem(DictionaryItem(csvRecord.get(0), 1.0, 0.0))
			indexCount++
		}

		stopWatch.stop()
		val indexTime: Long = stopWatch.getTime()

		stopWatch.reset()
		stopWatch.start()

		// for each spellTestSetEntry do all searches
		var success = 0
		var fail = 0
		var truePositives = 0
		var trueNegatives = 0
		var falsePositives = 0
		var falseNegatives = 0
		var count = 0

		for (candidate in tpCandidates.entries) {
			val results: List<SuggestionItem> = spellChecker.lookupCompound(candidate.key)
			Collections.sort(results)
			// first or second match count as success
			if (isMatch(candidate, results)) {
				success++
				truePositives++
			} else {
				if (printFailures) {
					println(
						count.toString() + ": '" + candidate.value + "' not found by search for " + candidate
							.key
					)
					if (results.size > 0) {
						println(
							"found '" + results[0]
									+ (if (results.size > 1) "' and '" + results[1] else "")
									+ "' instead"
						)
					}
					println()
				}
				fail++
				falseNegatives++
			}
			count++
		}

		for (candidate in fpCandidates.entries) {
			val results: MutableList<SuggestionItem> = spellChecker.lookupCompound(candidate.key).toMutableList()
			Collections.sort(results)
			// first or second match count as success
			if (isMatch(candidate, results) && (candidate.key != results[0].term)) {
				fail++
				falsePositives++
				if (printFailures) {
					println(
						"false-positive: found '" + results[0] + "' by search for '" + candidate
							.key + "'"
					)
					if (results.size > 1 && acceptSecondHitAsSuccess) {
						println("              + found '" + results[1] + "' as well'")
					}
					println()
				}
			} else {
				success++
				trueNegatives++
			}
			count++
		}

		stopWatch.stop()

		println("indexed " + indexCount + " words in " + indexTime + "ms")
		println("$count searches")
		println(
			"${stopWatch.getTime()}ms => "
					+ String.format("%1$.3f searches/ms", (count.toDouble() / (stopWatch.getTime())))
		)
		println()
		println(
			success.toString() + " success / accuracy => " + String.format("%.2f%%", (100.0 * success / count))
		)
		println("$truePositives true-positives")
		println("$trueNegatives true-negatives (?)")
		println()
		println(fail.toString() + " fail => " + String.format("%.2f%%", (100.0 * fail / count)))
		println("$falseNegatives false-negatives")
		println("$falsePositives false-positives")
		println()
	}

	private fun appendToList(tpCandidates: MutableMap<String, String>, csvRecord: CSVRecord) {
		val targetWord: String = csvRecord.get(0)
		val variants: Array<String> = csvRecord.get(2).split(",").toTypedArray()
		for (variant in variants) {
			tpCandidates[variant] = targetWord
		}
	}

	@Test
	@Throws(IOException::class, SpellCheckException::class)
	fun testAccuracy() {
		val accuracyTest = AccuracyTest()

		println("=========  Basic =============================")
		//Basic
		var spellCheckSettings = SpellCheckSettings(
			countThreshold = 0,
			prefixLength = 40,
			maxEditDistance = 2.0
		)

		var dataHolder: DataHolder = InMemoryDataHolder(
			spellCheckSettings,
			Murmur3HashFunction()
		)

		val spellChecker: SpellChecker = SymSpellCheck(
			dataHolder,
			accuracyTest.getStringDistance(spellCheckSettings, null),
			spellCheckSettings
		)
		accuracyTest.run(spellChecker)
		println("==================================================")

		//Weighted
		println("=========  Weighted =============================")
		spellCheckSettings = SpellCheckSettings(
			deletionWeight = 1.01f,
			insertionWeight = 0.9f,
			replaceWeight = 0.7f,
			transpositionWeight = 1.0f,
			countThreshold = 0,
			prefixLength = 40,
			maxEditDistance = 2.0
		)

		dataHolder = InMemoryDataHolder(
			spellCheckSettings,
			Murmur3HashFunction()
		)
		val weightedSpellChecker: SpellChecker = SymSpellCheck(
			dataHolder,
			accuracyTest.getStringDistance(spellCheckSettings, null),
			spellCheckSettings
		)
		accuracyTest.run(weightedSpellChecker)
		println("==================================================")


		//Qwerty
		println("=========  Qwerty =============================")
		spellCheckSettings = SpellCheckSettings(
			countThreshold = 0,
			prefixLength = 40,
			maxEditDistance = 2.0
		)
		dataHolder = InMemoryDataHolder(
			spellCheckSettings,
			Murmur3HashFunction()
		)
		val keyboardSpellChecker: SpellChecker = SymSpellCheck(
			dataHolder,
			accuracyTest.getStringDistance(spellCheckSettings, QwertyDistance()),
			spellCheckSettings
		)
		accuracyTest.run(keyboardSpellChecker)
		println("==================================================")

		//QwertyWeighted
		println("=========  QwertyWeighted =============================")
		spellCheckSettings = SpellCheckSettings(
			deletionWeight = 1.01f,
			insertionWeight = 0.9f,
			replaceWeight = 0.7f,
			transpositionWeight = 1.0f,
			countThreshold = 0,
			prefixLength = 40,
			maxEditDistance = 2.0,
		)
		dataHolder = InMemoryDataHolder(
			spellCheckSettings,
			Murmur3HashFunction()
		)
		val keyboardWeightedSpellChecker: SpellChecker = SymSpellCheck(
			dataHolder,
			accuracyTest.getStringDistance(spellCheckSettings, QwertyDistance()),
			spellCheckSettings
		)
		accuracyTest.run(keyboardWeightedSpellChecker)
		println("==================================================")
	}

	private fun getStringDistance(
		spellCheckSettings: SpellCheckSettings,
		charDistance: CharDistance?
	): StringDistance {
		return WeightedDamerauLevenshteinDistance(
			spellCheckSettings.deletionWeight.toDouble(),
			spellCheckSettings.insertionWeight.toDouble(),
			spellCheckSettings.replaceWeight.toDouble(),
			spellCheckSettings.transpositionWeight.toDouble(),
			charDistance
		)
	}

	companion object {
		private const val fullTestData = "full_test.txt"

		// very verbose!!
		private const val printFailures = false

		private const val acceptSecondHitAsSuccess = false

		private fun isMatch(candidate: Map.Entry<String, String>, results: List<SuggestionItem>): Boolean {
			return ((results.isNotEmpty() && results[0].term.trim() == candidate.value)
					|| (results.isNotEmpty() && results[0].term.trim() == candidate.key)
					|| (acceptSecondHitAsSuccess && results.size > 1 && results[1].term == candidate.value))
		}
	}
}
