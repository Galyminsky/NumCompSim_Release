package me.proton.jobforandroid.compositionofnumbers.domain.entity

/** Only arithmetic needs a choice of level; all other exercise types have a fixed level. */
enum class FractionArithmeticLevel { NORMAL, HARD }

/** Conditions retain their original fractions; the correct answer is derived separately. */
sealed class FractionExercise<A : FractionAnswer> {
    abstract val correctAnswer: A

    data class IdentifyFraction(val totalParts: Int, val shadedParts: Int) :
        FractionExercise<FractionAnswer.FractionValue>() {
        init {
            require(FractionMathValidator.isValidTestParts(shadedParts, totalParts))
        }

        override val correctAnswer: FractionAnswer.FractionValue
            get() = FractionAnswer.FractionValue(Fraction(shadedParts, totalParts))
    }

    data class IdentifyNumerator(val source: Fraction) : FractionExercise<FractionAnswer.Integer>() {
        init {
            require(FractionMathValidator.isValidEasyFraction(source))
        }

        override val correctAnswer: FractionAnswer.Integer
            get() = FractionAnswer.Integer(source.numerator)
    }

    data class IdentifyDenominator(val source: Fraction) : FractionExercise<FractionAnswer.Integer>() {
        init {
            require(FractionMathValidator.isValidEasyFraction(source))
        }

        override val correctAnswer: FractionAnswer.Integer
            get() = FractionAnswer.Integer(source.denominator)
    }

    data class Compare(val left: Fraction, val right: Fraction) : FractionExercise<FractionAnswer.Sign>() {
        init {
            require(FractionMathValidator.isValidNormalComparison(left, right))
        }

        override val correctAnswer: FractionAnswer.Sign
            get() = FractionAnswer.Sign(when {
                left < right -> FractionAnswer.ComparisonValue.LESS
                left > right -> FractionAnswer.ComparisonValue.GREATER
                else -> FractionAnswer.ComparisonValue.EQUAL
            })
    }

    data class Reduce(val source: Fraction) : FractionExercise<FractionAnswer.FractionValue>() {
        init {
            require(FractionMathValidator.isValidNormalReduction(source, source.reduced()))
        }

        override val correctAnswer: FractionAnswer.FractionValue
            get() = FractionAnswer.FractionValue(source.reduced())
    }

    data class Add(val level: FractionArithmeticLevel, val left: Fraction, val right: Fraction) :
        FractionExercise<FractionAnswer.FractionValue>() {
        init {
            val answer = left + right
            require(when (level) {
                FractionArithmeticLevel.NORMAL -> FractionMathValidator.isValidNormalAddition(left, right, answer)
                FractionArithmeticLevel.HARD -> FractionMathValidator.isValidHardAddition(left, right, answer)
            })
        }

        override val correctAnswer: FractionAnswer.FractionValue
            get() = FractionAnswer.FractionValue(left + right)
    }

    data class Subtract(val level: FractionArithmeticLevel, val left: Fraction, val right: Fraction) :
        FractionExercise<FractionAnswer.FractionValue>() {
        init {
            val answer = left - right
            require(when (level) {
                FractionArithmeticLevel.NORMAL -> FractionMathValidator.isValidNormalSubtraction(left, right, answer)
                FractionArithmeticLevel.HARD -> FractionMathValidator.isValidHardSubtraction(left, right, answer)
            })
        }

        override val correctAnswer: FractionAnswer.FractionValue
            get() = FractionAnswer.FractionValue(left - right)
    }

    data class ToMixedNumber(val source: Fraction) : FractionExercise<FractionAnswer.MixedNumber>() {
        init {
            // Validate before constructing MixedNumber, which cannot represent a zero fractional part.
            require(FractionMathValidator.isValidHardConversion(
                source, source.numerator / source.denominator,
                Fraction(source.numerator % source.denominator, source.denominator).reduced()
            ))
        }

        override val correctAnswer: FractionAnswer.MixedNumber
            get() = FractionAnswer.MixedNumber(
                source.numerator / source.denominator,
                Fraction(source.numerator % source.denominator, source.denominator).reduced()
            )
    }
}
