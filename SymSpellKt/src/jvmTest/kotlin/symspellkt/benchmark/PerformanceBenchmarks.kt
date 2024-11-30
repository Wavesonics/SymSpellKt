package symspellkt.benchmark

import kotlin.test.Test

class PerformanceBenchmarks {
	@Test
	fun `Run Benchmark Suite and generate report`() {
		val results = runFullBenchmarkSuite()
		results.prettyPrint()

		val timestamp = System.currentTimeMillis()
		results.writeResultsToFile("benchmark_results/benchmark-results-$timestamp.json")
	}
}
