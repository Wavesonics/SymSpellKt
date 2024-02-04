package com.darkrockstudios.symspellkt.api

import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.exception.SpellCheckException

/**
 * Interface to contain the dictionary
 */
interface DataHolder {
	@Throws(SpellCheckException::class)
	fun addItem(dictionaryItem: DictionaryItem): Boolean

	@Throws(SpellCheckException::class)
	fun getItemFrequency(term: String): Double?

	@Throws(SpellCheckException::class)
	fun getItemFrequencyBiGram(term: String): Double?

	@Throws(SpellCheckException::class)
	fun getDeletes(key: String): ArrayList<String>?

	val size: Int

	@Throws(SpellCheckException::class)
	fun clear(): Boolean

	fun addExclusionItem(key: String, value: String)

	fun addExclusionItems(values: Map<String, String>)

	fun getExclusionItem(key: String): String?
}
