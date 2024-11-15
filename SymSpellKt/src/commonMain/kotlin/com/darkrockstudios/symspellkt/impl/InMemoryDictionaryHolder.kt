package com.darkrockstudios.symspellkt.impl

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.HashFunction
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SpellHelper.getEditDeletes
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import kotlin.math.min

/**
 * Class to create in memory dictionary for the items with term->frequency
 */
class InMemoryDictionaryHolder(
	/**
	 * Spell check settings to use the values while ingesting the terms.
	 */
	private val spellCheckSettings: SpellCheckSettings,
	private val hashFunction: HashFunction,
) : DictionaryHolder {
	/**
	 * Dictionary of unique correct spelling words, and the frequency count for each word
	 */
	private val wordsDictionary: MutableMap<String, Double> = mutableMapOf()
	private val bigramsDictionary: MutableMap<String, Double> = mutableMapOf()
	private val exclusionDictionary: MutableMap<String, String> = mutableMapOf()

	/**
	 * Dictionary of unique words that are  below the count threshold for being considered correct
	 * spellings.
	 */
	private val belowThresholdWords: MutableMap<String, Double> = mutableMapOf()

	/**
	 * Dictionary that contains a mapping of lists of suggested correction words to the hashCodes of
	 * the original words and the deletes derived from them. Collisions of hashCodes is tolerated,
	 * because suggestions are ultimately verified via an edit distance function. A list of
	 * suggestions might have a single suggestion, or multiple suggestions.
	 */
	private val deletes: MutableMap<Long, ArrayList<String>> = mutableMapOf()


	/**
	 * Create/Update an entry in the dictionary. For every word there are deletes with an edit
	 * distance of 1...maxEditDistance created and added to the dictionary. Every delete entry has a
	 * suggestions list, which points to the original term(s) it was created from. The dictionary may
	 * be dynamically updated (word frequency and new words) at any time by calling addItem
	 *
	 * @param dictionaryItem [DictionaryItem]
	 * @return True if the word was added as a new correctly spelled word, or False if the word is
	 * added as a below threshold word, or updates an existing correctly spelled word.
	 */
	@Throws(SpellCheckException::class)
	override fun addItem(dictionaryItem: DictionaryItem): Boolean {
		if (dictionaryItem.frequency <= 0 && spellCheckSettings.countThreshold > 0) {
			return false
		}

		var frequency = dictionaryItem.frequency
		var key = dictionaryItem.term
		if (spellCheckSettings.lowerCaseTerms) {
			key = key.lowercase()
		}
		if (frequency <= 0) {
			frequency = 0.0
		}

		/*
     * look first in below threshold words, update count, and allow
     * promotion to correct spelling word if count reaches threshold
     * threshold must be >1 for there to be the possibility of low
     * threshold words
     */
		frequency = addItemToBelowThreshold(key, frequency)

		if (frequency == Double.MIN_VALUE) {
			return false
		}

		//Adding new threshold word
		if (!addToDictionary(key, frequency)) {
			return false
		}


		/*
     * edits/suggestions are created only once, no matter how often
     * word occurs. edits/suggestions are created as soon as the
     * word occurs in the corpus, even if the same term existed
     * before in the dictionary as an edit from another word
     */
		if (key.length > spellCheckSettings.maxLength) {
			spellCheckSettings.maxLength = key.length
		}

		//create deletes
		val editDeletes = getEditDeletes(
			key,
			spellCheckSettings.maxEditDistance,
			spellCheckSettings.prefixLength,
			spellCheckSettings.editFactor,
		)
		for (delete in editDeletes) {
			val hash = hashFunction.hash(delete)
			if (hash != null) {
				if (deletes.containsKey(hash)) {
					deletes[hash]!!.add(key)
				} else {
					deletes[hash] = arrayListOf(key)
				}
			}
		}
		return true
	}


	private fun addToDictionary(key: String, frequency: Double): Boolean {
		if (spellCheckSettings.doKeySplit
			&& key.split(spellCheckSettings.keySplitRegex).size > 1
		) {
			bigramsDictionary[key] = frequency
			if (frequency < spellCheckSettings.bigramCountMin) {
				spellCheckSettings.bigramCountMin = frequency
			}
			return false
		} else {
			wordsDictionary[key] = frequency
			return true
		}
	}


	@Throws(SpellCheckException::class)
	override fun getItemFrequency(term: String): Double? = wordsDictionary[term]

	@Throws(SpellCheckException::class)
	override fun getItemFrequencyBiGram(term: String): Double? = bigramsDictionary[term]

	override fun getDeletes(key: String): ArrayList<String>? = deletes[hashFunction.hash(key)]

	override val wordCount: Int
		get() = wordsDictionary.size

	override fun clear(): Boolean {
		wordsDictionary.clear()
		deletes.clear()
		belowThresholdWords.clear()
		return false
	}

	private fun addItemToBelowThreshold(key: String, frequency: Double): Double {
		var runningFrequency = frequency
		if (spellCheckSettings.countThreshold > 1 && belowThresholdWords.containsKey(key)) {
			val prevFreq = belowThresholdWords[key]!!
			runningFrequency =
				prevFreq + (if (Double.MAX_VALUE - prevFreq > runningFrequency) runningFrequency else Double.MAX_VALUE)
			if (runningFrequency > spellCheckSettings.countThreshold) {
				belowThresholdWords.remove(key)
			} else {
				belowThresholdWords[key] = runningFrequency
				return Double.MIN_VALUE
			}
		} else if (wordsDictionary.containsKey(key)) {
			val prevFreq = wordsDictionary[key] ?: 0.0
			runningFrequency = min(Double.MAX_VALUE, prevFreq + runningFrequency)
			addToDictionary(key, runningFrequency)
			return Double.MIN_VALUE
		} else if (runningFrequency < spellCheckSettings.countThreshold) {
			belowThresholdWords[key] = runningFrequency
			return Double.MIN_VALUE
		}
		return runningFrequency
	}

	override fun addExclusionItem(key: String, value: String) {
		exclusionDictionary[key] = value
	}

	override fun addExclusionItems(values: Map<String, String>) {
		exclusionDictionary.putAll(values)
	}

	override fun getExclusionItem(key: String): String? = exclusionDictionary[key]
}
