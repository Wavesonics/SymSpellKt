package symspellkt.benchmark

import kotlinx.serialization.json.Json
import symspellkt.benchmark.utils.*
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.fail

class PerformanceRegressionBenchmarks {
	/**
	 * This will compare a benchmark results json file, with a new fresh run of the benchmark suite.
	 * To generate the baseline file, run the [PerformanceBenchmarks] "Run Benchmark Suite and generate report"
	 * test. It will generate a new file in /benchmark_results in the form of "benchmark-results-1732922410366.json"
	 * Rename that file to "baseline-benchmark.json"
	 *
	 * Now make your changes or refactors to the code, and when you're done, run this test [compareToBaseline]
	 * to see how the code changes effected performance.
	 *
	 * Of course this is wildly dependent on the particular machine and conditions the report was generated under,
	 * for the comparison to have any relevance.
	 */
	//@Ignore("Run this manually")
	@Test
	fun compareToBaseline() {
		val baselineFile = File("benchmark_results/baseline-benchmark.json")
		if (!baselineFile.exists()) {
			println("No baseline file found at ${baselineFile.absolutePath}. Skipping comparison.")
			return
		}

		val json = Json {
			prettyPrint = true
			ignoreUnknownKeys = true
		}
		val baselineResults = json.decodeFromString<BenchmarkResults>(baselineFile.readText())

		val currentResults = runFullBenchmarkSuite()

		// Remove outliers before comparison
		val cleanBaseline = baselineResults.removeOutliers()
		val cleanCurrent = currentResults.removeOutliers()
		val comparisons = compareResults(cleanBaseline, cleanCurrent)

		// Print each comparison
		comparisons.forEach { comparison ->
			printComparisonReport(comparison)
		}

		printResultsSummary(comparisons)
	}

	private fun printResultsSummary(comparisons: List<BenchmarkComparison>) {
		val majorRegressions = comparisons.count { it.status == PerformanceStatus.MAJOR_REGRESSION }
		val minorRegressions = comparisons.count { it.status == PerformanceStatus.MINOR_REGRESSION }
		val improvements = comparisons.count { it.status == PerformanceStatus.IMPROVEMENT }
		val similar = comparisons.count { it.status == PerformanceStatus.SIMILAR }

		println("\n=== SUMMARY ===")
		println("Total Comparisons: ${comparisons.size}")
		println("Major Regressions: $majorRegressions")
		println("Minor Regressions: $minorRegressions")
		println("Improvements: $improvements")
		println("Similar: $similar")

		// Only fail on major regressions
		val hasMajorRegressions = comparisons.any { it.status == PerformanceStatus.MAJOR_REGRESSION }
		if (hasMajorRegressions) {
			val regressionDetails = comparisons
				.filter { it.status == PerformanceStatus.MAJOR_REGRESSION }
				.joinToString("\n") { comparison ->
					val timeChange = (comparison.timeInterval.mean - comparison.baselineTimeInterval.mean) /
							comparison.baselineTimeInterval.mean * 100
					val heapChange = (comparison.heapInterval.mean - comparison.baselineHeapInterval.mean) /
							comparison.baselineHeapInterval.mean * 100

					"""
                |${comparison.name} (${comparison.parameters}):
                |  Time: ${comparison.baselineTimeInterval} -> ${comparison.timeInterval} ms
				|  Change: ${sign(timeChange)}${String.format("%.1f%%", timeChange)}
                |  Heap: ${comparison.baselineHeapInterval} -> ${comparison.heapInterval} MB
				|  Change: ${sign(heapChange)}${String.format("%.1f%%", heapChange)}
                """.trimMargin()
				}
			fail("Major performance regressions detected:\n$regressionDetails")
		}
	}

	private fun sign(percent: Double): String = if(percent >= 0.0) "+" else ""

	private fun compareResult(
		baseline: BenchmarkResult,
		current: BenchmarkResult,
		minorRegressionThreshold: Double,
		majorRegressionThreshold: Double
	): BenchmarkComparison {
		return BenchmarkComparison(
			name = current.name,
			parameters = current.parameters,
			timeInterval = calculateConfidenceInterval(
				current.averageTimeMs,
				current.standardDeviationMs,
				current.totalRuns
			),
			baselineTimeInterval = calculateConfidenceInterval(
				baseline.averageTimeMs,
				baseline.standardDeviationMs,
				baseline.totalRuns
			),
			heapInterval = calculateConfidenceInterval(
				current.heapMemoryUsageMB,
				current.heapMemoryStdDevMB,
				current.totalRuns
			),
			baselineHeapInterval = calculateConfidenceInterval(
				baseline.heapMemoryUsageMB,
				baseline.heapMemoryStdDevMB,
				baseline.totalRuns
			),
			minorRegressionThreshold = minorRegressionThreshold,
			majorRegressionThreshold = majorRegressionThreshold,
		)
	}

	private fun compareResults(baseline: BenchmarkResults, current: BenchmarkResults): List<BenchmarkComparison> {
		val comparisons = mutableListOf<BenchmarkComparison>()

		// Compare search results
		baseline.searchResults.forEach { baselineResult ->
			current.searchResults
				.find { it.hasSameParameters(baselineResult) }
				?.let { currentResult ->
					comparisons.add(compareResult(baselineResult, currentResult, 0.01, 0.05))
				}
		}

		// Compare index results
		baseline.indexResults.forEach { baselineResult ->
			current.indexResults
				.find { it.hasSameParameters(baselineResult) }
				?.let { currentResult ->
					comparisons.add(compareResult(baselineResult, currentResult, 0.10, 0.25))
				}
		}

		return comparisons
	}

	private fun BenchmarkResult.hasSameParameters(other: BenchmarkResult): Boolean {
		return name == other.name && parameters == other.parameters
	}

	private fun printComparisonReport(comparison: BenchmarkComparison) {
		println("\n=== Performance Comparison ===")
		println("Benchmark: ${comparison.name}")
		println("Parameters: ${comparison.parameters}")

		val timeChange = (comparison.timeInterval.mean - comparison.baselineTimeInterval.mean) /
				comparison.baselineTimeInterval.mean * 100
		val heapChange = (comparison.heapInterval.mean - comparison.baselineHeapInterval.mean) /
				comparison.baselineHeapInterval.mean * 100

		println("\nExecution Time:")
		println("  Baseline: ${comparison.baselineTimeInterval} ms")
		println("  Current:  ${comparison.timeInterval} ms")
		println("  Change:   ${sign(timeChange)}${String.format("%.1f%%", timeChange)}")
		println("  Status:   ${comparison.timeRegressionLevel}")

		println("\nHeap Memory:")
		println("  Baseline: ${comparison.baselineHeapInterval} MB")
		println("  Current:  ${comparison.heapInterval} MB")
		println("  Change:   ${sign(timeChange)}${String.format("%.1f%%", heapChange)}")
		println("  Status:   ${comparison.heapRegressionLevel}")

		println("\nOverall Status: ${comparison.status}")
	}
}
