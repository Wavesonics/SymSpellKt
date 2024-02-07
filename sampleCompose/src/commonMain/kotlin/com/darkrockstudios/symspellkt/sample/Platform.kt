package com.darkrockstudios.symspellkt.sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform