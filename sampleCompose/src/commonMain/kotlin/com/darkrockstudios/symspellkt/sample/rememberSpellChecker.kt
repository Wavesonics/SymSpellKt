package com.darkrockstudios.symspellkt.sample

import androidx.compose.runtime.*
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.impl.createSymSpellChecker
import com.darkrockstudios.symspellkt.impl.loadBiGramLine
import com.darkrockstudios.symspellkt.impl.loadUniGramLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

@OptIn(InternalResourceApi::class)
@Composable
fun rememberSpellChecker(): SpellChecker? {
	val scope = rememberCoroutineScope()
	var spellChecker by remember { mutableStateOf<SpellChecker?>(null) }

	LaunchedEffect(Unit) {
		scope.launch(Dispatchers.Default) {
			val checker = createSymSpellChecker()

			readResourceBytes("raw/en-80k.txt")
				.decodeToString()
				.splitLines()
				.fastForEach { line ->
					checker.dataHolder.loadUniGramLine(line)
				}

			readResourceBytes("raw/frequency_bigramdictionary_en_243_342.txt")
				.decodeToString()
				.splitLines()
				.fastForEach { line ->
					checker.dataHolder.loadBiGramLine(line)
				}

			spellChecker = checker
		}
	}

	return spellChecker
}