package com.darkrockstudios.symspellkt.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.darkrockstudios.samplecompose.generated.resources.Res
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.impl.SymSpell
import com.darkrockstudios.symspellkt.impl.loadBiGramLine
import com.darkrockstudios.symspellkt.impl.loadUniGramLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
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
				Res.readBytes("files/en-80k.txt")
					.decodeToString()
					.lineSequence()
					.forEachAsync { line ->
						checker.dictionary.loadUniGramLine(line)
						yield()
					}

				Res.readBytes("files/frequency_bigramdictionary_en_243_342.txt")
					.decodeToString()
					.lineSequence()
					.forEachAsync { line ->
						checker.dictionary.loadBiGramLine(line)
						yield()
					}
			}
			println("Dictionary Loaded in: $mills ms")

			spellChecker = checker
		}
	}

	return spellChecker
}