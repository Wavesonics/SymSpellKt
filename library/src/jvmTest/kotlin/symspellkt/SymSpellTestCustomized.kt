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

class SymSpellTestCustomized {

	lateinit var dataHolder: DataHolder
	lateinit var symSpellCheck: SymSpellCheck
	lateinit var weightedDamerauLevenshteinDistance: WeightedDamerauLevenshteinDistance

	@Before
	@Throws(IOException::class, SpellCheckException::class)
	fun setup() {
		val classLoader = SymSpellTest::class.java.classLoader

		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 1,
			deletionWeight = 1.0,
			insertionWeight = 1.0,
			replaceWeight = 1.0,
			maxEditDistance = 2.0,
			transpositionWeight = 1.0,
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
				QwertyDistance(),
			)
		dataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())

		symSpellCheck = SymSpellCheck(
			dataHolder,
			weightedDamerauLevenshteinDistance,
			spellCheckSettings
		)
		loadUniGramFile(
			File(classLoader.getResource("frequency_dictionary_en_82_765.txt")!!.file)
		)
		loadBiGramFile(
			File(classLoader.getResource("frequency_bigramdictionary_en_243_342.txt")!!.file)
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testSingleWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"uick", "huck", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"bigjest", "biggest", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"playrs", "players", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"slatew", "slates", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"ith", "with", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"plety", "plenty", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"funn", "fun", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"slives", "slices", 2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDoubleWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(symSpellCheck, "Whereis", "where is", 2.0)
		SymSpellTest.assertTypoAndCorrected(symSpellCheck, "couqdn'tread", "couldn't read", 2.0)
		SymSpellTest.assertTypoAndCorrected(symSpellCheck, "hehaD", "he had", 2.0)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"Whereis th elove hehaD Dated FOREEVER forImuch of thepast who couqdn'tread in "
					+ "sixthgrade AND ins pired him",
			"where is the love he had dated forever for much of the past who couldn't read "
					+ "in sixth grade and inspired him",
			2.0
		)
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun loadUniGramFile(file: File) {
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dataHolder.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
		}
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun loadBiGramFile(file: File) {
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dataHolder
				.addItem(DictionaryItem(arr[0] + " " + arr[1], arr[2].toDouble(), -1.0))
		}
	}
}
