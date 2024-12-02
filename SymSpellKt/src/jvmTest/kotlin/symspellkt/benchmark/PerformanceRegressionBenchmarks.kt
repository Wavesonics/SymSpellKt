package symspellkt.benchmark

import kotlinx.serialization.json.Json
import symspellkt.benchmark.utils.*
import java.io.File
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
	//@Ignore
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

		val currentResults = runFullBenchmarkSuite(true, false)

		// Remove outliers before comparison
		val cleanBaseline = baselineResults.removeOutliers()
		val cleanCurrent = currentResults.removeOutliers()
		val comparisons = compareResults(cleanBaseline, cleanCurrent)

		// Print each comparison
		val comparisonReports: String = comparisons.joinToString { comparison ->
			buildComparisonReport(comparison)
		}

		val summary = buildResultsSummary(comparisons)

		printComparisonReport(comparisonReports, summary)

		val timestamp = System.currentTimeMillis()
		writeResultsToFile(comparisonReports, summary, "benchmark_results/benchmark-comparison-$timestamp.txt")
	}

	private fun writeResultsToFile(comparisonReports: String, summary: String, outputPath: String) {
		val text = StringBuilder(comparisonReports.length + summary.length + 1)
			.append(comparisonReports)
			.append('\n')
			.append(summary)
			.toString()

		val outFile = File(outputPath)
		outFile.parentFile.mkdirs()
		outFile.writeText(text)
		println("Benchmark Comparison results written to ${outFile.absolutePath}")
	}

	private fun buildResultsSummary(comparisons: List<BenchmarkComparison>): String {
		val sb = StringBuilder()

		val majorTimeRegressions = comparisons.count { it.timeRegressionLevel == PerformanceStatus.MAJOR_REGRESSION }
		val minorTimeRegressions = comparisons.count { it.timeRegressionLevel == PerformanceStatus.MINOR_REGRESSION }
		val timeImprovements = comparisons.count { it.timeRegressionLevel == PerformanceStatus.IMPROVEMENT }
		val timeSimilar = comparisons.count { it.timeRegressionLevel == PerformanceStatus.SIMILAR }

		val majorHeapRegressions = comparisons.count { it.heapRegressionLevel == PerformanceStatus.MAJOR_REGRESSION }
		val minorHeapRegressions = comparisons.count { it.heapRegressionLevel == PerformanceStatus.MINOR_REGRESSION }
		val heapImprovements = comparisons.count { it.heapRegressionLevel == PerformanceStatus.IMPROVEMENT }
		val heapSimilar = comparisons.count { it.heapRegressionLevel == PerformanceStatus.SIMILAR }

		sb.append("\n=== SUMMARY ===\n")
			.append("Total Comparisons: ").append(comparisons.size).append("\n\n")
			.append("Time Performance:\n")
			.append("  Major Regressions: ").append(majorTimeRegressions).append('\n')
			.append("  Minor Regressions: ").append(minorTimeRegressions).append('\n')
			.append("  Improvements: ").append(timeImprovements).append('\n')
			.append("  Similar: ").append(timeSimilar).append("\n\n")
			.append("Heap Performance:\n")
			.append("  Major Regressions: ").append(majorHeapRegressions).append('\n')
			.append("  Minor Regressions: ").append(minorHeapRegressions).append('\n')
			.append("  Improvements: ").append(heapImprovements).append('\n')
			.append("  Similar: ").append(heapSimilar)

		// Handle major regressions
		val hasMajorRegressions = comparisons.any { it.status == PerformanceStatus.MAJOR_REGRESSION }
		if (hasMajorRegressions) {
			sb.append("\n\nMajor performance regressions detected:\n")

			comparisons
				.filter { it.status == PerformanceStatus.MAJOR_REGRESSION }
				.forEach { comparison ->
					val timeChange = (comparison.timeInterval.mean - comparison.baselineTimeInterval.mean) /
							comparison.baselineTimeInterval.mean * 100
					val heapChange = (comparison.heapInterval.mean - comparison.baselineHeapInterval.mean) /
							comparison.baselineHeapInterval.mean * 100

					sb.append(comparison.name)
						.append(" (")
						.append(comparison.parameters)
						.append("):\n  Time: ")
						.append(comparison.baselineTimeInterval)
						.append(" -> ")
						.append(comparison.timeInterval)
						.append(" ms\n  Change: ")
						.append(sign(timeChange))
						.append(String.format("%.1f%%", timeChange))
						.append("\n  Heap: ")
						.append(comparison.baselineHeapInterval)
						.append(" -> ")
						.append(comparison.heapInterval)
						.append(" MB\n  Change: ")
						.append(sign(heapChange))
						.append(String.format("%.1f%%", heapChange))
						.append('\n')
				}
		}

		val result = sb.toString()

		// If there are major regressions, throw the failure
		if (hasMajorRegressions) {
			fail(result)
		}

		return result
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

	private fun buildComparisonReport(comparison: BenchmarkComparison): String {
		val sb = StringBuilder()

		// Calculate changes
		val timeChange = (comparison.timeInterval.mean - comparison.baselineTimeInterval.mean) /
				comparison.baselineTimeInterval.mean * 100
		val heapChange = (comparison.heapInterval.mean - comparison.baselineHeapInterval.mean) /
				comparison.baselineHeapInterval.mean * 100

		// Build report
		sb.append("\n=== Performance Comparison ===\n")
			.append("Benchmark: ").append(comparison.name).append('\n')
			.append("Parameters: ").append(comparison.parameters).append("\n\n")

			.append("Execution Time:\n")
			.append("  Baseline: ").append(comparison.baselineTimeInterval).append(" ms\n")
			.append("  Current:  ").append(comparison.timeInterval).append(" ms\n")
			.append("  Change:   ").append(sign(timeChange))
			.append(String.format("%.1f%%", timeChange)).append('\n')
			.append("  Status:   ").append(comparison.timeRegressionLevel).append("\n\n")

			.append("Heap Memory:\n")
			.append("  Baseline: ").append(comparison.baselineHeapInterval).append(" MB\n")
			.append("  Current:  ").append(comparison.heapInterval).append(" MB\n")
			.append("  Change:   ").append(sign(timeChange))
			.append(String.format("%.1f%%", heapChange)).append('\n')
			.append("  Status:   ").append(comparison.heapRegressionLevel).append("\n\n")

			.append("Overall Status: ").append(comparison.status).append('\n')

		return sb.toString()
	}

	private fun printComparisonReport(comparisonReports: String, summary: String) {
		println(comparisonReports)
		println(summary)
	}
}
