package com.darkrockstudios.symspellkt.sample

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
actual fun measureMillsTime(block: () -> Unit): Double {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return kotlin.system.measureNanoTime(block).toDouble() / 1000000.0
}