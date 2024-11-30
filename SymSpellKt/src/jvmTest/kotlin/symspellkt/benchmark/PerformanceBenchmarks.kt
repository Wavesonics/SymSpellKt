package symspellkt.benchmark

import kotlin.test.Ignore
import kotlin.test.Test

class PerformanceBenchmarks {

	@Ignore("Run this manually")
	@Test
	fun `Run Benchmark Suite and generate report`() {
		val results = runFullBenchmarkSuite()
		results.prettyPrint()

		val timestamp = System.currentTimeMillis()
		results.writeResultsToFile("benchmark_results/benchmark-results-$timestamp.json")
	}

	@Ignore("Run this manually")
	@Test
	fun `Create new benchmark baseline`() {
		val results = runFullBenchmarkSuite()
		results.prettyPrint()

		results.writeResultsToFile("benchmark_results/baseline-benchmark")
	}
}
