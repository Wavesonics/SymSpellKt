package symspellkt

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import org.junit.*

class TestDataHolder {
	private lateinit var dataHolder: DataHolder

	@Before
	fun setup() {
		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 4,
			deletionWeight = 0.8f,
			insertionWeight = 1.01f,
			replaceWeight = 0.9f,
			maxEditDistance = 2.0,
			transpositionWeight = 0.7f,
			topK = 5,
			prefixLength = 10,
			verbosity = Verbosity.ALL,
		)

		dataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderAdd() {
		dataHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("cold", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("cool", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("war", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("dummy", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("delta", 12.0, -1.0))

		Assert.assertEquals(6, dataHolder.size.toLong())
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderGetItemFreq() {
		dataHolder.addItem(DictionaryItem("word", 21.0, -1.0))
		dataHolder.addItem(DictionaryItem("cold", 22.0, -1.0))
		dataHolder.addItem(DictionaryItem("cool", 23.0, -1.0))
		Assert.assertEquals(22.0, dataHolder.getItemFrequency("cold"))
		Assert.assertEquals(23.0, dataHolder.getItemFrequency("cool"))
		Assert.assertEquals(21.0, dataHolder.getItemFrequency("word"))
		Assert.assertNull(dataHolder.getItemFrequency("hello"))
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderGetDeletes() {
		dataHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("cold", 21.0, -1.0))
		dataHolder.addItem(DictionaryItem("cool", 3.0, -1.0))

		Assert.assertNull(dataHolder.getItemFrequency("cool"))

		dataHolder.addItem(DictionaryItem("cool", 8.0, -1.0))

		Assert.assertEquals(11.0, dataHolder.getItemFrequency("cool"))
		Assert.assertEquals(1, dataHolder.getDeletes("wod")?.size)
		Assert.assertEquals(2, dataHolder.getDeletes("col")?.size)
		Assert.assertFalse(dataHolder.addItem(DictionaryItem("temp_data", 0.1, 0.0)))
		Assert.assertFalse(dataHolder.addItem(DictionaryItem("temp_data", -0.1, 0.0)))
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderClear() {
		dataHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("cold", 21.0, -1.0))
		dataHolder.addItem(DictionaryItem("cool", 3.0, -1.0))
		Assert.assertEquals(2, dataHolder.size)
		Assert.assertNotNull(dataHolder.getDeletes("col"))
		Assert.assertEquals(1, dataHolder.getDeletes("col")?.size)
		dataHolder.clear()
		Assert.assertEquals(0, dataHolder.size)
		Assert.assertNull(dataHolder.getDeletes("col"))
	}

	@After
	@Throws(SpellCheckException::class)
	fun clear() {
		dataHolder.clear()
	}
}
