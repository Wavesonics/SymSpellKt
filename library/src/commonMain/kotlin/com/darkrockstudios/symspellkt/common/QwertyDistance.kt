package com.darkrockstudios.symspellkt.common

import com.darkrockstudios.symspellkt.api.CharDistance

/**
 * Qwerty distance to get the distance between two chars, the default distance is 1.5f It uses 0.1
 * for direct connect and 0.4f for the diagonal connect. Adjacency matrix of the Qwerty is computed
 * using
 * [Qwerty Adjacency Map](http://www.nada.kth.se/~ann/exjobb/axel_samuelsson.pdf)
 */
class QwertyDistance : CharDistance {
	private val directConnect: Double = 0.1
	private val diagonalConnect: Double = 0.4
	private val defaultValue: Double = 1.0

	private var operationCost: Array<DoubleArray> = Array('z'.code + 1) { DoubleArray('z'.code + 1) }

	init {
		this.initializeCostMatrix()
	}

	override fun distance(a: Char, b: Char): Double {
		if (a > 'z' || b > 'z') {
			return defaultValue
		}
		return operationCost[a.code][b.code]
	}

	/**
	 * Initializing the cost matrix
	 */
	private fun initializeCostMatrix() {
		for (row in this.operationCost) {
			row.fill(defaultValue)
		}

		var i = 'a'.code
		while (i <= 'z'.code) {
			var j = 'a'.code
			while (j <= 'z'.code) {
				if (i == j) {
					operationCost[i][j] = 0.0
				}
				j++
			}
			i++
		}

		operationCost['a'.code]['s'.code] = directConnect
		operationCost['a'.code]['w'.code] = diagonalConnect
		operationCost['a'.code]['q'.code] = diagonalConnect
		operationCost['a'.code]['z'.code] = diagonalConnect

		operationCost['s'.code]['a'.code] = directConnect
		operationCost['s'.code]['d'.code] = directConnect
		operationCost['s'.code]['w'.code] = diagonalConnect
		operationCost['s'.code]['e'.code] = diagonalConnect
		operationCost['s'.code]['x'.code] = diagonalConnect
		operationCost['s'.code]['z'.code] = diagonalConnect

		operationCost['d'.code]['s'.code] = directConnect
		operationCost['d'.code]['f'.code] = directConnect
		operationCost['d'.code]['e'.code] = diagonalConnect
		operationCost['d'.code]['r'.code] = diagonalConnect
		operationCost['d'.code]['c'.code] = diagonalConnect
		operationCost['d'.code]['x'.code] = diagonalConnect

		operationCost['f'.code]['d'.code] = directConnect
		operationCost['f'.code]['g'.code] = directConnect
		operationCost['f'.code]['r'.code] = diagonalConnect
		operationCost['f'.code]['t'.code] = diagonalConnect
		operationCost['f'.code]['c'.code] = diagonalConnect
		operationCost['f'.code]['v'.code] = diagonalConnect

		operationCost['g'.code]['f'.code] = directConnect
		operationCost['g'.code]['h'.code] = directConnect
		operationCost['g'.code]['t'.code] = diagonalConnect
		operationCost['g'.code]['y'.code] = diagonalConnect
		operationCost['g'.code]['v'.code] = diagonalConnect
		operationCost['g'.code]['b'.code] = diagonalConnect

		operationCost['h'.code]['g'.code] = directConnect
		operationCost['h'.code]['j'.code] = directConnect
		operationCost['h'.code]['y'.code] = diagonalConnect
		operationCost['h'.code]['u'.code] = diagonalConnect
		operationCost['h'.code]['b'.code] = diagonalConnect
		operationCost['h'.code]['n'.code] = diagonalConnect

		operationCost['j'.code]['h'.code] = directConnect
		operationCost['j'.code]['k'.code] = directConnect
		operationCost['j'.code]['u'.code] = diagonalConnect
		operationCost['j'.code]['i'.code] = diagonalConnect
		operationCost['j'.code]['n'.code] = diagonalConnect
		operationCost['j'.code]['m'.code] = diagonalConnect

		operationCost['k'.code]['j'.code] = directConnect
		operationCost['k'.code]['l'.code] = directConnect
		operationCost['k'.code]['i'.code] = diagonalConnect
		operationCost['k'.code]['o'.code] = diagonalConnect
		operationCost['k'.code]['m'.code] = diagonalConnect

		operationCost['l'.code]['k'.code] = directConnect
		operationCost['l'.code]['o'.code] = diagonalConnect
		operationCost['l'.code]['p'.code] = diagonalConnect

		operationCost['q'.code]['w'.code] = directConnect
		operationCost['q'.code]['a'.code] = diagonalConnect

		operationCost['w'.code]['q'.code] = directConnect
		operationCost['w'.code]['e'.code] = directConnect
		operationCost['w'.code]['a'.code] = diagonalConnect
		operationCost['w'.code]['s'.code] = diagonalConnect

		operationCost['e'.code]['w'.code] = directConnect
		operationCost['e'.code]['r'.code] = directConnect
		operationCost['e'.code]['s'.code] = diagonalConnect
		operationCost['e'.code]['d'.code] = diagonalConnect

		operationCost['r'.code]['e'.code] = directConnect
		operationCost['r'.code]['t'.code] = directConnect
		operationCost['r'.code]['d'.code] = diagonalConnect
		operationCost['r'.code]['f'.code] = diagonalConnect

		operationCost['t'.code]['r'.code] = directConnect
		operationCost['t'.code]['y'.code] = directConnect
		operationCost['t'.code]['f'.code] = diagonalConnect
		operationCost['t'.code]['g'.code] = diagonalConnect

		operationCost['y'.code]['t'.code] = directConnect
		operationCost['y'.code]['u'.code] = directConnect
		operationCost['y'.code]['g'.code] = diagonalConnect
		operationCost['y'.code]['h'.code] = diagonalConnect

		operationCost['u'.code]['y'.code] = directConnect
		operationCost['u'.code]['i'.code] = directConnect
		operationCost['u'.code]['h'.code] = diagonalConnect
		operationCost['u'.code]['j'.code] = diagonalConnect

		operationCost['i'.code]['u'.code] = directConnect
		operationCost['i'.code]['o'.code] = directConnect
		operationCost['i'.code]['j'.code] = diagonalConnect
		operationCost['i'.code]['k'.code] = diagonalConnect

		operationCost['o'.code]['i'.code] = directConnect
		operationCost['o'.code]['p'.code] = directConnect
		operationCost['o'.code]['k'.code] = diagonalConnect
		operationCost['o'.code]['l'.code] = diagonalConnect

		operationCost['p'.code]['o'.code] = directConnect
		operationCost['p'.code]['l'.code] = diagonalConnect

		operationCost['z'.code]['x'.code] = directConnect
		operationCost['z'.code]['s'.code] = diagonalConnect
		operationCost['z'.code]['a'.code] = diagonalConnect

		operationCost['x'.code]['z'.code] = directConnect
		operationCost['x'.code]['c'.code] = directConnect
		operationCost['x'.code]['s'.code] = diagonalConnect
		operationCost['x'.code]['d'.code] = diagonalConnect

		operationCost['c'.code]['x'.code] = directConnect
		operationCost['c'.code]['v'.code] = directConnect
		operationCost['c'.code]['d'.code] = diagonalConnect
		operationCost['c'.code]['f'.code] = diagonalConnect

		operationCost['v'.code]['b'.code] = directConnect
		operationCost['v'.code]['c'.code] = directConnect
		operationCost['v'.code]['f'.code] = diagonalConnect
		operationCost['v'.code]['g'.code] = diagonalConnect

		operationCost['b'.code]['v'.code] = directConnect
		operationCost['b'.code]['n'.code] = directConnect
		operationCost['b'.code]['g'.code] = diagonalConnect
		operationCost['b'.code]['h'.code] = diagonalConnect

		operationCost['n'.code]['b'.code] = directConnect
		operationCost['n'.code]['m'.code] = directConnect
		operationCost['n'.code]['h'.code] = diagonalConnect
		operationCost['n'.code]['j'.code] = diagonalConnect

		operationCost['m'.code]['n'.code] = directConnect
		operationCost['m'.code]['j'.code] = diagonalConnect
		operationCost['m'.code]['k'.code] = diagonalConnect
	}
}

