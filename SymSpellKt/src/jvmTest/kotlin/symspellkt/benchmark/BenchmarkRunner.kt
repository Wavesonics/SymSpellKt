package symspellkt.benchmark

class BenchmarkRunner {
	private val stopWatch = StopWatch()
	private val memoryProfiler = MemoryProfiler()

	fun runBenchmark(
		name: String,
		iterations: Int = 1,
		warmupIterations: Int = 0,
		parameters: Map<String, String> = emptyMap(),
		benchmark: () -> Unit
	): BenchmarkResult {
		println()
		println("--- Executing $name")
		println("Dictionary: ${parameters["dictionaryFile"]}")
		println()

		// Do warmup runs
		repeat(warmupIterations) {
			println("Executing warmup run ${it + 1}/$warmupIterations...")
			benchmark()
		}

		println("Warmup complete. Starting benchmark measurements...")

		val times = mutableListOf<Double>()
		val heapUsages = mutableListOf<Double>()
		val nonHeapUsages = mutableListOf<Double>()

		repeat(iterations) {
			println("Executing benchmark run ${it + 1}/$iterations...")

			System.gc()
			Thread.sleep(100)

			memoryProfiler.beforeIteration()
			stopWatch.start()

			benchmark()

			stopWatch.stop()
			val (heapUsage, nonHeapUsage) = memoryProfiler.afterIteration()

			times.add(stopWatch.getTime().toDouble())
			heapUsages.add(heapUsage)
			nonHeapUsages.add(nonHeapUsage)

			stopWatch.reset()
		}

		println()

		return BenchmarkResult(
			name = name,
			averageTimeMs = times.average(),
			standardDeviationMs = times.standardDeviation(),
			totalRuns = iterations,
			heapMemoryUsageMB = heapUsages.average(),
			heapMemoryStdDevMB = heapUsages.standardDeviation(),
			nonHeapMemoryUsageMB = nonHeapUsages.average(),
			nonHeapMemoryStdDevMB = nonHeapUsages.standardDeviation(),
			parameters = parameters
		)
	}
}