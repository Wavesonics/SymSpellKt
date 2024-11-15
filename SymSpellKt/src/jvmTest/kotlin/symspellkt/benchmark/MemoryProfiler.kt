package symspellkt.benchmark

import org.openjdk.jmh.infra.BenchmarkParams
import org.openjdk.jmh.infra.IterationParams
import org.openjdk.jmh.profile.InternalProfiler
import org.openjdk.jmh.results.AggregationPolicy
import org.openjdk.jmh.results.IterationResult
import org.openjdk.jmh.results.Result
import org.openjdk.jmh.results.ScalarResult
import org.openjdk.jmh.runner.Defaults
import java.lang.management.ManagementFactory

class MemoryProfiler : InternalProfiler {
	override fun getDescription(): String {
		return "memory heap profiler"
	}

	override fun beforeIteration(benchmarkParams: BenchmarkParams?, iterationParams: IterationParams?) {
	}

	override fun afterIteration(
		bp: BenchmarkParams?, ip: IterationParams?,
		result: IterationResult?
	): Collection<Result<*>?> {
		val heapUsage = ManagementFactory.getMemoryMXBean().heapMemoryUsage
		val nonheapUsage = ManagementFactory.getMemoryMXBean().nonHeapMemoryUsage

		val results: MutableCollection<ScalarResult?> = ArrayList<ScalarResult?>()
		results.add(
			ScalarResult(
				Defaults.RESULT_FILE_PREFIX + "mem.heap", heapUsage.used / (1024 * 1024.0), "MB",
				AggregationPolicy.MAX
			)
		)
		results.add(
			ScalarResult(
				Defaults.RESULT_FILE_PREFIX + "mem.nonheap", nonheapUsage.used / (1024 * 1024.0), "MB",
				AggregationPolicy.MAX
			)
		)

		return results
	}
}