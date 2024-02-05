# SymSpell Spell Check Kotlin

_This is a Kotlin Multiplatform implementation of the [symspell](https://github.com/wolfgarbe/symspell) fuzzy search
algorithm. It has been ported from [Java implementation](https://github.com/MighTguY/customized-symspell/) of symspell._

## Current status

### Accuracy

The library is compiled and running, but lots of tests ported over from Java are failing.
The failures are mainly around correct accuracy.

There appear to be small differences in some of the mathematical calculations,
differences in the tenths or hundredths places.

Interestingly the customized values found in the Java implementation appear to work
completely, while the default values have the aforementioned accuracy errors in their
output.

### Performance

The performance of the Kotlin implementation VS the Java implementation is about half
as fast, it's possible there are more Kotlin specific optimizations that could improve this.

## SymSpell v6.6 (Bigrams)

* the optional bigram dictionary in order to use sentence level context information for selecting best spelling
  correction.

## SymSpell

* The Symmetric Delete spelling correction algorithm reduces the complexity of edit candidate generation and dictionary
  lookup for a given Damerau-Levenshtein distance.
* It is six orders of magnitude faster (than the standard approach with deletes + transposes + replaces + inserts) and
  language independent.
* Opposite to other algorithms only deletes are required, no transposes + replaces + inserts. Transposes + replaces +
  inserts of the input term are transformed into deletes of the dictionary term.
* The speed comes from the inexpensive delete-only edit candidate generation and the pre-calculation.

## Customizations

* We replaced the **Damerau-Levenshtein** implementation with a **weighted Damerau-Levenshtein** implementation: where
  each operation (delete, insert, swap, replace) can have different edit weights.
* We added some customizing "hooks" that are used to rerank the top-k results (candidate list). The results are then
  reordered based on a combined proximity
  * added keyboard-distance to get a dynamic replacement weight (since letters close to each other are more likely to be
    replaced)
  * do some query normalization before search

## Keyboard based  Qwerty/Qwertz Distance

There are 2 implementations of the keyboards one is English Qwerty based and other is German Qwertz based implementation
we used the adjancey graph of the keyboard for the weights to the connected nodes.
<img src="qwerty.png" align="center">

### Example

```
For 2 terms: 
        slices  
        olives

If the misspelled word is, slives 
both slices and olives is 1 edit distnace, 
  so in default case the one with higher frequency will end up in the result.
While with the qwerty based char distance,
 slives is more closer to slices.

The reason for this is in Qwerty Based Keyboard, 
 S and O are too far while V and C are adjacent.
```

## Generation of Deletes

Word deletes are generated with taking edit distance which is minimum of max edit distance and 0.3 * word.length
