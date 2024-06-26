package symspellkt

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDictionaryHolder
import com.darkrockstudios.symspellkt.impl.SymSpell
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

class GermanLangSpellChecker {

	lateinit var dictionaryHolder1: DictionaryHolder
	lateinit var dictionaryHolder2: DictionaryHolder
	lateinit var symSpell: SymSpell
	lateinit var damerauLevenshteinDistance: DamerauLevenshteinDistance

	@Before
	@Throws(IOException::class, SpellCheckException::class)
	fun setup() {
		val classLoader = SymSpellTest::class.java.classLoader

		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 1,
			maxEditDistance = 2.0,
			topK = 5,
			prefixLength = 10,
			verbosity = Verbosity.ALL,
		)

		damerauLevenshteinDistance = DamerauLevenshteinDistance()

		dictionaryHolder1 = InMemoryDictionaryHolder(spellCheckSettings, Murmur3HashFunction())
		dictionaryHolder2 = InMemoryDictionaryHolder(spellCheckSettings, Murmur3HashFunction())

		symSpell = SymSpell(
			dictionaryHolder = dictionaryHolder1,
			stringDistance = damerauLevenshteinDistance,
			spellCheckSettings = spellCheckSettings
		)

		loadUniGramFile(
			File(classLoader.getResource("de-100k.txt")!!.file)
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection() {
		assertTypoAndCorrected(
			symSpell,
			"entwick lung".lowercase(Locale.getDefault()),
			"entwicklung".lowercase(Locale.getDefault()),
			2.0
		)

		assertTypoEdAndCorrected(
			symSpell,
			"nömlich".lowercase(Locale.getDefault()),
			"nämlich".lowercase(Locale.getDefault()),
			2.0, 1.0
		)
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun loadUniGramFile(file: File) {
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dictionaryHolder1.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
			dictionaryHolder2.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
		}
	}

	companion object {
		@Throws(SpellCheckException::class)
		fun assertTypoAndCorrected(
			spellCheck: SymSpell, typo: String, correct: String,
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

		@Throws(SpellCheckException::class)
		fun assertTypoEdAndCorrected(
			spellCheck: SymSpell, typo: String, correct: String,
			maxEd: Double, expED: Double
		) {
			val suggestionItems: List<SuggestionItem> = spellCheck
				.lookupCompound(typo.lowercase().trim(), maxEd)
			Assert.assertTrue(suggestionItems.isNotEmpty())
			Assert.assertEquals(
				correct.lowercase().trim(),
				suggestionItems[0].term.trim()
			)
			Assert.assertEquals(suggestionItems[0].distance, expED, 0.12)
		}
	}
}
