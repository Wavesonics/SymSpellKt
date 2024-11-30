package symspellkt.benchmark

import kotlinx.serialization.json.Json
import symspellkt.benchmark.utils.*
import java.io.File
import kotlin.math.absoluteValue
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
		val comparisons = compareResults(baselineResults.removeOutliers(), currentResults.removeOutliers())

		printComparisonReport(comparisons)

		// Fail the test if there are significant regressions
		val hasRegressions = comparisons.any {
			it.status == PerformanceStatus.REGRESSION
		}
		if (hasRegressions) {
			fail("Performance regressions detected. See report above for details.")
		}
	}

	private fun compareResults(
		baseline: BenchmarkResults,
		current: BenchmarkResults
	): List<BenchmarkComparison> {
		val comparisons = mutableListOf<BenchmarkComparison>()

		// Compare search results
		baseline.searchResults.forEach { baselineResult ->
			current.searchResults
				.find { it.hasSameParameters(baselineResult) }
				?.let { currentResult ->
					comparisons.add(createComparison(baselineResult, currentResult))
				}
		}

		// Compare index results
		baseline.indexResults.forEach { baselineResult ->
			current.indexResults
				.find { it.hasSameParameters(baselineResult) }
				?.let { currentResult ->
					comparisons.add(createComparison(baselineResult, currentResult))
				}
		}

		return comparisons
	}

	private fun createComparison(
		baseline: BenchmarkResult,
		current: BenchmarkResult
	): BenchmarkComparison {
		return BenchmarkComparison(
			name = current.name,
			parameters = current.parameters,
			timeChange = calculatePercentageChange(baseline.averageTimeMs, current.averageTimeMs),
			heapMemoryChange = calculatePercentageChange(baseline.heapMemoryUsageMB, current.heapMemoryUsageMB),
			nonHeapMemoryChange = calculatePercentageChange(baseline.nonHeapMemoryUsageMB, current.nonHeapMemoryUsageMB),
			baselineResult = baseline,
			currentResult = current
		)
	}

	private fun calculatePercentageChange(baseline: Double, current: Double): Double {
		return ((current - baseline) / baseline) * 100
	}

	private fun printComparisonReport(comparisons: List<BenchmarkComparison>) {
		println("\n=== PERFORMANCE COMPARISON REPORT ===")
		println("Comparing ${comparisons.size} benchmarks against baseline")
		println("=" .repeat(80))

		comparisons.groupBy { it.status }.forEach { (status, results) ->
			println("\n${status.name} (${results.size} benchmarks):")
			println("-".repeat(40))

			results.forEach { comparison ->
				println("\n${comparison.name} (${comparison.parameters})")
				printf("  Time: %.2f ms → %.2f ms (%.1f%% %s)\n",
					comparison.baselineResult.averageTimeMs,
					comparison.currentResult.averageTimeMs,
					comparison.timeChange.absoluteValue,
					if (comparison.timeChange > 0) "slower" else "faster"
				)
				printf("  Heap: %.2f MB → %.2f MB (%.1f%% %s)\n",
					comparison.baselineResult.heapMemoryUsageMB,
					comparison.currentResult.heapMemoryUsageMB,
					comparison.heapMemoryChange.absoluteValue,
					if (comparison.heapMemoryChange > 0) "increase" else "decrease"
				)
			}
		}

		// Summary statistics
		val regressions = comparisons.count { it.status == PerformanceStatus.REGRESSION }
		val improvements = comparisons.count { it.status == PerformanceStatus.IMPROVEMENT }
		val similar = comparisons.count { it.status == PerformanceStatus.SIMILAR }

		println("\n=== SUMMARY ===")
		println("Total Benchmarks: ${comparisons.size}")
		println("Regressions: $regressions")
		println("Improvements: $improvements")
		println("Similar: $similar")
	}
}

// Extension function to compare benchmark parameters
private fun BenchmarkResult.hasSameParameters(other: BenchmarkResult): Boolean {
	return name == other.name && parameters == other.parameters
}