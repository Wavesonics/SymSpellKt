package com.darkrockstudios.symspellkt.impl

import com.darkrockstudios.symspellkt.api.DataHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.exception.SpellCheckExceptionCode
import kotlin.math.*

/**
 * Symspell variant of the Spellchecker
 */
class SymSpellCheck(
	dataHolder: DataHolder,
	stringDistance: StringDistance,
	spellCheckSettings: SpellCheckSettings,
) : SpellChecker(dataHolder, stringDistance, spellCheckSettings) {
	/**
	 * supports compound aware automatic spelling correction of multi-word input strings with three
	 * cases 1. mistakenly inserted space into a correct word led to two incorrect terms 2. mistakenly
	 * omitted space between two correct words led to one incorrect combined term 3. multiple
	 * independent input terms with/without spelling errors Find suggested spellings for a multi-word
	 * input string (supports word splitting/merging).
	 *
	 * @param word          The string being spell checked.
	 * @param editDistance The maximum edit distance between input and suggested words.
	 * @return A list of [SuggestionItem] object representing suggested correct spellings for
	 * the input string.
	 */
	@Throws(SpellCheckException::class)
	override fun lookupCompound(
		word: String,
		editDistance: Double,
		tokenizeOnWhiteSpace: Boolean
	): List<SuggestionItem> {
		var runningPhrase = word
		if (editDistance > spellCheckSettings.maxEditDistance) {
			throw SpellCheckException(
				SpellCheckExceptionCode.LOOKUP_ERROR,
				"max Edit distance should be less than  global Max i.e" + spellCheckSettings
					.maxEditDistance
			)
		}
		if (runningPhrase.isEmpty()) {
			throw SpellCheckException(
				SpellCheckExceptionCode.LOOKUP_ERROR,
				"Invalid input of string",
			)
		}
		if (spellCheckSettings.lowerCaseTerms) {
			runningPhrase = runningPhrase.lowercase()
		}
		val items = if (tokenizeOnWhiteSpace) {
			SpellHelper.tokenizeOnWhiteSpace(runningPhrase)
		} else {
			arrayOf<String>(runningPhrase)
		}
		var suggestions: MutableList<SuggestionItem> = mutableListOf()
		val suggestionParts: MutableList<SuggestionItem> = mutableListOf()
		var isLastCombi = false

		/*
	      Early exit when in exclusion list
	     */
		if (dataHolder.getExclusionItem(runningPhrase)?.isNotEmpty() == true) {
			return SpellHelper
				.earlyExit(
					suggestions,
					dataHolder.getExclusionItem(runningPhrase),
					editDistance,
					spellCheckSettings.topK,
					false
				)
		}

		for (i in items.indices) {
			val item = items[i]
			//Normal suggestions
			suggestions = lookup(item, Verbosity.TOP, editDistance)

			//combi check, always before split
			if (i > 0 && !isLastCombi
				&& lookupCombineWords(
					item,
					items[i - 1],
					suggestions,
					suggestionParts,
					editDistance,
				)
			) {
				isLastCombi = true
				continue
			}

			isLastCombi = false

			if (
				suggestions.isNotEmpty() &&
				(suggestions[0].distance == 0.0 || item.length == 1)
			) {
				//choose best suggestion
				suggestionParts.add(suggestions[0])
			} else {
				lookupSplitWords(suggestionParts, suggestions, item, editDistance)
			}
		}

		var joinedTerm = ""
		var joinedCount = Double.MAX_VALUE
		for (si in suggestionParts) {
			joinedTerm = joinedTerm + si.term + " "
			joinedCount = min(joinedCount, si.count)
		}
		joinedTerm = joinedTerm.trim()
		val dist: Double = stringDistance.getDistance(
			joinedTerm, runningPhrase, 2.0.pow(31.0) - 1.0
		)

		val suggestionItem = SuggestionItem(joinedTerm, dist, joinedCount)
		return listOf(suggestionItem)
	}

	/**
	 * supports compound aware automatic spelling correction of multi-word input strings with
	 * mistakenly omitted space between two correct words led to one incorrect combined term
	 *
	 * @param token           The string being spell checked.
	 * @param previousToken   The string previousToken being spell checked.
	 * @param maxEditDistance The maximum edit distance between input and suggested words.
	 * @param suggestions     Suggestions items List
	 * @param suggestionParts Partial suggestions list.
	 */
	@Throws(SpellCheckException::class)
	private fun lookupCombineWords(
		token: String,
		previousToken: String?,
		suggestions: List<SuggestionItem>,
		suggestionParts: MutableList<SuggestionItem>, maxEditDistance: Double
	): Boolean {
		val suggestionsCombi: List<SuggestionItem> = lookup(
			previousToken + token,
			Verbosity.TOP,
			maxEditDistance
		)
		if (suggestionsCombi.isEmpty()) {
			return false
		}
		val best1: SuggestionItem = suggestionParts[suggestionParts.size - 1]
		val best2 = if (suggestions.isNotEmpty()) {
			suggestions[0]
		} else {
			SuggestionItem(
				token,
				maxEditDistance + 1,
				0.0
			)
		}

		val editDistance: Double = stringDistance
			.getDistance(
				"${best1.term} ${best2.term}",
				"$previousToken $token",
				maxEditDistance
			)

		if (editDistance >= 0 && suggestionsCombi[0].distance < editDistance) {
			suggestionsCombi[0].distance = (suggestionsCombi[0].distance + 1)
			suggestionParts.removeAt(suggestionParts.size - 1)
			suggestionParts.add(suggestionsCombi[0])
			return true
		}
		return false
	}


	/**
	 * supports compound aware automatic spelling correction of multi-word input strings with
	 * mistakenly inserted space into a correct word led to two incorrect terms
	 *
	 * @param suggestions     Suggestions items List
	 * @param maxEditDistance The maximum edit distance between input and suggested words.
	 * @param suggestionParts Partial suggestions list.
	 */
	@Throws(SpellCheckException::class)
	private fun lookupSplitWords(
		suggestionParts: MutableList<SuggestionItem>,
		suggestions: List<SuggestionItem>,
		word: String,
		maxEditDistance: Double
	) {
		//if no perfect suggestion, split word into pairs
		var suggestionSplitBest: SuggestionItem? = null
		if (suggestions.isNotEmpty()) {
			suggestionSplitBest = suggestions[0]
		}

		if (word.length <= 1) {
			suggestionParts.add(SuggestionItem(word, maxEditDistance + 1, 0.0))
			return
		}

		for (j in 1 until word.length) {
			val part1 = word.substring(0, j)
			val part2 = word.substring(j)

			val suggestions1 = lookup(
				part1,
				Verbosity.TOP,
				maxEditDistance
			)

			if (SpellHelper.continueConditionIfHeadIsSame(suggestions, suggestions1)) {
				continue
			}

			val suggestions2 = lookup(part2, Verbosity.TOP, maxEditDistance)

			if (SpellHelper.continueConditionIfHeadIsSame(suggestions, suggestions2)) {
				continue
			}

			val split = suggestions1[0].term + " " + suggestions2[0].term
			var splitDistance = stringDistance.getDistance(word, split, maxEditDistance)
			var count: Double

			if (splitDistance < 0) {
				splitDistance = maxEditDistance + 1
			}

			if (suggestionSplitBest != null) {
				if (splitDistance > suggestionSplitBest.distance) {
					continue
				}
				if (splitDistance < suggestionSplitBest.distance) {
					suggestionSplitBest = null
				}
			}

			val bigramFreq = dataHolder.getItemFrequencyBiGram(split)

			//if bigram exists in bigram dictionary
			if (bigramFreq != null) {
				count = bigramFreq

				if (suggestions.isNotEmpty()) {
					if ((suggestions1[0].term + suggestions2[0].term) == word) {
						//make count bigger than count of single term correction
						count = max(count, suggestions[0].count + 2)
					} else if ((suggestions1[0].term === suggestions[0].term)
						|| (suggestions2[0].term == suggestions[0].term)
					) {
						//make count bigger than count of single term correction
						count = max(count, suggestions[0].count + 1)
					}
				} else if ((suggestions1[0].term + suggestions2[0].term) == word) {
					count = max(
						count,
						max(suggestions1[0].count, suggestions2[0].count)
					)
				}
			} else {
				count = min(
					spellCheckSettings.bigramCountMin,
					(suggestions1[0].count / nMax * suggestions2[0].count)
				)
			}

			val suggestionSplit = SuggestionItem(split, splitDistance, count)

			if ((suggestionSplitBest == null) || (suggestionSplit.count > suggestionSplitBest.count)) {
				suggestionSplitBest = suggestionSplit
			}
		}

		if (suggestionSplitBest != null) {
			suggestionParts.add(suggestionSplitBest)
		} else {
			suggestionParts.add(SuggestionItem(word, maxEditDistance + 1, 0.0))
		}
	}

	/**
	 * @param word          The word being spell checked.
	 * @param verbosity       The value controlling the quantity/closeness of the returned
	 * suggestions
	 * @param editDistance The maximum edit distance between phrase and suggested words.
	 * @return List of [SuggestionItem]
	 */
	@Throws(SpellCheckException::class)
	override fun lookup(
		word: String,
		verbosity: Verbosity,
		editDistance: Double
	): MutableList<SuggestionItem> {
		var curPhrase = word
		var maxEditDistance = editDistance
		if (maxEditDistance <= 0) {
			maxEditDistance = spellCheckSettings.maxEditDistance
		}

		if (maxEditDistance > spellCheckSettings.maxEditDistance) {
			throw SpellCheckException(
				SpellCheckExceptionCode.LOOKUP_ERROR,
				"max Edit distance should be less than  global Max i.e" + spellCheckSettings
					.maxEditDistance
			)
		}

		val phraseLen = curPhrase.length
		if (spellCheckSettings.lowerCaseTerms) {
			curPhrase = curPhrase.lowercase()
		}
		var suggestionCount: Double
		val consideredDeletes: MutableSet<String> = HashSet()
		val consideredSuggestions: MutableSet<String> = HashSet()
		val suggestionItems: MutableList<SuggestionItem> = ArrayList(spellCheckSettings.topK)

		/*
	      Early exit when in exclusion list
	     */
		val exclusionItem = dataHolder.getExclusionItem(curPhrase)
		if (!exclusionItem.isNullOrEmpty()) {
			return SpellHelper
				.earlyExit(
					suggestionItems,
					exclusionItem,
					maxEditDistance,
					spellCheckSettings.topK,
					false
				)
		}

		/*
	    Early exit when word is too big
	     */
		if ((phraseLen - maxEditDistance) > spellCheckSettings.maxLength) {
			return SpellHelper.earlyExit(
				suggestionItems,
				curPhrase,
				maxEditDistance,
				spellCheckSettings.topK,
				spellCheckSettings.ignoreUnknown
			)
		}

		val frequency = dataHolder.getItemFrequency(curPhrase)

		if (frequency != null) {
			suggestionCount = frequency
			val si = SuggestionItem(curPhrase, 0.0, suggestionCount)
			suggestionItems.addItemSorted(si, spellCheckSettings.topK)

			if (verbosity != Verbosity.ALL) {
				return SpellHelper.earlyExit(
					suggestionItems,
					curPhrase,
					maxEditDistance,
					spellCheckSettings.topK,
					spellCheckSettings.ignoreUnknown
				)
			}
		}

		consideredSuggestions.add(curPhrase)
		var maxEditDistance2 = maxEditDistance
		val phrasePrefixLen: Int
		val candidates: MutableList<String> = ArrayList()

		if (phraseLen > spellCheckSettings.prefixLength) {
			phrasePrefixLen = spellCheckSettings.prefixLength
			candidates.add(curPhrase.substring(0, phrasePrefixLen))
		} else {
			phrasePrefixLen = phraseLen
		}
		candidates.add(curPhrase)

		while (candidates.isNotEmpty()) {
			val candidate = candidates.removeAt(0)
			val candidateLen = candidate.length
			val lenDiff = phraseLen - candidateLen

			// Empty candidates cause a bunch of unnecessary suggestions and waste time.
			// I think this is okay? If there are ever problems with single letter word
			// corrections, this could be the culprit, because this causes words like "a"
			// and "I" to be suggested.
			if (candidate.isEmpty()) {
				continue
			}

			/*
      early termination: if candidate distance is already higher than suggestion distance,
      than there are no better suggestions to be expected
       */
			if (lenDiff > maxEditDistance2) {
				if (verbosity == Verbosity.ALL) {
					continue
				}
				break
			}


			/*
      read candidate entry from dictionary
       */
			val deletes = dataHolder.getDeletes(candidate)
			if (deletes != null && deletes.size > 0) {
				for (suggestion in deletes) {
					if (filterOnEquivalance(suggestion, curPhrase, candidate, maxEditDistance2)
						||
						filterOnPrefixLen(
							suggestion.length, spellCheckSettings.prefixLength,
							phrasePrefixLen, candidate.length, maxEditDistance2
						)
					) {
						continue
					}
					/*
            True Damerau-Levenshtein Edit Distance: adjust
                    distance, if both distances>0
                    We allow simultaneous edits (deletes) of
                    max_edit_distance on on both the dictionary and
                    the phrase term. For replaces and adjacent
                    transposes the resulting edit distance stays
                    <= max_edit_distance. For inserts and deletes the
                    resulting edit distance might exceed
                    max_edit_distance. To prevent suggestions of a
                    higher edit distance, we need to calculate the
                    resulting edit distance, if there are
                    simultaneous edits on both sides.
                    Example: (bank==bnak and bank==bink, but
                    bank!=kanb and bank!=xban and bank!=baxn for
                    max_edit_distance=1). Two deletes on each side of
                    a pair makes them all equal, but the first two
                    pairs have edit distance=1, the others edit
                    distance=2.
          */
					var distance: Double
					var minDistance: Int

					if (candidateLen == 0) {
						/*
              suggestions which have no common chars with
                        phrase (phrase_len<=max_edit_distance &&
                        suggestion_len<=max_edit_distance)
            */
						distance = max(phraseLen.toDouble(), suggestion.length.toDouble())
						if (distance > maxEditDistance2 || !consideredSuggestions.add(suggestion)) {
							continue
						}
					} else if (suggestion.length == 1) {
						distance = (if (curPhrase.indexOf(suggestion[0]) < 0) phraseLen else phraseLen - 1).toDouble()
						if (distance > maxEditDistance2 || !consideredSuggestions.add(suggestion)) {
							continue
						}
					} else {
						/*
              handles the shortcircuit of min_distance assignment when first boolean expression
              evaluates to False
             */

						minDistance = getMinDistanceOnPrefixbasis(
							maxEditDistance, candidate,
							curPhrase, suggestion
						)

						if (isDistanceCalculationRequired(
								curPhrase, maxEditDistance, minDistance, suggestion,
								candidate
							)
						) {
							continue
						} else {
							if (verbosity != Verbosity.ALL
								&& !deleteInSuggestionPrefix(
									candidate, candidateLen,
									suggestion, suggestion.length
								) || !consideredSuggestions
									.add(suggestion)
							) {
								continue
							}
							distance = stringDistance.getDistance(curPhrase, suggestion, maxEditDistance2)
							if (distance < 0) {
								continue
							}
						}
					}

					if (SpellHelper.isLessOrEqualDouble(distance, maxEditDistance2)) {
						suggestionCount = dataHolder.getItemFrequency(suggestion)!!
						val si = SuggestionItem(suggestion, distance, suggestionCount)
						if (suggestionItems.isNotEmpty()) {
							if (verbosity == Verbosity.CLOSEST && distance < maxEditDistance2) {
								suggestionItems.clear()
							} else if (verbosity == Verbosity.TOP) {
								if (SpellHelper.isLessDouble(distance, maxEditDistance2)
									|| suggestionCount > suggestionItems[0].count
								) {
									maxEditDistance2 = distance
									suggestionItems[0] = si
								}
								continue
							}
						}

						if (verbosity != Verbosity.ALL) {
							maxEditDistance2 = distance
						}
						suggestionItems.addItemSorted(si, spellCheckSettings.topK)
					}
				}
			}

			if (lenDiff < maxEditDistance && candidateLen <= spellCheckSettings.prefixLength) {
				if (verbosity != Verbosity.ALL && lenDiff >= maxEditDistance2) {
					continue
				}

				for (i in 0 until candidateLen) {
					val delete = candidate.substring(0, i) + candidate.substring(i + 1, candidateLen)
					if (consideredDeletes.add(delete)) {
						candidates.add(delete)
					}
				}
			}
		}

		return suggestionItems
	}

	private fun getMinDistanceOnPrefixbasis(
		maxEditDistance: Double, candidate: String?, phrase: String,
		suggestion: String
	): Int {
		return if ((spellCheckSettings.prefixLength - maxEditDistance) == candidate!!.length.toDouble()) {
			(min(
				phrase.length.toDouble(),
				suggestion.length.toDouble()
			) - spellCheckSettings.prefixLength).toInt()
		} else {
			0
		}
	}

	private fun filterOnPrefixLen(
		suggestionLen: Int,
		prefixLen: Int,
		phrasePrefixLen: Int,
		candidateLen: Int,
		maxEditDistance2: Double
	): Boolean {
		val suggestionPrefixLen = min(suggestionLen.toDouble(), prefixLen.toDouble()).toInt()
		return (suggestionPrefixLen > phrasePrefixLen
				&& (suggestionPrefixLen - candidateLen) > maxEditDistance2)
	}

	private fun filterOnEquivalance(
		delete: String,
		phrase: String,
		candidate: String,
		maxEditDistance2: Double
	): Boolean {
		return (delete == phrase || (abs((delete.length - phrase.length).toDouble()) > maxEditDistance2)
				|| (delete.length < candidate.length) || (delete.length == candidate.length
				&& delete != candidate))
	}

	/**
	 * Check whether all delete chars are present in the suggestion prefix in correct order, otherwise
	 * this is just a hash collision
	 */
	private fun deleteInSuggestionPrefix(
		delete: String,
		deleteLen: Int,
		suggestion: String,
		suggestionLen: Int
	): Boolean {
		var runningSuggestionLen = suggestionLen
		if (deleteLen == 0) {
			return true
		}
		if (spellCheckSettings.prefixLength < runningSuggestionLen) {
			runningSuggestionLen = spellCheckSettings.prefixLength
		}

		var j = 0
		for (i in 0 until deleteLen) {
			val delChar = delete[i]
			while (j < runningSuggestionLen && delChar != suggestion[j]) {
				j++
			}
			if (j == runningSuggestionLen) {
				return false
			}
		}
		return true
	}

	private fun isDistanceCalculationRequired(
		phrase: String,
		maxEditDistance: Double,
		min: Int,
		suggestion: String,
		candidate: String
	): Boolean {
		return (phrase.length - maxEditDistance == candidate.length.toDouble()
				&& (min > 1
				&& phrase.substring(phrase.length + 1 - min) != suggestion.substring(suggestion.length + 1 - min))
				|| (min > 0 && phrase[phrase.length - min] != suggestion[suggestion.length - min] && phrase[phrase.length - min - 1] != suggestion[suggestion.length - min] && phrase[phrase.length - min] != suggestion[suggestion.length - min - 1]))
	}

	/**
	 * word_segmentation` divides a string into words by inserting missing spaces at the appropriate
	 * positions misspelled words are corrected and do not affect segmentation existing spaces are
	 * allowed and considered for optimum segmentation
	 *
	 *
	 * `word_segmentation` uses a novel approach *without* recursion. https://medium.com/@wolfgarbe/fast-word-segmentation-for-noisy-text-2c2c41f9e8da
	 * While each string of length n can be segmented in 2^n−1 possible compositions
	 * https://en.wikipedia.org/wiki/Composition_(combinatorics) `word_segmentation` has a linear
	 * runtime O(n) to find the optimum composition
	 *
	 *
	 * Find suggested spellings for a multi-word input string (supports word splitting/merging).
	 *
	 * @param phrase                    The string being spell checked.
	 * @param maxSegmentationWordLength The maximum word length
	 * @param maxEditDistance           The maximum edit distance
	 * @return The word segmented string
	 */
	@Throws(SpellCheckException::class)
	override fun wordBreakSegmentation(
		phrase: String,
		maxSegmentationWordLength: Int,
		maxEditDistance: Double
	): Composition {
		/*
	    number of all words in the corpus used to generate the
	        frequency dictionary. This is used to calculate the word
	        occurrence probability p from word counts c : p=c/nMax. nMax equals
	        the sum of all counts c in the dictionary only if the
	        dictionary is complete, but not if the dictionary is
	        truncated or filtered
	     */

		var curPhrase = phrase
		if (curPhrase.isEmpty()) {
			return Composition()
		}
		if (spellCheckSettings.lowerCaseTerms) {
			curPhrase = curPhrase.lowercase()
		}

		/*
	      Early exit when in exclusion list
	     */
		val exclusion = dataHolder.getExclusionItem(curPhrase)
		if (!exclusion.isNullOrEmpty()) {
			return Composition(curPhrase, dataHolder.getExclusionItem(curPhrase))
		}

		val arraySize = min(maxSegmentationWordLength.toDouble(), curPhrase.length.toDouble()).toInt()
		val compositions: Array<Composition?> = arrayOfNulls<Composition>(arraySize)
		for (i in 0 until arraySize) {
			compositions[i] = Composition()
		}
		var circularIndex = -1
		//outer loop (column): all possible part start positions
		for (j in curPhrase.indices) {
			//inner loop (row): all possible part lengths (from start position): part can't be bigger
			// than longest word in dictionary (other than long unknown word)
			val imax = min((curPhrase.length - j).toDouble(), maxSegmentationWordLength.toDouble())
				.toInt()
			for (i in 1..imax) {
				//get top spelling correction/ed for part
				var part = curPhrase.substring(j, j + i)
				var separatorLength = 0
				var topEd = 0.0
				var topProbabilityLog: Double
				var topResult: String
				if (part[0].isWhitespace()) {
					//remove space for levensthein calculation
					part = part.substring(1)
				} else {
					//add ed+1: space did not exist, had to be inserted
					separatorLength = 1
				}

				//remove space from part1, add number of removed spaces to topEd
				topEd += part.length
				//remove space
				part = part.replace(" ", "")
				//add number of removed spaces to ed
				topEd -= part.length

				val results: List<SuggestionItem> = this.lookup(part, Verbosity.TOP, maxEditDistance)
				if (results.isNotEmpty()) {
					topResult = results[0].term
					topEd += results[0].distance

					//Naive Bayes Rule
					//we assume the word probabilities of two words to be independent
					//therefore the resulting probability of the word combination is the product of the
					// two word probabilities

					//instead of computing the product of probabilities we are computing the sum of the
					// logarithm of probabilities
					//because the probabilities of words are about 10^-10, the product of many such small
					// numbers could exceed (underflow) the floating number range and become zero
					//log(ab)=log(a)+log(b)
					topProbabilityLog = log10(results[0].count / nMax)
				} else {
					topResult = part
					//default, if word not found
					//otherwise long input text would win as long unknown word (with ed=edmax+1 ), although
					// there should many spaces inserted
					topEd += part.length
					topProbabilityLog = log10(10.0 / (nMax * 10.0.pow(part.length.toDouble())))
				}
				val destinationIndex = ((i + circularIndex) % arraySize)

				val destComp =
					compositions[destinationIndex] ?: error("Failed to find destinationIndex: $destinationIndex")

				//set values in first loop
				if (j == 0) {
					destComp.apply {
						segmentedString = part
						correctedString = topResult
						distanceSum = topEd
						logProbSum = topProbabilityLog
					}
				} else {
					val circularComp =
						compositions[circularIndex] ?: error("Failed to find circularIndex: $circularIndex")

					if ((i == maxSegmentationWordLength) //replace values if better probabilityLogSum, if same edit distance OR one
						// space difference
						|| (((circularComp.distanceSum + topEd
								== destComp.distanceSum) || (circularComp.distanceSum + separatorLength + topEd
								== destComp.distanceSum)) && (destComp.logProbSum
								< circularComp.logProbSum + topProbabilityLog)) //replace values if smaller edit distance
						|| (circularComp.distanceSum + separatorLength + topEd
								< destComp.distanceSum)
					) {
						destComp.segmentedString = circularComp.segmentedString + " " + part
						destComp.correctedString = circularComp.correctedString + " " + topResult
						destComp.distanceSum = circularComp.distanceSum + topEd
						destComp.logProbSum = circularComp.logProbSum + topProbabilityLog
					}
				}
			}

			circularIndex++
			if (circularIndex >= arraySize) {
				circularIndex = 0
			}
		}
		return compositions[circularIndex]!!
	}


	companion object {
		//N equals the sum of all counts c in the dictionary only if the dictionary is complete,
		// but not if the dictionary is truncated or filtered
		private const val nMax = 1024908267229L
	}
}
