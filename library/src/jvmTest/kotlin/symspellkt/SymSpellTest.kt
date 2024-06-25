package symspellkt

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.common.stringdistance.WeightedDamerauLevenshteinDistance
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import com.darkrockstudios.symspellkt.impl.SymSpellCheck
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

class SymSpellTest {
	private lateinit var dataHolder: DataHolder
	private lateinit var symSpellCheck: SymSpellCheck
	private lateinit var weightedDamerauLevenshteinDistance: WeightedDamerauLevenshteinDistance

	@Before
	@Throws(IOException::class, SpellCheckException::class)
	fun setup() {
		val classLoader = SymSpellTest::class.java.classLoader

		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 1L,
			deletionWeight = 1.0,
			insertionWeight = 1.0,
			replaceWeight = 1.0,
			maxEditDistance = 2.0,
			transpositionWeight = 1.0,
			topK = 100,
			prefixLength = 10,
			verbosity = Verbosity.ALL,
		)

		weightedDamerauLevenshteinDistance =
			WeightedDamerauLevenshteinDistance(
				spellCheckSettings.deletionWeight,
				spellCheckSettings.insertionWeight,
				spellCheckSettings.replaceWeight,
				spellCheckSettings.transpositionWeight,
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
	fun testMultiWordCorrection() {
		assertTypoAndCorrected(
			symSpellCheck,
			"theq uick brown f ox jumps over the lazy dog",
			"the quick brown fox jumps over the lazy dog",
			2.0
		)

		assertTypoAndCorrected(
			symSpellCheck,
			"Whereis th elove hehaD Dated FOREEVER forImuch of thepast who couqdn'tread in sixthgrade AND ins pired him",
			"where is the love he had dated forever for much of the past who couldn't read in sixth grade and inspired him",
			2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection2() {
		assertTypoAndCorrected(
			symSpellCheck,
			"Whereis th elove hehaD",
			"where is the love he had",
			2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testSingleWordCorrection() {
		assertTypoAndCorrected(
			symSpellCheck,
			"bigjest", "biggest", 2.0
		)
		assertTypoAndCorrected(
			symSpellCheck,
			"playrs", "players", 2.0
		)
		assertTypoAndCorrected(
			symSpellCheck,
			"slatew", "slate", 2.0
		)
		assertTypoAndCorrected(
			symSpellCheck,
			"ith", "with", 2.0
		)
		assertTypoAndCorrected(
			symSpellCheck,
			"plety", "plenty", 2.0
		)
		assertTypoAndCorrected(
			symSpellCheck,
			"funn", "fun", 2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDoubleWordCorrection() {
		val testPhrase = "couqdn'tread".lowercase(Locale.getDefault())
		val correctedPhrase = "couldn't read"
		val suggestionItems: List<SuggestionItem> = symSpellCheck
			.lookupCompound(testPhrase.lowercase(Locale.getDefault()), 2.0)

		Assert.assertTrue(suggestionItems.isNotEmpty())
		Assert.assertEquals(correctedPhrase.lowercase(), suggestionItems[0].term.trim())
	}

	@Test
	fun testDoubleComparison() {
		Assert.assertTrue(SpellHelper.isEqualDouble(1.00999, 1.0))
		Assert.assertTrue(SpellHelper.isLessDouble(0.90999, 1.0))
		Assert.assertTrue(SpellHelper.isLessOrEqualDouble(0.7, 1.0))
	}

	@Test(expected = SpellCheckException::class)
	@Throws(SpellCheckException::class)
	fun testEdgeCases2() {
		val suggestionItems: List<SuggestionItem> = symSpellCheck
			.lookupCompound("tes", 5.0)
		Assert.assertNotNull(suggestionItems)
		assertTypoAndCorrected(
			symSpellCheck,
			"", "with", 2.0
		)
		assertTypoAndCorrected(
			symSpellCheck,
			"", "with", 3.0
		)
	}

	@Test(expected = SpellCheckException::class)
	@Throws(SpellCheckException::class)
	fun testEdgeCases3() {
		val suggestionItems: List<SuggestionItem> = symSpellCheck
			.lookupCompound("a", 5.0)
		Assert.assertNotNull(suggestionItems)
		assertTypoAndCorrected(
			symSpellCheck,
			"", "with", 2.0
		)
		assertTypoAndCorrected(
			symSpellCheck,
			"", "with", 3.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testLookup() {
		var suggestionItems = symSpellCheck.lookup("hel")
		Assert.assertNotNull(suggestionItems)
		Assert.assertTrue(suggestionItems.size > 0)
		Assert.assertEquals(78, suggestionItems.size)

		suggestionItems = symSpellCheck.lookup("hel", Verbosity.ALL)
		Assert.assertEquals(78, suggestionItems.size)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testLookupCloset() {
		val suggestionItems: List<SuggestionItem> = symSpellCheck.lookup("resial", Verbosity.CLOSEST)
		Assert.assertNotNull(suggestionItems)
		Assert.assertTrue(suggestionItems.isNotEmpty())
		Assert.assertEquals(3, suggestionItems.size)
	}

	@Test
	@Throws(Exception::class)
	fun testWordBreak() {
		val suggestionItems: Composition = symSpellCheck
			.wordBreakSegmentation(
				"itwasabrightcolddayinaprilandtheclockswerestrikingthirteen", 10,
				2.0
			)
		Assert.assertNotNull(suggestionItems)
		Assert.assertEquals(
			"it was bright cold day in april and the clock were striking thirteen",
			suggestionItems.correctedString
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

	companion object {
		@Throws(SpellCheckException::class)
		fun assertTypoAndCorrected(
			spellCheck: SymSpellCheck, typo: String, correct: String,
			maxEd: Double
		) {
			val suggestionItems: List<SuggestionItem> = spellCheck
				.lookupCompound(typo.lowercase().trim(), maxEd)
			Assert.assertTrue(suggestionItems.isNotEmpty())
			Assert.assertEquals(
				correct.lowercase().trim(),
				suggestionItems[0].term.trim()
			)
		}
	}
}
