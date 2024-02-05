package symspellkt

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import com.darkrockstudios.symspellkt.impl.SymSpellCheck
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class SymSpellTestWeighted {
	private lateinit var dataHolder: DataHolder
	private lateinit var symSpellCheck: SymSpellCheck
	private lateinit var weightedDamerauLevenshteinDistance: WeightedDamerauLevenshteinDistance

	@Before
	@Throws(IOException::class, SpellCheckException::class)
	fun setup() {
		val classLoader = SymSpellTest::class.java.classLoader

		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 1,
			deletionWeight = 0.8,
			insertionWeight = 1.01,
			replaceWeight = 1.5,
			maxEditDistance = 2.0,
			transpositionWeight = 0.7,
			topK = 5,
			prefixLength = 10,
			verbosity = Verbosity.ALL,
		)

		weightedDamerauLevenshteinDistance =
			WeightedDamerauLevenshteinDistance(
				spellCheckSettings.deletionWeight,
				spellCheckSettings.insertionWeight,
				spellCheckSettings.replaceWeight,
				spellCheckSettings.transpositionWeight,
				null
			)
		dataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())

		symSpellCheck = SymSpellCheck(
			dataHolder, weightedDamerauLevenshteinDistance,
			spellCheckSettings
		)
		val file = File(classLoader.getResource("frequency_dictionary_en_82_765.txt")!!.file)
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dataHolder.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
		}
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testSingleWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"uick", "quick", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"bigjest", "big jest", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"playrs", "plays", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"slatew", "slate", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"ith", "it", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"plety", "plenty", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"funn", "fun", 2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDoubleWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"theq uick brown f ox jumps over the lazy dog",
			"the quick brown fox jumps over the lazy dog",
			2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"theq uick brown f ox jumps over the lazy dog",
			"the quick brown fox jumps over the lazy dog",
			2.0
		)
	}
}
