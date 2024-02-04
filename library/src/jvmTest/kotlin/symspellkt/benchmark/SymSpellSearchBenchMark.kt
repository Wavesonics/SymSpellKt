package symspellkt.benchmark

import com.darkrockstudios.symspellkt.api.CharDistance
import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.*
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

@State(Scope.Benchmark)
class SymSpellSearchBenchMark {
	@Param("TOP", "CLOSEST", "ALL")
	private lateinit var verbosity: String

	@Param("1.0d", "2.0d", "3.0d")
	var maxEditDistance: Double = 0.0

	@Param(
		"frequency_dictionary_en_30_000.txt",
		"frequency_dictionary_en_82_765.txt",
		"frequency_dictionary_en_500_000.txt"
	)
	var dataFile: String? = null

	var queryFile: String = "noisy_query_en_1000.txt"
	var queries: List<String> = readQueries(queryFile)
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
			getStringDistance(spellCheckSettings, null),
			spellCheckSettings
		)
		indexData(dataFile, dataHolder)
		System.out.println(
			" DataHolder Indexed Size " + dataHolder.size
		)
	}


	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Measurement(iterations = 1)
	@Throws(
		SpellCheckException::class
	)
	fun searchBenchmark() {
		for (query in queries) {
			totalMatches += spellChecker.lookup(query, Verbosity.valueOf(verbosity), maxEditDistance)
				.size
		}
	}

	@TearDown(Level.Iteration)
	fun tearDown() {

	}

	private fun getStringDistance(
		spellCheckSettings: SpellCheckSettings,
		charDistance: CharDistance?
	): StringDistance {
		return WeightedDamerauLevenshteinDistance(
			spellCheckSettings.deletionWeight,
			spellCheckSettings.insertionWeight,
			spellCheckSettings.replaceWeight,
			spellCheckSettings.transpositionWeight,
			charDistance
		)
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

	private fun readQueries(queryFile: String): List<String> {
		val queries: MutableList<String> = ArrayList()
		try {
			val queryResourceUrl = this.javaClass.classLoader.getResource(queryFile)
			val qparser: CSVParser = CSVParser
				.parse(
					queryResourceUrl, Charset.forName("UTF-8"),
					CSVFormat.DEFAULT.withDelimiter(' ')
				)
			val csvIterator: Iterator<CSVRecord> = qparser.iterator()
			while (csvIterator.hasNext()) {
				val csvRecord: CSVRecord = csvIterator.next()
				queries.add(csvRecord.get(0))
			}
		} catch (ex: IOException) {
			System.err.println("Error occurred $ex")
		}
		return queries
	}

	@Test
	@Throws(RunnerException::class, IOException::class)
	fun testBenchmarkSearch() {
		val file = checkFileAndCreate(SymSpellSearchBenchMark::class.java.simpleName)
		val opt: Options = OptionsBuilder()
			.include(SymSpellSearchBenchMark::class.java.simpleName)
			.addProfiler(MemoryProfiler::class.java.name)
			.resultFormat(ResultFormatType.JSON)
			.result(file.absolutePath)
			.warmupIterations(0)
			.measurementIterations(1)
			.forks(1)
			.build()
		Runner(opt).run()
		println("Total Lookup results instance $totalMatches")
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
		private var totalMatches: Long = 0
	}
}
