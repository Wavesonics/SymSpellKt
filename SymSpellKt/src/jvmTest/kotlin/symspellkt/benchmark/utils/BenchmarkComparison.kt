package symspellkt.benchmark.utils

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class BenchmarkComparison(
	val name: String,
	val parameters: Map<String, String>,
	val timeChange: Double, // Percentage change
	val heapMemoryChange: Double,
	val nonHeapMemoryChange: Double,
	val baselineResult: BenchmarkResult,
	val currentResult: BenchmarkResult
) {
	val hasSignificantChange: Boolean
		get() = abs(timeChange) > 15.0 || // More than this % change in time
				abs(heapMemoryChange) > 20.0 // More than this % change in memory

	val status: PerformanceStatus
		get() = when {
			!hasSignificantChange -> PerformanceStatus.SIMILAR
			timeChange > 0 -> PerformanceStatus.REGRESSION
			else -> PerformanceStatus.IMPROVEMENT
		}
}

enum class PerformanceStatus {
	IMPROVEMENT,
	REGRESSION,
	SIMILAR
}