package symspellkt.benchmark

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
}