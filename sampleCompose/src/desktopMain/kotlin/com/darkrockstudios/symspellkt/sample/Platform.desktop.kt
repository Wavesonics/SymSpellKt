package com.darkrockstudios.symspellkt.sample

import kotlinx.coroutines.runBlocking
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class JVMPlatform: Platform {
	override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

@OptIn(ExperimentalContracts::class)
actual fun measureMillsTime(block: () -> Unit): Double {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return kotlin.system.measureNanoTime(block).toDouble() / 1000000.0
}

@OptIn(ExperimentalContracts::class)
actual suspend fun measureMillsTimeAsync(block: suspend () -> Unit): Double {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return kotlin.system.measureNanoTime {
		runBlocking { block() }
	}.toDouble() / 1000000.0
}