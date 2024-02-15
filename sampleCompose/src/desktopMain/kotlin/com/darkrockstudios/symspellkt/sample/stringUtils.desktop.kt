package com.darkrockstudios.symspellkt.sample

import java.nio.charset.Charset

actual fun ByteArray.decodeToString(): String = this.toString(charset = Charset.defaultCharset())
