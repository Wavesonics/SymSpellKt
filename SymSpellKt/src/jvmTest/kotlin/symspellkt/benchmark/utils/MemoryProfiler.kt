package symspellkt.benchmark.utils

import java.lang.management.ManagementFactory

class MemoryProfiler {
	private val memoryBean = ManagementFactory.getMemoryMXBean()
	var lastHeapUsageMB: Double = 0.0
		private set
	var lastNonHeapUsageMB: Double = 0.0
		private set

	fun beforeIteration() {
		System.gc() // Optional: force GC before measurement
	}

	fun afterIteration(): Pair<Double, Double> {
		val heapUsage = memoryBean.heapMemoryUsage
		val nonHeapUsage = memoryBean.nonHeapMemoryUsage

		lastHeapUsageMB = heapUsage.used / (1024.0 * 1024.0)
		lastNonHeapUsageMB = nonHeapUsage.used / (1024.0 * 1024.0)

		return lastHeapUsageMB to lastNonHeapUsageMB
	}
}