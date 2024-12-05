package performance.strategies

import java.io.File
import kotlin.collections.associate
import kotlin.collections.map
import kotlin.io.readLines
import kotlin.text.split
import kotlin.text.toLong
import kotlin.to

val fdicTextFile = File("en-80k.txt")

fun loadTxtDict(): Map<String, Long> {
    val frequencyDict: Map<String, Long> = fdicTextFile.readLines()
        .map { it.split(" ") }
        .associate { it[0] to it[1].toLong() }

    return frequencyDict
}
