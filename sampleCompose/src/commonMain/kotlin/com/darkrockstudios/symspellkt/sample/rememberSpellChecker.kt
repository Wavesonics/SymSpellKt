package com.darkrockstudios.symspellkt.sample

import androidx.compose.runtime.*
import com.darkrockstudios.samplecompose.generated.resources.Res
import com.darkrockstudios.symspell.fdic.loadFdicFile
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.impl.SymSpell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberSpellChecker(): SpellChecker? {
	val scope = rememberCoroutineScope()
	var spellChecker by remember { mutableStateOf<SpellChecker?>(null) }

	LaunchedEffect(Unit) {
		scope.launch(Dispatchers.Default) {
			val checker = SymSpell()

			val mills = measureMillsTimeAsync {
				checker.dictionary.loadFdicFile(Res.readBytes("files/en-80k.fdic"))
				//checker.dictionary.loadUnigramTxtFile(Res.readBytes("files/en-80k.txt"))

				checker.dictionary.loadFdicFile(Res.readBytes("files/frequency_bigramdictionary_en_243_342.fdic"))
				//checker.dictionary.loadBigramTxtFile(Res.readBytes("files/frequency_bigramdictionary_en_243_342.txt"))
			}
			println("Dictionary Loaded in: $mills ms")

			spellChecker = checker
		}
	}

	return spellChecker
}