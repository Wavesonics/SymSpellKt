package symspellkt.benchmark

import kotlinx.serialization.json.Json
import symspellkt.benchmark.utils.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
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

		val comparisons = mutableListOf<BenchmarkComparison>()

		// Compare search results
		cleanBaseline.searchResults.forEach { baseline ->
			cleanCurrent.searchResults
				.find { it.hasSameParameters(baseline) }
				?.let { current ->
					comparisons.add(compareResults(baseline, current))
				}
		}

		// Compare index results
		cleanBaseline.indexResults.forEach { baseline ->
			cleanCurrent.indexResults
				.find { it.hasSameParameters(baseline) }
				?.let { current ->
					comparisons.add(compareResults(baseline, current))
				}
		}

		// Print each comparison
		comparisons.forEach { comparison ->
			printComparisonReport(comparison)
		}

		// Summary stats
		printResultsSummary(comparisons)
	}

	@Test
	fun testOverlap() {
		val interval1 = ConfidenceInterval(0.28, 0.20, 0.32)
		val interval2 = ConfidenceInterval(0.49, 0.32, 0.67)

		assertTrue(interval1.overlaps(interval2))
	}

	@Test
	fun testOverlapEqual() {
		val interval1 = ConfidenceInterval(0.49, 0.33, 0.66)
		val interval2 = ConfidenceInterval(0.49, 0.32, 0.67)

		assertTrue(interval1.overlaps(interval2))
	}

	private fun printResultsSummary(comparisons: MutableList<BenchmarkComparison>) {
		val regressions = comparisons.count { it.status == PerformanceStatus.REGRESSION }
		val improvements = comparisons.count { it.status == PerformanceStatus.IMPROVEMENT }
		val similar = comparisons.count { it.status == PerformanceStatus.SIMILAR }

		println("\n=== SUMMARY ===")
		println("Total Comparisons: ${comparisons.size}")
		println("Regressions: $regressions")
		println("Improvements: $improvements")
		println("Similar: $similar")

		// Fail the test if there are significant regressions
		val hasRegressions = comparisons.any { it.status == PerformanceStatus.REGRESSION }
		if (hasRegressions) {
			val regressionDetails = comparisons
				.filter { it.status == PerformanceStatus.REGRESSION }
				.joinToString("\n") { comparison ->
					"""
	                |${comparison.name} (${comparison.parameters}):
	                |  Time: ${comparison.baselineTimeInterval} -> ${comparison.timeInterval} ms
	                |  Heap: ${comparison.baselineHeapInterval} -> ${comparison.heapInterval} MB
	                """.trimMargin()
				}
			fail("Performance regressions detected:\n$regressionDetails")
		}
	}

	// Helper function to compare parameters
	private fun BenchmarkResult.hasSameParameters(other: BenchmarkResult): Boolean {
		return name == other.name && parameters == other.parameters
	}

	fun compareResults(baseline: BenchmarkResult, current: BenchmarkResult): BenchmarkComparison {
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
			)
		)
	}

	fun printComparisonReport(comparison: BenchmarkComparison) {
		println("\n=== Performance Comparison ===")
		println("Benchmark: ${comparison.name}")
		println("Parameters: ${comparison.parameters}")
		println("\nExecution Time:")
		println("  Baseline: ${comparison.baselineTimeInterval} ms")
		println("  Current:  ${comparison.timeInterval} ms")
		println("  Status:   ${if (comparison.hasTimeRegression) "REGRESSION" else "OK"}")

		println("\nHeap Memory:")
		println("  Baseline: ${comparison.baselineHeapInterval} MB")
		println("  Current:  ${comparison.heapInterval} MB")
		println("  Status:   ${if (comparison.hasHeapRegression) "REGRESSION" else "OK"}")

		println("\nOverall Status: ${comparison.status}")
	}
}
