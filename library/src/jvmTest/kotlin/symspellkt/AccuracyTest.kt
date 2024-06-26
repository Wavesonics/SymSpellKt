package symspellkt

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.darkrockstudios.symspellkt.common.stringdistance.DamerauLevenshteinDistance
import com.darkrockstudios.symspellkt.common.stringdistance.LevenshteinDistance
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDictionaryHolder
import com.darkrockstudios.symspellkt.impl.SymSpell
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import symspellkt.benchmark.StopWatch
import java.io.IOException
import java.nio.charset.Charset
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

			spellChecker.createDictionaryEntry(csvRecord.get(0), 1.0)
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
			val results: MutableList<SuggestionItem> =
				spellChecker.lookupCompound(candidate.key).toMutableList()
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
					+ String.format(
				"%1$.3f searches/ms",
				(count.toDouble() / (stopWatch.getTime()))
			)
		)
		println()
		println(
			success.toString() + " success / accuracy => " + String.format(
				"%.2f%%",
				(100.0 * success / count)
			)
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

		println("========= Pure DamerauLevenshteinDistance =============================")
		//Basic
		var spellCheckSettings = SpellCheckSettings(
			countThreshold = 0,
			prefixLength = 40,
		)

		var dictionaryHolder: DictionaryHolder = InMemoryDictionaryHolder(
			spellCheckSettings,
			Murmur3HashFunction()
		)

		val spellChecker: SpellChecker = SymSpell(
			dictionaryHolder = dictionaryHolder,
			stringDistance = DamerauLevenshteinDistance(),
			spellCheckSettings = spellCheckSettings,
		)
		accuracyTest.run(spellChecker)

		println("=========  Pure Levenshtein =============================")
		spellCheckSettings = SpellCheckSettings(
			countThreshold = 0,
			prefixLength = 40,
		)
		dictionaryHolder = InMemoryDictionaryHolder(
			spellCheckSettings,
			Murmur3HashFunction()
		)
		val pureLevenshteinSpellChecker: SpellChecker = SymSpell(
			dictionaryHolder = dictionaryHolder,
			stringDistance = LevenshteinDistance(),
			spellCheckSettings = spellCheckSettings
		)
		accuracyTest.run(pureLevenshteinSpellChecker)
		println("==================================================")

	}

	companion object {
		private const val fullTestData = "full_test.txt"

		// very verbose!!
		private const val printFailures = false

		private const val acceptSecondHitAsSuccess = false

		private fun isMatch(
			candidate: Map.Entry<String, String>,
			results: List<SuggestionItem>
		): Boolean {
			return ((results.isNotEmpty() && results[0].term.trim() == candidate.value)
					|| (results.isNotEmpty() && results[0].term.trim() == candidate.key)
					|| (acceptSecondHitAsSuccess && results.size > 1 && results[1].term == candidate.value))
		}
	}
}
