package com.darkrockstudios.symspellkt.sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun measureMillsTime(block: () -> Unit): Double

expect suspend fun measureMillsTimeAsync(block: suspend () -> Unit): Double