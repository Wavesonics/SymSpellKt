package com.darkrockstudios.symspellkt.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.impl.createSymSpellChecker
import com.darkrockstudios.symspellkt.impl.loadBiGramLine
import com.darkrockstudios.symspellkt.impl.loadUniGramLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

@Composable
fun SpellCheckerUi() {
	val spellChecker = rememberSpellChecker()
	Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
		Text(
			"SymSpell Sample",
			style = MaterialTheme.typography.displayLarge
		)

		Row(
			modifier = Modifier.padding(8.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			if (spellChecker == null) {
				CircularProgressIndicator()
				Text(" Spell Checker: Loading... ")
			} else {
				Text("Spell Checker: Ready.")
			}
		}

		Row(
			modifier = Modifier.fillMaxSize(),
			horizontalArrangement = Arrangement.SpaceAround,
		) {
			SingleWordCorrectionUi(spellChecker)

			MultiWordCorrectionUi(spellChecker)
		}
	}
}

@Composable
fun SingleWordCorrectionUi(spellChecker: SpellChecker?) {
	var suggestions by remember { mutableStateOf("") }

	Column {
		Text(
			"Single Word Correction",
			style = MaterialTheme.typography.headlineSmall
		)

		var text by remember { mutableStateOf("") }
		OutlinedTextField(
			value = text,
			onValueChange = {
				val phrase = it.trim().lowercase()
				if (phrase.isNotEmpty()) {
					var newSuggestions = ""
					spellChecker?.apply {
						val items = lookup(
							it,
							Verbosity.ALL,
							2.0
						).sorted().take(10)

						if (items.firstOrNull()?.term == phrase) {
							suggestions = "<No Misspelling>"
						} else {
							items.forEach { item ->
								newSuggestions += "${item.term}\n"
							}
							suggestions = newSuggestions
						}
					}
				} else {
					suggestions = ""
				}

				text = it.trim()
			},
			label = { Text("Spell Checker") },
			placeholder = { Text("Type misspelled words") },
			singleLine = true,
			enabled = (spellChecker != null)
		)
		Spacer(modifier = Modifier.size(16.dp))
		Text(
			"Possible Corrections",
			style = MaterialTheme.typography.labelLarge
		)
		Text(
			suggestions,
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.verticalScroll(rememberScrollState()),
		)
	}
}

@Composable
fun MultiWordCorrectionUi(spellChecker: SpellChecker?) {
	var suggestions by remember { mutableStateOf("") }

	Column {
		Text(
			"Multi Word Correction",
			style = MaterialTheme.typography.headlineSmall
		)

		var text by remember { mutableStateOf("") }
		OutlinedTextField(
			value = text,
			onValueChange = {
				val phrase = it.trim()
				if (phrase.isNotEmpty()) {
					var newSuggestions = ""
					spellChecker?.apply {
						val items = lookupCompound(it)
							.sorted()
							.take(10)
							.forEach { item ->
								newSuggestions += "${item.term}\n"
							}
					}
					suggestions = newSuggestions
				} else {
					suggestions = ""
				}

				text = it
			},
			label = { Text("Spell Checker") },
			placeholder = { Text("Type misspelled words") },
			singleLine = true,
			enabled = (spellChecker != null)
		)
		Spacer(modifier = Modifier.size(16.dp))
		Text(
			"Possible Corrections",
			style = MaterialTheme.typography.labelLarge
		)
		Text(
			suggestions,
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.verticalScroll(rememberScrollState()),
		)
	}
}