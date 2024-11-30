package symspellkt.benchmark

import com.darkrockstudios.symspellkt.common.Verbosity
import symspellkt.benchmark.utils.BenchmarkResults
import symspellkt.benchmark.utils.SymSpellBenchmarks

fun runFullBenchmarkSuite(): BenchmarkResults {
	val benchmarks = SymSpellBenchmarks()

	val warmupIterations = 2
	val iterations = 10

	// Run search benchmarks
	val verbosities = listOf(Verbosity.Top, Verbosity.Closest, Verbosity.All)
	val maxEditDistances = listOf(1.0, 2.0, 3.0)
	val dictionaries = listOf(
		"frequency_dictionary_en_30_000.txt",
		"frequency_dictionary_en_82_765.txt",
		"frequency_dictionary_en_500_000.txt"
	)

	for (verbosity in verbosities) {
		for (distance in maxEditDistances) {
			for (dict in dictionaries) {
				benchmarks.runSearchBenchmark(
					verbosity = verbosity,
					maxEditDistance = distance,
					dictionaryFile = dict,
					warmupIterations = warmupIterations,
					iterations = iterations,
				)
			}
		}
	}

	// Run index benchmarks
	for (distance in maxEditDistances) {
		for (dict in dictionaries) {
			benchmarks.runIndexBenchmark(
				maxEditDistance = distance,
				dictionaryFile = dict,
				warmupIterations = warmupIterations,
				iterations = iterations,
			)
		}
	}

	// Get and process results
	val results = benchmarks.getResults()
	return results
}