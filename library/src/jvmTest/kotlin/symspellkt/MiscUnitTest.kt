package symspellkt

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import com.darkrockstudios.symspellkt.impl.SymSpellCheck
import org.junit.Assert
import org.junit.Test

class MiscUnitTest {
	@Test
	fun testDictionaryItem() {
		val di = DictionaryItem("term", 1.0, 2.0)
		Assert.assertEquals("term", di.term)
		Assert.assertTrue(1.0 == di.frequency)
		Assert.assertTrue(2.0 == di.distance)
	}

	@Test
	fun testSpellCheckSettings() {
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

		spellCheckSettings.maxLength = 1
		Assert.assertNotNull(spellCheckSettings)
		Assert.assertEquals(1, spellCheckSettings.maxLength)
		Assert.assertTrue(spellCheckSettings.toString().contains("countThreshold"))
		Assert.assertTrue(spellCheckSettings.toString().length > 20)
	}

	@Test
	fun testSpellChecker() {
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

		val weightedDamerauLevenshteinDistance: StringDistance =
			WeightedDamerauLevenshteinDistance(
				spellCheckSettings.deletionWeight,
				spellCheckSettings.insertionWeight,
				spellCheckSettings.replaceWeight,
				spellCheckSettings.transpositionWeight,
				null
			)
		val dataHolder: DataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())

		val symSpellCheck: SpellChecker = SymSpellCheck(
			dataHolder, weightedDamerauLevenshteinDistance,
			spellCheckSettings
		)

		Assert.assertEquals(dataHolder, symSpellCheck.dataHolder)
		Assert.assertEquals(weightedDamerauLevenshteinDistance, symSpellCheck.stringDistance)
		Assert.assertEquals(spellCheckSettings, symSpellCheck.spellCheckSettings)
	}

	@Test
	fun testSpellDeletes() {
		val del: MutableSet<String> = SpellHelper.getEditDeletes("a", 2.0, 0, 1.0).toMutableSet()
		Assert.assertNotNull(del)
		Assert.assertEquals(2, del.size.toLong())

		val del1: Set<String> = SpellHelper.edits("", 2.0, del, 2.0)
		Assert.assertNotNull(del)
		Assert.assertEquals(del1, del)
	}

	@Test
	fun testEarlyExit() {
		var suggestionItems: MutableList<SuggestionItem> = ArrayList<SuggestionItem>()
		var suggestionItems1: MutableList<SuggestionItem> = SpellHelper
			.earlyExit(suggestionItems, "term", 2.0, false)

		Assert.assertNotNull(suggestionItems1)
		Assert.assertEquals(1, suggestionItems1.size.toLong())
		suggestionItems = ArrayList<SuggestionItem>()
		suggestionItems1 = SpellHelper
			.earlyExit(suggestionItems, "term", 2.0, true)
		Assert.assertNotNull(suggestionItems1)
		Assert.assertEquals(0, suggestionItems1.size.toLong())
	}

	@Test
	fun suggestItemTest() {
		var si: SuggestionItem = SuggestionItem("term1", 1.0, 20.0)
		var si2: SuggestionItem? = SuggestionItem("term2", 1.001, 20.0)
		Assert.assertEquals(0, si.compare(si, si2))

		si = SuggestionItem("term1", 1.0, 20.0)
		si2 = SuggestionItem("term2", 2.001, 20.0)
		Assert.assertEquals(-1, si.compare(si, si2))

		si = SuggestionItem("term1", 2.0, 20.0)
		si2 = SuggestionItem("term2", 1.001, 20.0)
		Assert.assertEquals(1, si.compare(si, si2))

		si = SuggestionItem("term1", 1.0, 21.0)
		si2 = SuggestionItem("term2", 1.001, 20.0)
		Assert.assertEquals(-1, si.compare(si, si2))

		si = SuggestionItem("term1", 1.0, 20.0)
		si2 = SuggestionItem("term2", 1.001, 21.0)
		Assert.assertEquals(1, si.compare(si, si2))

		si = SuggestionItem("term1", 1.0, 20.0)
		si2 = SuggestionItem("term2", 1.001, 21.0)
		Assert.assertFalse(si == si2)

		si = SuggestionItem("term1", 1.0, 20.0)
		si2 = SuggestionItem("term1", 1.001, 20.0)
		Assert.assertFalse(si == si2)

		Assert.assertTrue(si.toString().length > 10)
	}
}
