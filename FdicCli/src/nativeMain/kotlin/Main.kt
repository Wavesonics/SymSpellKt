import com.darkrockstudios.fdic.FrequencyDictionary
import com.darkrockstudios.fdic.FrequencyDictionaryEncoder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
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

		val dictionary = FrequencyDictionary()
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

		val encoder = FrequencyDictionaryEncoder()

		val outpath = out ?: getOutPath(path)
		encoder.writeFdic2Kmp(dictionary, outpath)

		t.println(" ")
		t.println(yellow("Wrote encoded dictionary to: ${brightGreen(outpath.toString())}"))
	}
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