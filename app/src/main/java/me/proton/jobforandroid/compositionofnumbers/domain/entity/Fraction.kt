package me.proton.jobforandroid.compositionofnumbers.domain.entity

/**
 * Preserves the supplied representation, including its sign and common factors.
 * Equality compares that representation; use [isEquivalentTo] for mathematical equality.
 * Arithmetic returns reduced fractions with a positive denominator, or throws
 * [ArithmeticException] when the reduced components cannot be represented as Int.
 * Level-specific restrictions belong to tasks, not this mathematical model.
 */
data class Fraction(val numerator: Int, val denominator: Int) : Comparable<Fraction> {
    init {
        require(denominator != 0) { "Denominator must not be zero" }
    }

    fun reduced(): Fraction = fromLongs(numerator.toLong(), denominator.toLong())

    fun isEquivalentTo(other: Fraction): Boolean = compareTo(other) == 0

    override fun compareTo(other: Fraction): Int {
        // Every product of two Int components fits in Long, including Int.MIN_VALUE.
        val left = numerator.toLong() * other.denominator.toLong()
        val right = other.numerator.toLong() * denominator.toLong()
        val comparison = left.compareTo(right)
        return if ((denominator < 0) != (other.denominator < 0)) -comparison else comparison
    }

    operator fun plus(other: Fraction): Fraction = combine(other, subtract = false)

    operator fun minus(other: Fraction): Fraction = combine(other, subtract = true)

    private fun combine(other: Fraction, subtract: Boolean): Fraction {
        val leftDenominator = positiveMagnitude(denominator.toLong())
        val rightDenominator = positiveMagnitude(other.denominator.toLong())
        val commonDivisor = gcd(leftDenominator, rightDenominator)
        val leftMultiplier = rightDenominator / commonDivisor
        val rightMultiplier = leftDenominator / commonDivisor
        val leftNumerator = if (denominator < 0) -numerator.toLong() else numerator.toLong()
        val rightNumerator = if (other.denominator < 0) -other.numerator.toLong() else other.numerator.toLong()
        // Magnitudes are at most 2^31 per factor, so products and negation fit in Long.
        val left = leftNumerator * leftMultiplier
        val right = rightNumerator * rightMultiplier
        val resultNumerator = checkedAdd(left, if (subtract) -right else right)
        return fromLongs(resultNumerator, leftDenominator * leftMultiplier)
    }

    private companion object {
        fun fromLongs(numerator: Long, denominator: Long): Fraction {
            val divisor = gcd(positiveMagnitude(numerator), positiveMagnitude(denominator))
            val sign = if (denominator < 0) -1L else 1L
            val reducedNumerator = numerator / divisor * sign
            val reducedDenominator = denominator / divisor * sign
            if (reducedNumerator !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ||
                reducedDenominator !in 1L..Int.MAX_VALUE.toLong()) {
                throw ArithmeticException("Reduced fraction components must fit in Int")
            }
            return Fraction(reducedNumerator.toInt(), reducedDenominator.toInt())
        }

        fun checkedAdd(left: Long, right: Long): Long {
            if ((right > 0 && left > Long.MAX_VALUE - right) ||
                (right < 0 && left < Long.MIN_VALUE - right)) {
                throw ArithmeticException("Fraction arithmetic overflow")
            }
            return left + right
        }

        fun positiveMagnitude(value: Long): Long {
            if (value == Long.MIN_VALUE) throw ArithmeticException("Fraction magnitude overflow")
            return if (value < 0) -value else value
        }

        fun gcd(first: Long, second: Long): Long {
            var a = first
            var b = second
            while (b != 0L) {
                val remainder = a % b
                a = b
                b = remainder
            }
            return a
        }
    }
}
