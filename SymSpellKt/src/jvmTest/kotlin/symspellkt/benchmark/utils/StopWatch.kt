package symspellkt.benchmark.utils

class StopWatch {
	private var startTime: Long = 0
	private var stopTime: Long = 0

	fun start() {
		startTime = System.nanoTime()
	}

	fun stop() {
		stopTime = System.nanoTime()
	}

	fun reset() {
		startTime = 0
		stopTime = 0
	}

	// Returns elapsed time in milliseconds with nanosecond precision
	fun getTime(): Double {
		return if(startTime == 0L) {
			0.0
		} else {
			(stopTime - startTime) / 1_000_000.0 // Convert nanoseconds to milliseconds
		}
	}
}