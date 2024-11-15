package com.darkrockstudios.symspellkt.common

/**
 * Some utilities for checking if a word is spelled correctly already.
 * Using a Lookup method on an already correct word will return a List
 * containing exactly 1 item, who's term is the original word.
 *
 * Checking that is not very expressive, so these provide a more
 * expressive way to do the check.
 */

/**
 * Check if a given suggestion list implies that the original word was spelled
 * correctly.
 *
 * {@snippet :
 *      val suggestions = spellChecker.lookup(word)
 *      if (suggestions.spellingIsCorrect(word)) println("Word is spell correctly!")
 * }
 */
fun List<SuggestionItem>.spellingIsCorrect(word: String): Boolean =
    (size == 1 && get(0).term.equals(word, ignoreCase = true))

/**
 * Check if a given suggestion list implies that the original word was spelled
 * correctly.
 *
 * {@snippet :
 *      val suggestions = spellChecker.lookup(word)
 *      if (word.isSpelledCorrectly(suggestions)) println("Word is spell correctly!")
 * }
 */
fun String.isSpelledCorrectly(suggestions: List<SuggestionItem>): Boolean =
    (suggestions.size == 1 && suggestions[0].term.equals(this, ignoreCase = true))