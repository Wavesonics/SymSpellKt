package symspellkt.benchmark.utils

enum class PerformanceStatus {
	IMPROVEMENT,
	SIMILAR,
	MINOR_REGRESSION,
	MAJOR_REGRESSION
}

data class BenchmarkComparison(
	val name: String,
	val parameters: Map<String, String>,
	val timeInterval: ConfidenceInterval,
	val baselineTimeInterval: ConfidenceInterval,
	val heapInterval: ConfidenceInterval,
	val baselineHeapInterval: ConfidenceInterval,
	val minorRegressionThreshold: Double,
	val majorRegressionThreshold: Double,
) {
	val timeRegressionLevel: PerformanceStatus
		get() {
			if (timeInterval.overlaps(baselineTimeInterval)) return PerformanceStatus.SIMILAR

			val percentChange = (timeInterval.mean - baselineTimeInterval.mean) / baselineTimeInterval.mean
			return when {
				percentChange > majorRegressionThreshold -> PerformanceStatus.MAJOR_REGRESSION
				percentChange > minorRegressionThreshold -> PerformanceStatus.MINOR_REGRESSION
				percentChange < 0 -> PerformanceStatus.IMPROVEMENT
				else -> PerformanceStatus.SIMILAR
			}
		}

	val heapRegressionLevel: PerformanceStatus
		get() {
			if (heapInterval.overlaps(baselineHeapInterval)) return PerformanceStatus.SIMILAR

			val percentChange = (heapInterval.mean - baselineHeapInterval.mean) / baselineHeapInterval.mean
			return when {
				percentChange > majorRegressionThreshold -> PerformanceStatus.MAJOR_REGRESSION
				percentChange > minorRegressionThreshold -> PerformanceStatus.MINOR_REGRESSION
				percentChange < 0 -> PerformanceStatus.IMPROVEMENT
				else -> PerformanceStatus.SIMILAR
			}
		}

	val status: PerformanceStatus
		get() = maxOf(timeRegressionLevel, heapRegressionLevel)
}