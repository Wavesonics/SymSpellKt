package symspellkt

import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.QwertyDistance
import com.darkrockstudios.symspellkt.common.SpellHelper
import com.darkrockstudios.symspellkt.common.WeightedDamerauLevenshteinDistance
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TestedWDamerauLevenshtein {
	private lateinit var damerauLevenshtein: StringDistance
	private lateinit var wdamerauLevenshtein: StringDistance
	private lateinit var customDamerauLevenshtein: StringDistance

	@Before
	fun setup() {
		damerauLevenshtein = WeightedDamerauLevenshteinDistance(1.0, 1.0, 1.0, 1.0, null)
		wdamerauLevenshtein = WeightedDamerauLevenshteinDistance(0.8, 1.01, 0.9, 0.7, null)
		customDamerauLevenshtein = WeightedDamerauLevenshteinDistance(
			0.8, 1.01, 0.9, 0.7,
			QwertyDistance()
		)
	}

	@Test
	fun testWeightedDamerauLevenshtein() {
		Assert.assertEquals(0.89, wdamerauLevenshtein.getDistance("sommer", "summer"), SpellHelper.EPSILON)
		Assert.assertEquals(0.89, wdamerauLevenshtein.getDistance("bigjest", "big est"), SpellHelper.EPSILON)
		Assert.assertEquals(1.8, wdamerauLevenshtein.getDistance("cool", "cola"), SpellHelper.EPSILON)
		Assert.assertEquals(0.9, wdamerauLevenshtein.getDistance("slives", "slices"), SpellHelper.EPSILON)
		Assert.assertEquals(0.9, wdamerauLevenshtein.getDistance("slives", "olives"), SpellHelper.EPSILON)
		Assert.assertEquals(0.0, wdamerauLevenshtein.getDistance("slime", "slime"), SpellHelper.EPSILON)
	}

	@Test
	fun testDamerauLevenshtein() {
		Assert.assertEquals(1.0, damerauLevenshtein.getDistance("sommer", "summer"), SpellHelper.EPSILON)
		Assert.assertEquals(1.0, damerauLevenshtein.getDistance("bigjest", "big est"), SpellHelper.EPSILON)
		Assert.assertEquals(2.0, damerauLevenshtein.getDistance("cool", "cola"), SpellHelper.EPSILON)
		Assert.assertEquals(1.0, damerauLevenshtein.getDistance("slives", "slices"), SpellHelper.EPSILON)
		Assert.assertEquals(1.0, damerauLevenshtein.getDistance("slives", "olives"), SpellHelper.EPSILON)
		Assert.assertEquals(0.0, damerauLevenshtein.getDistance("slime", "slime"), SpellHelper.EPSILON)
		Assert.assertEquals(1.0, damerauLevenshtein.getDistance("playrs", "players"), SpellHelper.EPSILON)
		Assert.assertEquals(2.0, damerauLevenshtein.getDistance("playrs", "player"), SpellHelper.EPSILON)
	}

	@Test
	fun testCustomDamerauLevenshtein() {
		Assert.assertEquals(0.899, customDamerauLevenshtein.getDistance("bigjest", "biggest"), SpellHelper.EPSILON)
		Assert.assertEquals(0.899, customDamerauLevenshtein.getDistance("bigjest", "big est"), SpellHelper.EPSILON)
		Assert.assertEquals(1.009, customDamerauLevenshtein.getDistance("bigjest", "big jest"), SpellHelper.EPSILON)
		Assert.assertEquals(1.25, customDamerauLevenshtein.getDistance("cool", "cola"), SpellHelper.EPSILON)
		Assert.assertEquals(0.08, customDamerauLevenshtein.getDistance("slives", "slices"), SpellHelper.EPSILON)
		Assert.assertEquals(0.899, customDamerauLevenshtein.getDistance("slives", "olives"), SpellHelper.EPSILON)
		Assert.assertEquals(0.0, customDamerauLevenshtein.getDistance("slime", "slime"), SpellHelper.EPSILON)
	}
}
