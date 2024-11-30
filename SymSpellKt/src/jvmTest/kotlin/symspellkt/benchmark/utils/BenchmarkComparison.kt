package symspellkt.benchmark.utils

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkComparison(
	val name: String,
	val parameters: Map<String, String>,
	val timeInterval: ConfidenceInterval,
	val baselineTimeInterval: ConfidenceInterval,
	val heapInterval: ConfidenceInterval,
	val baselineHeapInterval: ConfidenceInterval
) {
	companion object {
		const val SIGNIFICANT_CHANGE_THRESHOLD = 0.05 // 5%
	}

	val hasTimeRegression: Boolean
		get() {
			if (timeInterval.overlaps(baselineTimeInterval)) return false

			val percentChange = (timeInterval.mean - baselineTimeInterval.mean) / baselineTimeInterval.mean
			return percentChange > SIGNIFICANT_CHANGE_THRESHOLD
		}

	val hasHeapRegression: Boolean
		get() {
			if (heapInterval.overlaps(baselineHeapInterval)) return false

			val percentChange = (heapInterval.mean - baselineHeapInterval.mean) / baselineHeapInterval.mean
			return percentChange > SIGNIFICANT_CHANGE_THRESHOLD
		}

	val status: PerformanceStatus
		get() = when {
			hasTimeRegression || hasHeapRegression -> PerformanceStatus.REGRESSION
			!timeInterval.overlaps(baselineTimeInterval) &&
					timeInterval.mean < baselineTimeInterval.mean -> PerformanceStatus.IMPROVEMENT
			else -> PerformanceStatus.SIMILAR
		}
}


enum class PerformanceStatus {
	IMPROVEMENT,
	REGRESSION,
	SIMILAR
}