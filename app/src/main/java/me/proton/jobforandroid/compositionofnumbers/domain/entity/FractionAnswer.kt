package me.proton.jobforandroid.compositionofnumbers.domain.entity

/** Answer representations only; exercise-specific correctness is checked by the exercise. */
sealed class FractionAnswer {
    data class Integer(val value: Int) : FractionAnswer()

    data class FractionValue(val value: Fraction) : FractionAnswer()

    /** Does not reduce the supplied part or impose the correct answer's whole-part range on options. */
    data class MixedNumber(val wholePart: Int, val fractionalPart: Fraction) : FractionAnswer() {
        init {
            require(fractionalPart.numerator > 0 && fractionalPart.numerator < fractionalPart.denominator) {
                "A mixed number requires a positive proper fractional part"
            }
        }
    }

    enum class ComparisonValue(val comparison: Int) { LESS(-1), EQUAL(0), GREATER(1) }

    data class Sign(val value: ComparisonValue) : FractionAnswer()
}
