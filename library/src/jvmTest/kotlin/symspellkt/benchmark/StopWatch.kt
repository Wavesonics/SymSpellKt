package symspellkt.benchmark

class StopWatch {
	private var startTime: Long = 0
	private var stopTime: Long = 0

	fun start() {
		startTime = System.currentTimeMillis()
	}

	fun stop() {
		stopTime = System.currentTimeMillis()
	}

	fun reset() {
		startTime = 0
		stopTime = 0
	}

	fun getTime(): Long {
		return if(startTime == 0L) {
			0L
		} else {
			stopTime - startTime
		}
	}
}