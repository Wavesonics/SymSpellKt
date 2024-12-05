# SymSpellKt Fdic
These are sime extension functions for loading [fdic](../Fdic/README.md) dictionary files into a SymSpell spell checker.

## Getting started
Add to your project from Maven Central:

`implementation("com.darkrockstudios:symspellfdic:3.1.0)`

Then an `fdic` dictionary into your Spell Checker:
```kotlin
val checker = SymSpell()
// Load fdic dictionary from composeResources
checker.dictionary.loadFdicFile(Res.readBytes("files/en-80k.fdic"))
// Load fdic dictionary from the file system
checker.dictionary.loadFdicFile("path/to/en-80k.fdic")
```
_**Note:** Unigram VS Bigram dictionaries are handled automatically for you, no need to specify which this particular
dictionary is._