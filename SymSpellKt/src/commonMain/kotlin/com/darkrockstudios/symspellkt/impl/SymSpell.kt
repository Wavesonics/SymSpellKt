package com.darkrockstudios.symspellkt.impl

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.api.StringDistance
import com.darkrockstudios.symspellkt.common.Composition
import com.darkrockstudios.symspellkt.common.DamerauLevenshteinDistance
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.Murmur3HashFunction
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SpellHelper
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.common.addItemSorted
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.exception.SpellCheckExceptionCode
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Symspell implementation of the Spellchecker
 */
class SymSpell(
	spellCheckSettings: SpellCheckSettings = SpellCheckSettings(),
	stringDistance: StringDistance = DamerauLevenshteinDistance(),
	dictionaryHolder: DictionaryHolder = InMemoryDictionaryHolder(
		spellCheckSettings,
		Murmur3HashFunction()
	),
) : SpellChecker(dictionaryHolder, stringDistance, spellCheckSettings) {
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
		if (dictionary.getExclusionItem(runningPhrase)?.isNotEmpty() == true) {
			return SpellHelper
				.earlyExit(
					suggestions,
					dictionary.getExclusionItem(runningPhrase),
					editDistance,
					spellCheckSettings.topK,
					false
				)
		}

		for (i in items.indices) {
			val item = items[i]
			//Normal suggestions
			suggestions = lookup(item, Verbosity.Top, editDistance)

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
			joinedCount = min(joinedCount, si.frequency)
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
			Verbosity.Top,
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
				Verbosity.Top,
				maxEditDistance
			)

			if (SpellHelper.continueConditionIfHeadIsSame(suggestions, suggestions1)) {
				continue
			}

			val suggestions2 = lookup(part2, Verbosity.Top, maxEditDistance)

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

			val bigramFreq = dictionary.getItemFrequencyBiGram(split)

			//if bigram exists in bigram dictionary
			if (bigramFreq != null) {
				count = bigramFreq

				if (suggestions.isNotEmpty()) {
					if ((suggestions1[0].term + suggestions2[0].term) == word) {
						//make count bigger than count of single term correction
						count = max(count, suggestions[0].frequency + 2)
					} else if ((suggestions1[0].term === suggestions[0].term)
						|| (suggestions2[0].term == suggestions[0].term)
					) {
						//make count bigger than count of single term correction
						count = max(count, suggestions[0].frequency + 1)
					}
				} else if ((suggestions1[0].term + suggestions2[0].term) == word) {
					count = max(
						count,
						max(suggestions1[0].frequency, suggestions2[0].frequency)
					)
				}
			} else {
				count = min(
					spellCheckSettings.bigramCountMin,
					(suggestions1[0].frequency / nMax * suggestions2[0].frequency)
				)
			}

			val suggestionSplit = SuggestionItem(split, splitDistance, count)

			if ((suggestionSplitBest == null) || (suggestionSplit.frequency > suggestionSplitBest.frequency)) {
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
		var suggestionFrequency: Double
		val consideredDeletes: MutableSet<String> = HashSet(phraseLen * 2)
		val consideredSuggestions: MutableSet<String> = HashSet()
		val suggestionItems: MutableList<SuggestionItem> = ArrayList(spellCheckSettings.topK)

		/*
		  Early exit when in exclusion list
		 */
		val exclusionItem = dictionary.getExclusionItem(curPhrase)
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

		val frequency = dictionary.getItemFrequency(curPhrase)

		if (frequency != null) {
			suggestionFrequency = frequency
			val si = SuggestionItem(curPhrase, 0.0, suggestionFrequency)
			suggestionItems.addItemSorted(si, spellCheckSettings.topK)

			if (verbosity != Verbosity.All) {
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
		var phrasePrefixLen: Int = phraseLen
		val candidates: MutableList<String> = ArrayDeque<String>(phraseLen)

		if (phraseLen > spellCheckSettings.prefixLength) {
			phrasePrefixLen = spellCheckSettings.prefixLength
			candidates.add(curPhrase.substring(0, phrasePrefixLen))
		} else {
			candidates.add(curPhrase)
		}

		while (candidates.isNotEmpty()) {
			val candidate = candidates.removeAt(0)
			val candidateLen = candidate.length
			val lenDiff = phrasePrefixLen - candidateLen

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
				if (verbosity == Verbosity.All) {
					continue
				}
				break
			}


			/*
	  read candidate entry from dictionary
	   */
			val deletes = dictionary.getDeletes(candidate)
			if (deletes != null && deletes.size > 0) {
				for (suggestion in deletes) {
					val suggestionLength = suggestion.length

					if (filterOnEquivalance(suggestion, curPhrase, candidate, maxEditDistance2)
						||
						filterOnPrefixLen(
							suggestionLength, spellCheckSettings.prefixLength,
							phrasePrefixLen, candidate.length, maxEditDistance2
						)
					) {
						continue
					}

					// Early exit before expensive distance calculation
					// This is an attempt at optimization
					if (suggestion.length > curPhrase.length + maxEditDistance2) {
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
						distance = max(phraseLen.toDouble(), suggestionLength.toDouble())
						if (distance > maxEditDistance2 || !consideredSuggestions.add(suggestion)) {
							continue
						}
					} else if (suggestionLength == 1) {
						distance =
							(if (curPhrase.indexOf(suggestion[0]) < 0) phraseLen else phraseLen - 1).toDouble()
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
							if (verbosity != Verbosity.All
								&& !deleteInSuggestionPrefix(
									candidate, candidateLen,
									suggestion, suggestionLength
								) || !consideredSuggestions
									.add(suggestion)
							) {
								continue
							}
							distance = if (maxEditDistance2 <= 2) {
								// Try quick distance first for small edit distances
								quickDistance(curPhrase, suggestion)
									?: stringDistance.getDistance(curPhrase, suggestion, maxEditDistance2)
							} else {
								// Fall back to full distance calculation for larger edit distances
								stringDistance.getDistance(curPhrase, suggestion, maxEditDistance2)
							}
							if (distance < 0) {
								continue
							}
						}
					}

					if (SpellHelper.isLessOrEqualDouble(distance, maxEditDistance2)) {
						suggestionFrequency = dictionary.getItemFrequency(suggestion)!!
						val si = SuggestionItem(suggestion, distance, suggestionFrequency)
						if (suggestionItems.isNotEmpty()) {
							if (verbosity == Verbosity.Closest && distance < maxEditDistance2) {
								suggestionItems.clear()
							} else if (verbosity == Verbosity.Top) {
								if (SpellHelper.isLessDouble(distance, maxEditDistance2)
									|| suggestionFrequency > suggestionItems[0].frequency
								) {
									maxEditDistance2 = distance
									suggestionItems[0] = si
								}
								continue
							}
						}

						if (verbosity != Verbosity.All) {
							maxEditDistance2 = distance
						}
						suggestionItems.addItemSorted(si, spellCheckSettings.topK)
					}
				}
			}

			if (lenDiff < maxEditDistance && candidateLen <= spellCheckSettings.prefixLength) {
				if (verbosity != Verbosity.All && lenDiff >= maxEditDistance2) {
					continue
				}

				for (i in 0 until candidateLen) {
					val delete = StringBuilder(candidateLen - 1)
						.append(candidate, 0, i)
						.append(candidate, i + 1, candidateLen)
						.toString()
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
		val suggestionLength = suggestion.length
		val phraseLength = phrase.length

		return (phraseLength - maxEditDistance == candidate.length.toDouble()
				&& (min > 1
				&& phrase.substring(phraseLength + 1 - min) != suggestion.substring(suggestionLength + 1 - min))
				|| (min > 0 && phrase[phraseLength - min] != suggestion[suggestionLength - min] && phrase[phraseLength - min - 1] != suggestion[suggestionLength - min] && phrase[phraseLength - min] != suggestion[suggestionLength - min - 1]))
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
		val exclusion = dictionary.getExclusionItem(curPhrase)
		if (!exclusion.isNullOrEmpty()) {
			return Composition(curPhrase, dictionary.getExclusionItem(curPhrase))
		}

		val arraySize =
			min(maxSegmentationWordLength.toDouble(), curPhrase.length.toDouble()).toInt()
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

				val results: List<SuggestionItem> =
					this.lookup(part, Verbosity.Top, maxEditDistance)
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
					topProbabilityLog = log10(results[0].frequency / nMax)
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
					compositions[destinationIndex]
						?: error("Failed to find destinationIndex: $destinationIndex")

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
						compositions[circularIndex]
							?: error("Failed to find circularIndex: $circularIndex")

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

	override fun createDictionaryEntry(word: String, frequency: Int): Boolean {
		return createDictionaryEntry(word, frequency.toDouble())
	}

	override fun createDictionaryEntry(word: String, frequency: Double): Boolean {
		return dictionary.addItem(DictionaryItem(word, frequency))
	}

	/**
	 * Quick distance calculation for edit distance 1 and 2.
	 * Returns null if the edit distance is definitely greater than 2,
	 * otherwise returns the actual edit distance.
	 */
	private fun quickDistance(s1: String, s2: String): Double? {
		val len1 = s1.length
		val len2 = s2.length

		// If lengths differ by more than 2, edit distance must be > 2
		if (abs(len1 - len2) > 2) return null

		var diffs = 0
		val minLen = min(len1, len2)
		var i = 0

		// Check for character differences
		while (i < minLen) {
			if (s1[i] != s2[i]) {
				diffs++
				// Try to detect transposition
				if (i + 1 < minLen &&
					s1[i] == s2[i + 1] &&
					s1[i + 1] == s2[i]
				) {
					diffs++
					i += 2
					continue
				}
			}
			if (diffs > 2) return null
			i++
		}

		// Add remaining length difference to diffs
		diffs += abs(len1 - len2)

		return if (diffs <= 2) diffs.toDouble() else null
	}

	companion object {
		// N equals the sum of all counts c in the dictionary only if the dictionary is complete,
		// but not if the dictionary is truncated or filtered
		private const val nMax = 1024908267229L
	}
}