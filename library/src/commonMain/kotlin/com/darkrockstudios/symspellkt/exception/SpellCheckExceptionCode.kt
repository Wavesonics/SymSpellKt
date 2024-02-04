package com.darkrockstudios.symspellkt.exception

enum class SpellCheckExceptionCode(val message: String) {
	LOOKUP_ERROR("Exception occured while looking up the term"),
	INDEX_ERROR("Exception occured while indexing  the term"),
	DELETE_ERROR("Exception occured while deleting up the term"),
	DUMP_ERROR("Exception occured while dumping up the term")
}
