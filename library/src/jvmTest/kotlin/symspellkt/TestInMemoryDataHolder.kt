package symspellkt

import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import org.junit.*

class TestInMemoryDataHolder {
	private val spellCheckSettings = SpellCheckSettings(
		countThreshold = 4,
		deletionWeight = 0.8,
		insertionWeight = 1.01,
		replaceWeight = 0.9,
		maxEditDistance = 2.0,
		transpositionWeight = 0.7,
		topK = 5,
		prefixLength = 10,
		verbosity = Verbosity.ALL,
	)

	private lateinit var dataHolder: InMemoryDataHolder

	@Before
	fun setup() {
		dataHolder = InMemoryDataHolder(spellCheckSettings, Murmur3HashFunction())
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderAdd() {
		dataHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("word", 23.0, -1.0))
		dataHolder.addItem(DictionaryItem("cold", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("cool", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("war", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("dummy", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("delta", 12.0, -1.0))

		Assert.assertEquals(6, dataHolder.size)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderGetItemFreq() {
		dataHolder.addItem(DictionaryItem("word", 21.0, -1.0))
		dataHolder.addItem(DictionaryItem("cold", 22.0, -1.0))
		dataHolder.addItem(DictionaryItem("cool", 23.0, -1.0))
		Assert.assertEquals(22.0, dataHolder.getItemFrequency("cold"))
		Assert.assertNull(dataHolder.getItemFrequency("hello"))
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDataHolderGetDeletes() {
		dataHolder.addItem(DictionaryItem("word", 12.0, -1.0))
		dataHolder.addItem(DictionaryItem("cold", 21.0, -1.0))
		dataHolder.addItem(DictionaryItem("cool", 3.0, -1.0))
		dataHolder.addItem(DictionaryItem("tool", 3.0, -1.0))
		dataHolder.addItem(DictionaryItem("tool", 2.0, -1.0))

		Assert.assertNull(dataHolder.getItemFrequency("cool"))
		Assert.assertEquals(5.0, dataHolder.getItemFrequency("tool")!!, 0.01)
		Assert.assertEquals(12.0, dataHolder.getItemFrequency("word")!!, 0.01)
		Assert.assertEquals(21.0, dataHolder.getItemFrequency("cold")!!, 0.01)

		dataHolder.addItem(DictionaryItem("cool", 8.0, -1.0))

		Assert.assertEquals(11.0, dataHolder.getItemFrequency("cool"))
		Assert.assertEquals(1, dataHolder.getDeletes("wod")?.size)
		Assert.assertEquals(2, dataHolder.getDeletes("col")?.size)
		Assert.assertFalse(dataHolder.addItem(DictionaryItem("temp_data", 0.1, 0.0)))
		Assert.assertFalse(dataHolder.addItem(DictionaryItem("temp_data", -0.1, 0.0)))
	}

	@After
	@Throws(SpellCheckException::class)
	fun clear() {
		dataHolder.clear()
	}
}
