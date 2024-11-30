package symspellkt.benchmark.utils

import junit.framework.TestCase.assertFalse
import org.junit.Test
import kotlin.test.assertTrue

class ConfidenceIntervalTests {
	@Test
	fun testNoOverlap() {
		val interval1 = ConfidenceInterval(0.28, 0.20, 0.31)
		val interval2 = ConfidenceInterval(0.49, 0.32, 0.67)

		assertFalse(interval1.overlaps(interval2))
	}

	@Test
	fun testOverlap() {
		val interval1 = ConfidenceInterval(0.28, 0.20, 0.34)
		val interval2 = ConfidenceInterval(0.49, 0.32, 0.67)

		assertTrue(interval1.overlaps(interval2))
	}

	@Test
	fun testOverlapEqual() {
		val interval1 = ConfidenceInterval(0.49, 0.20, 0.33)
		val interval2 = ConfidenceInterval(0.49, 0.33, 0.67)

		assertTrue(interval1.overlaps(interval2))
	}
}