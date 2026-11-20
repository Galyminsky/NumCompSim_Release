package me.proton.jobforandroid.compositionofnumbers.domain.entity

/** Mathematical data checks only; task formats, option sets and session quotas are separate. */
object FractionMathValidator {
    fun isValidTestFraction(fraction: Fraction): Boolean = isProperSource(fraction, 7)

    fun isValidTestParts(shadedParts: Int, totalParts: Int): Boolean =
        totalParts in 2..7 && shadedParts in 1 until totalParts

    fun isCorrectTestAnswer(shadedParts: Int, totalParts: Int, answer: Fraction): Boolean =
        isValidTestParts(shadedParts, totalParts) &&
            answer.isEquivalentTo(Fraction(shadedParts, totalParts))

    fun isValidEasyFraction(fraction: Fraction): Boolean = isProperSource(fraction, 10)

    fun isCorrectEasyNumerator(fraction: Fraction, answer: Int): Boolean =
        isValidEasyFraction(fraction) && answer == fraction.numerator

    fun isCorrectEasyDenominator(fraction: Fraction, answer: Int): Boolean =
        isValidEasyFraction(fraction) && answer == fraction.denominator

    fun isValidNormalComparison(left: Fraction, right: Fraction): Boolean =
        isProperSource(left, 12) && isProperSource(right, 12)

    /** [sign] is -1 for '<', 0 for '=', and 1 for '>'. */
    fun isCorrectNormalComparison(left: Fraction, right: Fraction, sign: Int): Boolean =
        sign in -1..1 && isValidNormalComparison(left, right) && left.compareTo(right) == sign

    fun isValidNormalReduction(source: Fraction, answer: Fraction): Boolean {
        if (!isProperSource(source, 12)) return false
        val reduced = source.reduced()
        return source != reduced && answer == reduced
    }

    fun isValidNormalAddition(left: Fraction, right: Fraction, answer: Fraction): Boolean =
        normalArithmeticOperands(left, right) && isProperReduced(answer) &&
            answer.isEquivalentTo(left + right)

    fun isValidNormalSubtraction(left: Fraction, right: Fraction, answer: Fraction): Boolean =
        normalArithmeticOperands(left, right) && isProperReduced(answer) &&
            answer.isEquivalentTo(left - right)

    fun isValidHardAddition(left: Fraction, right: Fraction, answer: Fraction): Boolean =
        hardArithmeticOperands(left, right) && isPositiveReducedNoninteger(answer) &&
            answer.isEquivalentTo(left + right)

    fun isValidHardSubtraction(left: Fraction, right: Fraction, answer: Fraction): Boolean =
        hardArithmeticOperands(left, right) && isPositiveReducedNoninteger(answer) &&
            answer.isEquivalentTo(left - right)

    fun isValidHardConversion(source: Fraction, wholePart: Int, fractionalPart: Fraction): Boolean {
        if (source.denominator !in 2..15 || source.numerator <= source.denominator ||
            wholePart !in 1..3 || wholePart != source.numerator / source.denominator) return false
        val remainder = source.numerator % source.denominator
        return remainder != 0 && fractionalPart == Fraction(remainder, source.denominator).reduced()
    }

    private fun isProperSource(fraction: Fraction, maxDenominator: Int): Boolean =
        fraction.denominator in 2..maxDenominator &&
            fraction.numerator in 1 until fraction.denominator

    private fun normalArithmeticOperands(left: Fraction, right: Fraction): Boolean =
        isProperSource(left, 12) && isProperSource(right, 12) &&
            (left.denominator == right.denominator || lcm(left.denominator, right.denominator) <= 24)

    private fun hardArithmeticOperands(left: Fraction, right: Fraction): Boolean =
        isProperSource(left, 15) && isProperSource(right, 15) &&
            left.denominator != right.denominator && lcm(left.denominator, right.denominator) <= 60

    private fun isProperReduced(fraction: Fraction): Boolean =
        fraction.numerator > 0 && fraction.numerator < fraction.denominator &&
            fraction == fraction.reduced()

    private fun isPositiveReducedNoninteger(fraction: Fraction): Boolean =
        fraction.numerator > 0 && fraction.denominator > 1 && fraction == fraction.reduced()

    // Called only after validating source denominators in 2..15; every intermediate fits in Int.
    private fun lcm(first: Int, second: Int): Int {
        var a = first
        var b = second
        while (b != 0) {
            val remainder = a % b
            a = b
            b = remainder
        }
        return (first / a) * second
    }
}
