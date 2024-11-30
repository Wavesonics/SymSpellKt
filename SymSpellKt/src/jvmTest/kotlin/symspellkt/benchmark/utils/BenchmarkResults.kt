package symspellkt.benchmark.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class BenchmarkResults(
	val searchResults: List<BenchmarkResult>,
	val indexResults: List<BenchmarkResult>,
) {
	fun writeResultsToFile(outputPath: String) {
		val json = Json {
			prettyPrint = true
		}
		val outFile = File(outputPath)
		outFile.parentFile.mkdirs()
		outFile.writeText(json.encodeToString(serializer(), this))
		println("Benchmark results written to ${outFile.absolutePath}")
	}

	fun prettyPrint() {
		println("\n=== BENCHMARK RESULTS SUMMARY ===")
		println("Total Search Benchmarks: ${searchResults.size}")
		println("Total Index Benchmarks: ${indexResults.size}")
		println()

		println("=== SEARCH BENCHMARKS ===")
		searchResults.prettyPrintAll()

		println("=== INDEX BENCHMARKS ===")
		indexResults.prettyPrintAll()
	}

	/**
	 * Remove the lowest and highest results
	 */
	fun removeOutliers(): BenchmarkResults {
		return BenchmarkResults(
			searchResults = searchResults.groupBy { it.getConfigKey() }
				.map { (_, results) -> removeOutliersFromGroup(results) }
				.flatten(),
			indexResults = indexResults.groupBy { it.getConfigKey() }
				.map { (_, results) -> removeOutliersFromGroup(results) }
				.flatten()
		)
	}

	private fun removeOutliersFromGroup(results: List<BenchmarkResult>): List<BenchmarkResult> {
		if (results.size <= 2) return results // Need at least 3 results to remove outliers

		// Sort by execution time
		val sortedByTime = results.sortedBy { it.averageTimeMs }

		// Remove first and last element (min and max)
		return sortedByTime.subList(1, sortedByTime.size - 1)
	}
}

private fun BenchmarkResult.getConfigKey(): String {
	return buildString {
		append(name)
		parameters.entries.sortedBy { it.key }.forEach { (key, value) ->
			append("_${key}_${value}")
		}
	}
}