package symspellkt.benchmark.utils

import kotlinx.serialization.Serializable

@Serializable
data class ConfidenceInterval(
	val mean: Double,
	val lower: Double,
	val upper: Double
) {
	fun overlaps(other: ConfidenceInterval): Boolean {
		return !(this.upper < other.lower || this.lower > other.upper)
	}

	// For printing
	override fun toString(): String =
		String.format("%.2f [%.2f, %.2f]", mean, lower, upper)
}

fun calculateConfidenceInterval(
	mean: Double,
	stdDev: Double,
	sampleSize: Int,
	confidenceLevel: Double = 0.99
): ConfidenceInterval {
	// Convert confidence level to t-value
	// These are approximate t-values for common confidence levels
	val tValue = when (confidenceLevel) {
		0.99 -> when {
			sampleSize <= 2 -> 63.657
			sampleSize <= 3 -> 9.925
			sampleSize <= 4 -> 5.841
			sampleSize <= 5 -> 4.604
			sampleSize <= 10 -> 3.250
			sampleSize <= 20 -> 2.861
			sampleSize <= 30 -> 2.756
			else -> 2.576
		}
		0.95 -> when {
			sampleSize <= 2 -> 12.71
			sampleSize <= 3 -> 4.303
			sampleSize <= 4 -> 3.182
			sampleSize <= 5 -> 2.776
			sampleSize <= 10 -> 2.262
			sampleSize <= 20 -> 2.093
			sampleSize <= 30 -> 2.042
			else -> 1.96
		}
		0.90 -> when {
			sampleSize <= 2 -> 6.314
			sampleSize <= 3 -> 2.920
			sampleSize <= 4 -> 2.353
			sampleSize <= 5 -> 2.132
			sampleSize <= 10 -> 1.833
			sampleSize <= 20 -> 1.729
			sampleSize <= 30 -> 1.699
			else -> 1.645
		}
		else -> throw IllegalArgumentException("Supported confidence levels are: 0.90, 0.95, 0.99")
	}

	val standardError = stdDev / kotlin.math.sqrt(sampleSize.toDouble())
	val margin = tValue * standardError

	return ConfidenceInterval(
		mean = mean,
		lower = mean - margin,
		upper = mean + margin
	)
}