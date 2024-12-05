package performance.strategies

import korlibs.io.compression.compress
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.lang.UTF8
import korlibs.io.lang.toString
import java.io.File
import kotlin.collections.associate
import kotlin.collections.map
import kotlin.io.readBytes
import kotlin.io.readText
import kotlin.io.writeBytes
import kotlin.text.lines
import kotlin.text.split
import kotlin.text.toByteArray
import kotlin.text.toLong
import kotlin.text.trim
import kotlin.to

val fdicGzFile = File("en-80k.gz")

fun loadGzDict(): Map<String, Long> {
    val frequencyDict: Map<String, Long> = fdicGzFile.readBytes().uncompress(GZIP).toString(charset = UTF8).trim().lines()
        .map { it.split(" ") }
        .associate { it[0] to it[1].toLong() }

    return frequencyDict
}

fun readAndStoreGzDict() {
    fdicGzFile.writeBytes(fdicTextFile.readText().toByteArray().compress(GZIP))
}