package com.darkrockstudios.fdic

import korlibs.io.stream.FastByteArrayInputStream
import korlibs.memory.ByteArrayBuilder


suspend fun ByteArrayBuilder.writeStringKmp(str: String) {
    append(str.encodeToByteArray())
    append(0) // null terminator
}

fun FastByteArrayInputStream.readDelimitedString(): String {
    val DELIMITER =  0.toByte()
    val bytes = mutableListOf<Byte>()
    while (true) {
        if (available <= 0) break
        val byte = readS8().toByte()
        if (byte == DELIMITER) break
        bytes.add(byte)
    }
    return bytes.toByteArray().decodeToString()
}