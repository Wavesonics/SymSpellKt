package com.darkrockstudios.symspellkt.sample

import com.darkrockstudios.symspellkt.sample.Platform

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()