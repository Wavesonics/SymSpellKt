package com.darkrockstudios.symspellkt.common

import com.darkrockstudios.symspellkt.api.HashFunction
import com.goncalossilva.murmurhash.MurmurHash3

class Murmur3HashFunction : HashFunction {
	private val murmurHash = MurmurHash3(SEED)

	override fun hash(bytes: ByteArray?): Long? {
		return if(bytes != null) {
			murmurHash.hash32x86(bytes).toLong()
		} else {
			null
		}
	}

	override fun hash(data: String?): Long? {
		return hash(data?.encodeToByteArray())
	}

	companion object {
		private val SEED: UInt = 0x7f3a21eaL.toUInt()
	}
}
