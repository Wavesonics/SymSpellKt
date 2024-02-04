package symspellkt

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.common.*
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

class GermanLangSpellChecker {

	lateinit var dataHolder1: DataHolder
	lateinit var dataHolder2: DataHolder
	lateinit var symSpellCheck: SymSpellCheck
	lateinit var qwertzSymSpellCheck: SymSpellCheck
	lateinit var weightedDamerauLevenshteinDistance: WeightedDamerauLevenshteinDistance
	lateinit var qwertzWeightedDamerauLevenshteinDistance: WeightedDamerauLevenshteinDistance

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
				null
			)

		qwertzWeightedDamerauLevenshteinDistance =
			WeightedDamerauLevenshteinDistance(
				spellCheckSettings.deletionWeight,
				spellCheckSettings.insertionWeight,
				spellCheckSettings.replaceWeight,
				spellCheckSettings.transpositionWeight,
				QwertzDistance()
			)

		dataHolder1 = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())
		dataHolder2 = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())

		symSpellCheck = SymSpellCheck(
			dataHolder1,
			weightedDamerauLevenshteinDistance,
			spellCheckSettings
		)

		qwertzSymSpellCheck = SymSpellCheck(
			dataHolder2,
			qwertzWeightedDamerauLevenshteinDistance,
			spellCheckSettings
		)

		loadUniGramFile(
			File(classLoader.getResource("de-100k.txt")!!.file)
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection() {
		assertTypoAndCorrected(
			symSpellCheck,
			"entwick lung".lowercase(Locale.getDefault()),
			"entwicklung".lowercase(Locale.getDefault()),
			2.0
		)

		assertTypoEdAndCorrected(
			symSpellCheck,
			"nömlich".lowercase(Locale.getDefault()),
			"nämlich".lowercase(Locale.getDefault()),
			2.0, 1.0
		)

		assertTypoEdAndCorrected(
			qwertzSymSpellCheck,
			"nömlich".lowercase(Locale.getDefault()),
			"nämlich".lowercase(Locale.getDefault()),
			2.0, 0.10
		)
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun loadUniGramFile(file: File) {
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dataHolder1.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
			dataHolder2.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
		}
	}

	companion object {
		@Throws(SpellCheckException::class)
		fun assertTypoAndCorrected(
			spellCheck: SymSpellCheck, typo: String, correct: String,
			maxEd: Double
		) {
			val suggestionItems: List<SuggestionItem> = spellCheck
				.lookupCompound(typo.lowercase().trim { it <= ' ' }, maxEd)
			Assert.assertTrue(suggestionItems.isNotEmpty())
			Assert.assertEquals(
				correct.lowercase().trim { it <= ' ' },
				suggestionItems[0].term.trim()
			)
		}

		@Throws(SpellCheckException::class)
		fun assertTypoEdAndCorrected(
			spellCheck: SymSpellCheck, typo: String, correct: String,
			maxEd: Double, expED: Double
		) {
			val suggestionItems: List<SuggestionItem> = spellCheck
				.lookupCompound(typo.lowercase().trim { it <= ' ' }, maxEd)
			Assert.assertTrue(suggestionItems.isNotEmpty())
			Assert.assertEquals(
				correct.lowercase().trim { it <= ' ' },
				suggestionItems[0].term.trim()
			)
			Assert.assertEquals(suggestionItems[0].distance, expED, 0.12)
		}
	}
}
