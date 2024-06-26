package symspellkt

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDictionaryHolder
import org.junit.*

class TestDictionaryHolder {
	private lateinit var dictionaryHolder: DictionaryHolder

	@Before
	fun setup() {
		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 4,
			maxEditDistance = 2.0,
			topK = 5,
			prefixLength = 10,
			verbosity = Verbosity.ALL,
		)

		dictionaryHolder = InMemoryDictionaryHolder(spellCheckSettings, Murmur3HashFunction())
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderAdd() {
		dictionaryHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cold", 12.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cool", 12.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("war", 12.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("dummy", 12.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("delta", 12.0, -1.0))

		Assert.assertEquals(6, dictionaryHolder.wordCount.toLong())
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderGetItemFreq() {
		dictionaryHolder.addItem(DictionaryItem("word", 21.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cold", 22.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cool", 23.0, -1.0))
		Assert.assertEquals(22.0, dictionaryHolder.getItemFrequency("cold"))
		Assert.assertEquals(23.0, dictionaryHolder.getItemFrequency("cool"))
		Assert.assertEquals(21.0, dictionaryHolder.getItemFrequency("word"))
		Assert.assertNull(dictionaryHolder.getItemFrequency("hello"))
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderGetDeletes() {
		dictionaryHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cold", 21.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cool", 3.0, -1.0))

		Assert.assertNull(dictionaryHolder.getItemFrequency("cool"))

		dictionaryHolder.addItem(DictionaryItem("cool", 8.0, -1.0))

		Assert.assertEquals(11.0, dictionaryHolder.getItemFrequency("cool"))
		Assert.assertEquals(1, dictionaryHolder.getDeletes("wod")?.size)
		Assert.assertEquals(2, dictionaryHolder.getDeletes("col")?.size)
		Assert.assertFalse(dictionaryHolder.addItem(DictionaryItem("temp_data", 0.1, 0.0)))
		Assert.assertFalse(dictionaryHolder.addItem(DictionaryItem("temp_data", -0.1, 0.0)))
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderClear() {
		dictionaryHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cold", 21.0, -1.0))
		dictionaryHolder.addItem(DictionaryItem("cool", 3.0, -1.0))
		Assert.assertEquals(2, dictionaryHolder.wordCount)
		Assert.assertNotNull(dictionaryHolder.getDeletes("col"))
		Assert.assertEquals(1, dictionaryHolder.getDeletes("col")?.size)
		dictionaryHolder.clear()
		Assert.assertEquals(0, dictionaryHolder.wordCount)
		Assert.assertNull(dictionaryHolder.getDeletes("col"))
	}

	@After
	@Throws(SpellCheckException::class)
	fun clear() {
		dictionaryHolder.clear()
	}
}
