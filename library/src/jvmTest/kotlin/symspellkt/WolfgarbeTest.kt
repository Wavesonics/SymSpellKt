package symspellkt

import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.impl.SymSpell
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * These are the tests from the canonical implementation
 * https://github.com/wolfgarbe/SymSpell/blob/master/SymSpell.Test/SymSpell.Test.cs
 */
class WolfgarbeTest {

	@Test
	fun WordsWithSharedPrefixShouldRetainCounts() {
		val symSpell = SymSpell(
			SpellCheckSettings(
				topK = 16,
				maxEditDistance = 1.0,
				prefixLength = 3
			)
		)
		symSpell.createDictionaryEntry("pipe", 5.0)
		symSpell.createDictionaryEntry("pips", 10.0)

		var result = symSpell.lookup("pipe", Verbosity.ALL, 1.0)
		assertEquals(2, result.size)
		assertEquals("pipe", result[0].term)
		assertEquals(5.0, result[0].frequency)
		assertEquals("pips", result[1].term)
		assertEquals(10.0, result[1].frequency)

		result = symSpell.lookup("pips", Verbosity.ALL, 1.0)
		assertEquals(2, result.size)
		assertEquals("pips", result[0].term)
		assertEquals(10.0, result[0].frequency)
		assertEquals("pipe", result[1].term)
		assertEquals(5.0, result[1].frequency)

		result = symSpell.lookup("pip", Verbosity.ALL, 1.0)
		assertEquals(2, result.size)
		assertEquals("pips", result[0].term)
		assertEquals(10.0, result[0].frequency)
		assertEquals("pipe", result[1].term)
		assertEquals(5.0, result[1].frequency)
	}

	@Test
	fun addAdditionalCountsShouldNotAddWordAgain() {
		val symSpell = SymSpell()
		val word = "hello"
		symSpell.createDictionaryEntry(word, 11.0)
		assertEquals(1, symSpell.dictionary.wordCount)

		symSpell.createDictionaryEntry(word, 3.0)
		assertEquals(1, symSpell.dictionary.wordCount)
	}

	@Test
	fun addAdditionalCountsShouldNotOverflow() {
		val symSpell = SymSpell()
		val word = "hello"
		symSpell.createDictionaryEntry(word, Double.MAX_VALUE - 10.0)
		var result = symSpell.lookup(word, Verbosity.TOP)
		var count = if (result.isNotEmpty()) result[0].frequency else 0
		assertEquals((Double.MAX_VALUE - 10.0), count)

		symSpell.createDictionaryEntry(word, 11)
		result = symSpell.lookup(word, Verbosity.TOP)
		count = if (result.isNotEmpty()) result[0].frequency else 0
		assertEquals(Double.MAX_VALUE, count)
	}

	@Test
	fun verbosityShouldControlLookupResults() {
		val symSpell = SymSpell()
		symSpell.createDictionaryEntry("steam", 1)
		symSpell.createDictionaryEntry("steams", 2)
		symSpell.createDictionaryEntry("steem", 3)
		var result = symSpell.lookup("steems", Verbosity.TOP, 2.0)
		assertEquals(1, result.size)

		result = symSpell.lookup("steems", Verbosity.CLOSEST, 2.0)
		assertEquals(2, result.size)

		result = symSpell.lookup("steems", Verbosity.ALL, 2.0)
		assertEquals(3, result.size)
	}

	@Test
	fun lookupShouldReturnMostFrequent() {
		val symSpell = SymSpell()
		symSpell.createDictionaryEntry("steama", 4)
		symSpell.createDictionaryEntry("steamb", 6)
		symSpell.createDictionaryEntry("steamc", 2)
		val result = symSpell.lookup("steam", Verbosity.TOP, 2.0)
		assertEquals(1, result.size)
		assertEquals("steamb", result[0].term)
		assertEquals(6.0, result[0].frequency)
	}

	@Test
	fun lookupShouldFindExactMatch() {
		val symSpell = SymSpell()
		symSpell.createDictionaryEntry("steama", 4)
		symSpell.createDictionaryEntry("steamb", 6)
		symSpell.createDictionaryEntry("steamc", 2)

		val result = symSpell.lookup("steama", Verbosity.TOP, 2.0)
		assertEquals(1, result.size)
		assertEquals("steama", result[0].term)
	}

	@Test
	fun lookupShouldNotReturnNonWordDelete() {
		val symSpell = SymSpell(
			SpellCheckSettings(
				topK = 16,
				maxEditDistance = 2.0,
				prefixLength = 7,
				countThreshold = 10,
			)
		)
		symSpell.createDictionaryEntry("pawn", 10)

		var result = symSpell.lookup("paw", Verbosity.TOP, 0.0)
		assertEquals(0, result.size)

		result = symSpell.lookup("awn", Verbosity.TOP, 0.0)
		assertEquals(0, result.size)
	}

	@Test
	fun lookupShouldNotReturnLowCountWord() {
		val symSpell = SymSpell(
			SpellCheckSettings(
				topK = 16,
				maxEditDistance = 2.0,
				prefixLength = 7,
				countThreshold = 10,
			)
		)
		symSpell.createDictionaryEntry("pawn", 1)

		val result = symSpell.lookup("pawn", Verbosity.TOP, 0.0)
		assertEquals(0, result.size)
	}

	@Test
	fun lookupShouldNotReturnLowCountWordThatsAlsoDeleteWord() {
		val symSpell = SymSpell(
			SpellCheckSettings(
				topK = 16,
				maxEditDistance = 2.0,
				prefixLength = 7,
				countThreshold = 10,
			)
		)
		symSpell.createDictionaryEntry("flame", 20)
		symSpell.createDictionaryEntry("flam", 1)

		val result = symSpell.lookup("flam", Verbosity.TOP, 0.0)
		assertEquals(0, result.size)
	}
}