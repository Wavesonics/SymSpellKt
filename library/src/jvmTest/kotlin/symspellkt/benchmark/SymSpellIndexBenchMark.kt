package symspellkt.benchmark

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.stringdistance.DamerauLevenshteinDistance
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDataHolder
import com.darkrockstudios.symspellkt.impl.SymSpellCheck
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.junit.Test
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.RunnerException
import org.openjdk.jmh.runner.options.Options
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore

@State(Scope.Benchmark)
class SymSpellIndexBenchMark {
	@Param("1.0d", "2.0d", "3.0d")
	var maxEditDistance: Double = 0.0

	@Param(
		"frequency_dictionary_en_30_000.txt",
		"frequency_dictionary_en_82_765.txt",
		"frequency_dictionary_en_500_000.txt"
	)
	var dataFile: String? = null

	lateinit var spellChecker: SpellChecker

	@Setup(Level.Iteration)
	@Throws(SpellCheckException::class, IOException::class)
	fun setup() {
		val spellCheckSettings = SpellCheckSettings(
			maxEditDistance = maxEditDistance
		)

		val dataHolder: DataHolder = InMemoryDataHolder(
			spellCheckSettings,
			Murmur3HashFunction()
		)

		spellChecker = SymSpellCheck(
			dataHolder,
			DamerauLevenshteinDistance(),
			spellCheckSettings
		)
	}

	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
	@Throws(
		SpellCheckException::class, IOException::class, InterruptedException::class
	)
	fun searchBenchmark() {
		indexData(dataFile, spellChecker.dataHolder)
		println(" DataHolder Indexed Size " + spellChecker.dataHolder.size)
		Thread.sleep(10000)
	}

	@TearDown(Level.Iteration)
	fun tearDown() {
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun indexData(dataResourceName: String?, dataHolder: DataHolder) {
		val resourceUrl = this.javaClass.classLoader.getResource(dataResourceName)
		val parser: CSVParser = CSVParser
			.parse(resourceUrl, Charset.forName("UTF-8"), CSVFormat.DEFAULT.withDelimiter(' '))
		val csvIterator: Iterator<CSVRecord> = parser.iterator()
		while (csvIterator.hasNext()) {
			val csvRecord: CSVRecord = csvIterator.next()
			dataHolder
				.addItem(DictionaryItem(csvRecord.get(0), csvRecord.get(1).toDouble(), 0.0))
		}
	}

	@Ignore("KAPT isn't working in KMP projects yet")
	@Test
	@Throws(RunnerException::class, IOException::class)
	fun testBenchmarkIndex() {
		val file = checkFileAndCreate(SymSpellIndexBenchMark::class.java.simpleName)
		val opt: Options = OptionsBuilder()
			.include(this::class.java.simpleName)
			.addProfiler(MemoryProfiler::class.java.name)
			.resultFormat(ResultFormatType.JSON)
			.result(file.absolutePath)
			.warmupIterations(0)
			.measurementIterations(1)
			.forks(1)
			.build()
		Runner(opt).run()
		println("Total Lookup results instance " + totalMatches)
	}

	@Throws(IOException::class)
	private fun checkFileAndCreate(name: String): File {
		val targetFolderPath = Paths.get(
			this.javaClass.getResource("/")!!.file.substring(1)
		).parent.toString() + "/benchmark-result/"

		val targetFolder = File(targetFolderPath)
		targetFolder.mkdirs()

		val file = File(
			targetFolder.toString() + name
					+ "_" + System.currentTimeMillis() + ".json"
		)
		if (file.exists()) {
			file.delete()
		}
		file.createNewFile()
		return file
	}

	companion object {
		private const val totalMatches: Long = 0
	}
}
