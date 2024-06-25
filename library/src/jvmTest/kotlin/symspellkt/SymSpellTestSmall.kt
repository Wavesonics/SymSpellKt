package symspellkt

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.common.stringdistance.LevenshteinDistance
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import com.darkrockstudios.symspellkt.impl.SymSpellCheck
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class SymSpellTestSmall {
	private lateinit var dataHolder: DataHolder
	private lateinit var symSpellCheck: SymSpellCheck
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
			verbosity = Verbosity.ALL,
		)

		stringDistance = LevenshteinDistance()
		dataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())

		symSpellCheck = SymSpellCheck(
			dataHolder,
			stringDistance,
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
			"bigjest", "biggest", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"playrs", "players", 2.0
		)
		SymSpellTest.assertTypoAndCorrected(
			symSpellCheck,
			"slatew", "slate", 2.0
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
