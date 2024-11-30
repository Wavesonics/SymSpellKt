package symspellkt.benchmark.utils

import kotlin.math.sqrt

fun BenchmarkResult.prettyPrint() {
	val divider = "=".repeat(100)
	val subDivider = "-".repeat(50)

	println(divider)
	println("Benchmark: ${name.uppercase()}")
	println(divider)

	println("Performance Metrics:")
	println(subDivider)
	println("Average Time: ${String.format("%.2f", averageTimeMs)}ms ± ${String.format("%.2f", standardDeviationMs)}ms")
	println("Total Runs: $totalRuns")

	println("\nMemory Usage:")
	println(subDivider)
	println("Heap Memory: ${String.format("%.2f", heapMemoryUsageMB)}MB ± ${String.format("%.2f", heapMemoryStdDevMB)}MB")
	println("Non-Heap Memory: ${String.format("%.2f", nonHeapMemoryUsageMB)}MB ± ${String.format("%.2f", nonHeapMemoryStdDevMB)}MB")

	if (parameters.isNotEmpty()) {
		println("\nParameters:")
		println(subDivider)
		parameters.forEach { (key, value) ->
			println("${key.replace(Regex("([A-Z])"), " $1").capitalize()}: $value")
		}
	}

	println(divider + "\n")
}
fun List<BenchmarkResult>.prettyPrintAll() {
	val headerDivider = "*".repeat(120)
	println(headerDivider)
	println("BENCHMARK RESULTS SUMMARY")
	println("Total Benchmarks Run: ${this.size}")
	println(headerDivider + "\n")

	forEach { it.prettyPrint() }

	println("Summary Table:")
	println("=".repeat(140))
	printf("%-20s %-20s %-10s %-20s %-20s\n",
		"Benchmark", "Time (ms)", "Runs", "Heap (MB)", "Non-Heap (MB)")
	println("-".repeat(140))

	forEach {
		printf("%-20s %-20s %-10d %-20s %-20s\n",
			it.name,
			"${String.format("%.2f", it.averageTimeMs)} ± ${String.format("%.2f", it.standardDeviationMs)}",
			it.totalRuns,
			"${String.format("%.2f", it.heapMemoryUsageMB)} ± ${String.format("%.2f", it.heapMemoryStdDevMB)}",
			"${String.format("%.2f", it.nonHeapMemoryUsageMB)} ± ${String.format("%.2f", it.nonHeapMemoryStdDevMB)}")
	}
	println("=".repeat(140))
}

internal fun printf(format: String, vararg args: Any) {
	println(String.format(format, *args))
}

// Extension function to calculate standard deviation
internal fun List<Double>.standardDeviation(): Double {
	val mean = this.average()
	val variance = this.map { (it - mean) * (it - mean) }.average()
	return sqrt(variance)
}