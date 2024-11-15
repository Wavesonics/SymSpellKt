package com.darkrockstudios.symspellkt.exception

/**
 * Exception for Spellcheck
 * @param message
 * @param cause
 * @param code
 */
class SpellCheckException(
	val spellCheckExceptionCode: SpellCheckExceptionCode,
	val customMessage: String? = null,
	cause: Throwable? = null,
) : Exception(customMessage ?: "", cause)