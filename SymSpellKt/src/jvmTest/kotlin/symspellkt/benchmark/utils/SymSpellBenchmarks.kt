package symspellkt.benchmark.utils

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.DamerauLevenshteinDistance
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDictionaryHolder
import com.darkrockstudios.symspellkt.impl.SymSpell
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

class SymSpellBenchmarks {
	private val searchResults = mutableListOf<BenchmarkResult>()
	private val indexResults = mutableListOf<BenchmarkResult>()

	private val benchmarkRunner = BenchmarkRunner()

	fun runSearchBenchmark(
		verbosity: Verbosity,
		maxEditDistance: Double,
		dictionaryFile: String,
		queryFile: String = "noisy_query_en_1000.txt",
		iterations: Int,
		warmupIterations: Int,
	) {
		val params = mapOf(
			"verbosity" to verbosity.name,
			"maxEditDistance" to maxEditDistance.toString(),
			"dictionaryFile" to dictionaryFile,
			"queryFile" to queryFile
		)

		val spellChecker = createSpellChecker(maxEditDistance)
		val queries = readQueries(queryFile)

		val result = benchmarkRunner.runBenchmark(
			name = "SymSpellSearch | [${verbosity.name}][MED $maxEditDistance]",
			iterations = iterations,
			warmupIterations = warmupIterations,
			parameters = params
		) {
			var totalMatches = 0L
			for (query in queries) {
				totalMatches += spellChecker.lookup(
					query,
					verbosity,
					maxEditDistance
				).size
			}
		}

		searchResults.add(result)
	}

	fun runIndexBenchmark(
		maxEditDistance: Double,
		dictionaryFile: String,
		iterations: Int,
		warmupIterations: Int,
	) {
		val params = mapOf(
			"maxEditDistance" to maxEditDistance.toString(),
			"dictionaryFile" to dictionaryFile
		)

		// Load it all into memory before we start
		// hopefully this helps reduce variability
		val inputStream = this.javaClass.classLoader.getResourceAsStream(dictionaryFile)!!.fullyBuffered()

		val result = benchmarkRunner.runBenchmark(
			name = "SymSpellIndex | [MED:$maxEditDistance]",
			iterations = iterations,
			warmupIterations = warmupIterations,
			parameters = params
		) {
			val parser: CSVParser = CSVParser
				.parse(inputStream, Charset.forName("UTF-8"), CSVFormat.DEFAULT.withDelimiter(' '))

			val spellChecker = createSpellChecker(maxEditDistance)
			indexData(parser, spellChecker.dictionary)
			println("DataHolder Indexed Size: ${spellChecker.dictionary.wordCount}")

			inputStream.reset()
		}
		inputStream.close()

		indexResults.add(result)
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

	private fun createSpellChecker(maxEditDistance: Double): SpellChecker {
		val settings = SpellCheckSettings(maxEditDistance = maxEditDistance)
		val dictionary = InMemoryDictionaryHolder(settings, Murmur3HashFunction())
		return SymSpell(
			dictionaryHolder = dictionary,
			stringDistance = DamerauLevenshteinDistance(),
			spellCheckSettings = settings
		)
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun indexData(parser: CSVParser, dictionaryHolder: DictionaryHolder) {
		val csvIterator: Iterator<CSVRecord> = parser.iterator()
		while (csvIterator.hasNext()) {
			val csvRecord: CSVRecord = csvIterator.next()
			dictionaryHolder
				.addItem(DictionaryItem(csvRecord.get(0), csvRecord.get(1).toDouble(), 0.0))
		}
	}

	fun getResults(): BenchmarkResults {
		return BenchmarkResults(
			searchResults = searchResults.toList(),
			indexResults = indexResults.toList()
		)
	}
}

fun InputStream.fullyBuffered(): InputStream = ByteArrayInputStream(this.readBytes())