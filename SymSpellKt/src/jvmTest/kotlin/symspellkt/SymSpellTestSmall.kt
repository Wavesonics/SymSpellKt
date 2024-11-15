package symspellkt

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDictionaryHolder
import com.darkrockstudios.symspellkt.impl.SymSpell
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class SymSpellTestSmall {
	private lateinit var dictionaryHolder: DictionaryHolder
	private lateinit var symSpell: SymSpell
	private lateinit var stringDistance: StringDistance

	@Before
	@Throws(IOException::class, SpellCheckException::class)
	fun setup() {
		val classLoader = SymSpellTest::class.java.classLoader

		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 1,
			maxEditDistance = 2.0,
			topK = 5,
			prefixLength = 10,
			verbosity = Verbosity.All,
		)

		stringDistance = DamerauLevenshteinDistance()
		dictionaryHolder = InMemoryDictionaryHolder(spellCheckSettings, Murmur3HashFunction())

		symSpell = SymSpell(
			dictionaryHolder = dictionaryHolder,
			stringDistance = stringDistance,
			spellCheckSettings = spellCheckSettings,
		)
		val file = File(classLoader.getResource("frequency_dictionary_en_82_765.txt")!!.file)
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dictionaryHolder.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
		}
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testSingleWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"uick", "quick", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"bigjest", "biggest", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"playrs", "players", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"slatew", "slate", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"ith", "with", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"plety", "plenty", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"funn", "fun", 2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDoubleWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"theq uick brown f ox jumps over the lazy dog",
			"the quick brown fox jumps over the lazy dog",
			2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection() {
		SymSpellTest.assertTypoAndCorrected(
			symSpell,
			"theq uick brown f ox jumps over the lazy dog",
			"the quick brown fox jumps over the lazy dog",
			2.0
		)
	}
}
