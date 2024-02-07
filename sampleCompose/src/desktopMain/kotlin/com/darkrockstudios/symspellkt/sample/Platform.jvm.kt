package com.darkrockstudios.symspellkt.sample

import com.darkrockstudios.symspellkt.sample.Platform

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()