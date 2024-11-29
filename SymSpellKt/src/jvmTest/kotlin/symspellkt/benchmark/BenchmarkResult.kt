package symspellkt.benchmark

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkResult(
	val name: String,
	val averageTimeMs: Double,
	val standardDeviationMs: Double,
	val totalRuns: Int,
	val heapMemoryUsageMB: Double,
	val heapMemoryStdDevMB: Double,
	val nonHeapMemoryUsageMB: Double,
	val nonHeapMemoryStdDevMB: Double,
	val parameters: Map<String, String>
)
