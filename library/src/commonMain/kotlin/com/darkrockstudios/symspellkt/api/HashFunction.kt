package com.darkrockstudios.symspellkt.api

interface HashFunction {
	/**
	 * Return the hash of the bytes as long.
	 *
	 * @param bytes the bytes to be hashed
	 * @return the generated hash value
	 */
	fun hash(bytes: ByteArray?): Long


	/**
	 * Return the hash of the bytes as long.
	 *
	 * @param data the String to be hashed
	 * @return the generated  hash value
	 */
	fun hash(data: String?): Long?
}
