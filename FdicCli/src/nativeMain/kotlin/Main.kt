import com.darkrockstudios.fdic.FdicException
import com.darkrockstudios.fdic.FrequencyDictionary
import com.darkrockstudios.fdic.FrequencyDictionaryIO
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Auto
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Expand
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.EmptyWidget
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.progress.*
import kotlinx.coroutines.*
import okio.Path
import okio.Path.Companion.toPath


private val splitRegex = "\\s+".toRegex()

class FdicConverter : CliktCommand() {

	val n by option("--ngram", "-n").int().default(1)
		.help("The number of grams in each entry. 1 for Unigram, 2 for Bigram")
		.check("Must be either 1 or 2, only Unigrams and Bigrams are supported.") {
			it == 1 || it == 2
		}

	val locale by option("--locale", "-l").default("en")
		.help("The locale tag and any qualifiers of this dictionary. This can be any valid Locale tag such as en-US, de and so on")
		.check("Locale tags and sugtags can not be longer than 32 characters.") {
			it.length < 32
		}

	val path by argument(
		name = "path",
		help = "Path to the plain text frequency dictionary to be converted"
	)
		.convert { it.toPath() }
		.check("Must be a path to a valid file") { p ->
			p.validatePath() && fs.exists(p)
		}

	val out: Path? by argument(
		name = "out",
		help = "Output path of the encoded file to be written"
	)
		.convert { it.toPath() }
		.optional()
		.check("Must be a path") { p ->
			p.validatePath()
		}

	val verify by option("--verify", "-v").flag()
		.help("Read back the dictionary after it is written to validate it is correct")

	override fun run() = runBlocking {
		val t = Terminal()

		val inputMetadata = fs.metadata(path)
		val numEntries = countLines(path) - 1

		t.println(
			Panel(
				expand = true,
				content = horizontalLayout {
					column(0) { width = Expand(1) }
					column(1) {
						width = Auto
					}
					column(2) { width = Expand(1) }

					cell(EmptyWidget)
					cell(
						table {
							captionTop("Input Dictionary")
							body {
								row("Input File", path.name)
								row("Size", inputMetadata.size.toString() + " Bytes")
								row("Entries", numEntries)
							}
						}
					)
					cell(EmptyWidget)

				},
				title = Text("FDIC Converter")
			)
		)

		val progress = progressBarLayout {
			spinner(Spinner.Dots())
			marquee(terminal.theme.warning("Running conversion"), width = 24, align = TextAlign.CENTER)
			percentage()
			progressBar()
			completed(style = terminal.theme.success)
		}.animateInCoroutine(terminal)

		val scope = CoroutineScope(Dispatchers.Default)
		scope.launch { progress.execute() }

		progress.update {
			total = numEntries
		}

		val dictionary = FrequencyDictionary(
			ngrams = n,
			locale = locale,
			termCount = numEntries.toInt(),
		)
		fs.read(path) {
			var line = readUtf8Line()
			while (line != null) {
				val arr = line.split(splitRegex)
				if (n == 1) {
					if (arr.size > 1) {
						dictionary.terms[arr[0]] = arr[1].toLong()
					}
				} else {
					if (arr.size > 2) {
						dictionary.terms[arr[0] + " " + arr[1]] = arr[2].toLong()
					}
				}
				progress.advance(1)
				line = readUtf8Line()
			}
		}

		val outpath = out ?: getOutPath(path)
		FrequencyDictionaryIO.writeFdic(dictionary, outpath)

		val outputMetadata = fs.metadata(outpath)

		t.println(" ")
		t.println(yellow("Wrote encoded dictionary to: ${brightGreen(outpath.toString())}"))

		if (verify) {
			try {
				val readBack = FrequencyDictionaryIO.readFdic(outpath)
				val compression = (outputMetadata.size!! / inputMetadata.size!!.toFloat()) * 100f

				t.println(" ")
				t.println(
					table {
						captionTop("Output Dictionary")
						body {
							row("File", outpath.name)
							row("Version", readBack.formatVersion)
							row("Entries", readBack.termCount)
							row("Locale", readBack.locale)
							row("Ngram", readBack.getNgramName())
							row("Input Size", inputMetadata.size!!.sizeToKb() + " KB")
							row("Output Size", outputMetadata.size!!.sizeToKb().toString() + " KB")
							row("Compression", compression.formatTwoDecimals() + "%")
							row("Outpath", outpath)
						}
					}
				)

				readBack.validate()

				if (dictionary == readBack) {
					t.println(green("Dictionary validated successfully!: ${white(outpath.toString())}"))
				} else {
					t.println(brightRed("Dictionary failed validation: Contents did not match"))
				}
			} catch (e: FdicException) {
				t.println(brightRed("Dictionary failed validation: ${white(e.message ?: "Unknown error")}"))
			}
		}
	}
}

private fun Long.sizeToKb(): String = (this.toDouble() / 1024.0).toFloat().formatTwoDecimals().toString()

private fun Float.formatTwoDecimals(): String {
	val rounded = (this * 100f).toInt() / 100f
	return rounded.toString()
}

private fun getOutPath(inPath: Path): Path {
	return if (inPath.name.endsWith(".txt")) {
		(inPath.name.removeSuffix(".txt") + ".fdic").toPath()
	} else {
		(inPath.name + ".fdic").toPath()
	}
}

private fun countLines(path: Path): Long {
	return fs.read(path) {
		var count = 1L
		val chunk = ByteArray(8192)

		while (!exhausted()) {
			val bytesRead = read(chunk)
			for (i in 0 until bytesRead) {
				if (chunk[i] == '\n'.code.toByte()) {
					count++
				}
			}
		}
		count
	}
}

fun main(args: Array<String>) = FdicConverter().main(args)