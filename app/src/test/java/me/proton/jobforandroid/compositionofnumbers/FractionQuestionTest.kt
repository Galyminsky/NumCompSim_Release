package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionArithmeticLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FractionQuestionTest {
    private fun fraction(numerator: Int, denominator: Int) =
        FractionAnswer.FractionValue(Fraction(numerator, denominator))

    @Test fun testPictureStoresCountsAndReturnsFractionWithoutReducingIt() {
        val exercise = FractionExercise.IdentifyFraction(totalParts = 4, shadedParts = 2)
        val question: FractionQuestion<FractionAnswer.FractionValue> = FractionQuestion(
            exercise, listOf(fraction(1, 3), fraction(1, 2), fraction(1, 4), fraction(3, 4))
        )
        assertEquals(4, exercise.totalParts)
        assertEquals(2, exercise.shadedParts)
        assertEquals(fraction(2, 4), question.correctAnswer)
        assertEquals(fraction(1, 2), question.options[1])
    }

    @Test fun easyExercisesUseOriginalNumeratorAndDenominator() {
        val source = Fraction(6, 8)
        val numerator: FractionQuestion<FractionAnswer.Integer> = FractionQuestion(
            FractionExercise.IdentifyNumerator(source), listOf(3, 6, 8, 4).map { FractionAnswer.Integer(it) }
        )
        val denominator: FractionQuestion<FractionAnswer.Integer> = FractionQuestion(
            FractionExercise.IdentifyDenominator(source), listOf(4, 6, 8, 3).map { FractionAnswer.Integer(it) }
        )
        assertEquals(FractionAnswer.Integer(6), numerator.correctAnswer)
        assertEquals(FractionAnswer.Integer(8), denominator.correctAnswer)
        assertEquals(Fraction(6, 8), (numerator.exercise as FractionExercise.IdentifyNumerator).source)
        assertEquals(Fraction(6, 8), (denominator.exercise as FractionExercise.IdentifyDenominator).source)
    }

    @Test fun normalComparisonUsesTypedSignsAndMathematicalEquality() {
        val exercise = FractionExercise.Compare(Fraction(2, 4), Fraction(1, 2))
        val options = listOf(FractionAnswer.ComparisonValue.GREATER,
            FractionAnswer.ComparisonValue.EQUAL, FractionAnswer.ComparisonValue.LESS).map { FractionAnswer.Sign(it) }
        val question: FractionQuestion<FractionAnswer.Sign> = FractionQuestion(exercise, options)
        assertEquals(FractionAnswer.Sign(FractionAnswer.ComparisonValue.EQUAL), question.correctAnswer)
        assertEquals(options, question.options)
        assertEquals(Fraction(2, 4), exercise.left)
        assertEquals(FractionAnswer.Sign(FractionAnswer.ComparisonValue.LESS),
            FractionExercise.Compare(Fraction(1, 3), Fraction(1, 2)).correctAnswer)
        assertEquals(FractionAnswer.Sign(FractionAnswer.ComparisonValue.GREATER),
            FractionExercise.Compare(Fraction(3, 4), Fraction(1, 2)).correctAnswer)
    }

    @Test fun reductionSeparatesOriginalConditionFromReducedCorrectAnswer() {
        val exercise = FractionExercise.Reduce(Fraction(6, 8))
        val question = FractionQuestion(exercise,
            listOf(fraction(3, 8), fraction(3, 4), fraction(2, 3), fraction(1, 2)))
        assertEquals(Fraction(6, 8), exercise.source)
        assertEquals(fraction(3, 4), question.correctAnswer)
    }

    @Test fun arithmeticSupportsBothLevelsAndOnlyFractionAnswers() {
        val exercises: List<Pair<FractionExercise<FractionAnswer.FractionValue>, FractionAnswer.FractionValue>> = listOf(
            FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(1, 3), Fraction(1, 8)) to fraction(11, 24),
            FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(3, 4), Fraction(1, 4)) to fraction(1, 2),
            FractionExercise.Add(FractionArithmeticLevel.HARD, Fraction(1, 2), Fraction(3, 4)) to fraction(5, 4),
            FractionExercise.Subtract(FractionArithmeticLevel.HARD, Fraction(14, 15), Fraction(1, 12)) to fraction(17, 20)
        )
        exercises.forEach { (exercise, expected) ->
            val question: FractionQuestion<FractionAnswer.FractionValue> = FractionQuestion(
                exercise, listOf(expected, fraction(1, 7), fraction(2, 7), fraction(3, 7))
            )
            assertEquals(expected, question.correctAnswer)
        }
    }

    @Test fun arithmeticDoesNotReduceStoredOperands() {
        val addition = FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(2, 4), Fraction(2, 8))
        assertEquals(Fraction(2, 4), addition.left)
        assertEquals(Fraction(2, 8), addition.right)
        assertEquals(fraction(3, 4), addition.correctAnswer)
    }

    @Test fun hardConversionStoresOriginalAndReturnsMixedNumber() {
        val exercise = FractionExercise.ToMixedNumber(Fraction(14, 4))
        val correct = FractionAnswer.MixedNumber(3, Fraction(1, 2))
        val question: FractionQuestion<FractionAnswer.MixedNumber> = FractionQuestion(exercise, listOf(
            FractionAnswer.MixedNumber(1, Fraction(1, 2)), correct,
            FractionAnswer.MixedNumber(2, Fraction(1, 2)), FractionAnswer.MixedNumber(4, Fraction(1, 2))
        ))
        assertEquals(Fraction(14, 4), exercise.source)
        assertEquals(correct, question.correctAnswer)
        assertEquals(3, question.correctAnswer.wholePart)
        assertEquals(Fraction(1, 2), question.correctAnswer.fractionalPart)
    }

    @Test fun mixedAnswerPreservesFractionalRecordAndRequiresProperFractionalPart() {
        assertEquals(Fraction(2, 4), FractionAnswer.MixedNumber(2, Fraction(2, 4)).fractionalPart)
        listOf(Fraction(0, 1), Fraction(1, 1), Fraction(3, 2), Fraction(-1, 2), Fraction(-1, -2)).forEach {
            assertThrows(IllegalArgumentException::class.java) { FractionAnswer.MixedNumber(2, it) }
        }
    }

    @Test fun optionOrderIsPreservedAndInputListIsCopied() {
        val options = mutableListOf(fraction(1, 4), fraction(1, 2), fraction(3, 4), fraction(1, 3))
        val original = options.toList()
        val question = FractionQuestion(FractionExercise.IdentifyFraction(4, 2), options)
        options.reverse()
        options.clear()
        assertEquals(original, question.options)
        assertEquals(fraction(2, 4), question.correctAnswer)
    }

    @Test fun optionCountMatchesExerciseFormat() {
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.IdentifyNumerator(Fraction(1, 2)),
                listOf(FractionAnswer.Integer(1), FractionAnswer.Integer(2), FractionAnswer.Integer(3)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Compare(Fraction(1, 2), Fraction(1, 3)),
                listOf(FractionAnswer.Sign(FractionAnswer.ComparisonValue.LESS)))
        }
    }

    @Test fun invalidConditionsAreRejectedByExistingMathValidators() {
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.IdentifyFraction(4, 4) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.IdentifyFraction(0, 1) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.IdentifyNumerator(Fraction(1, 11)) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.IdentifyDenominator(Fraction(0, 2)) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.Compare(Fraction(1, 13), Fraction(1, 2)) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.Reduce(Fraction(1, 2)) }
        assertThrows(IllegalArgumentException::class.java) {
            FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(1, 2), Fraction(1, 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(1, 3), Fraction(1, 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionExercise.Add(FractionArithmeticLevel.HARD, Fraction(1, 4), Fraction(1, 4))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionExercise.Subtract(FractionArithmeticLevel.HARD, Fraction(1, 2), Fraction(2, 4))
        }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.ToMixedNumber(Fraction(4, 2)) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.ToMixedNumber(Fraction(9, 2)) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.ToMixedNumber(Fraction(1, 2)) }
        assertThrows(IllegalArgumentException::class.java) { FractionExercise.ToMixedNumber(Fraction(17, 16)) }
    }

    @Test fun missingCorrectOptionsAreRejectedForEveryAnswerFormat() {
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.IdentifyFraction(4, 2),
                listOf(fraction(1, 3), fraction(1, 4), fraction(3, 4), fraction(1, 5)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.IdentifyNumerator(Fraction(6, 8)),
                listOf(1, 2, 3, 4).map { FractionAnswer.Integer(it) })
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.ToMixedNumber(Fraction(14, 4)),
                listOf(1, 2, 4, 5).map { FractionAnswer.MixedNumber(it, Fraction(1, 2)) })
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Compare(Fraction(2, 4), Fraction(1, 2)), listOf(
                FractionAnswer.Sign(FractionAnswer.ComparisonValue.LESS),
                FractionAnswer.Sign(FractionAnswer.ComparisonValue.GREATER),
                FractionAnswer.Sign(FractionAnswer.ComparisonValue.GREATER)))
        }
    }

    @Test fun duplicateNumericOptionsAreRejectedEvenWhenTheyAreWrong() {
        val exercise = FractionExercise.IdentifyNumerator(Fraction(6, 8))
        listOf(listOf(6, 6, 3, 4), listOf(6, 3, 3, 4)).forEach { values ->
            assertThrows(IllegalArgumentException::class.java) {
                FractionQuestion(exercise, values.map { FractionAnswer.Integer(it) })
            }
        }
    }

    @Test fun duplicateAndEquivalentFractionsAreRejectedForCorrectAndWrongOptions() {
        val exercise = FractionExercise.IdentifyFraction(4, 2)
        val invalidSets = listOf(
            listOf(fraction(1, 2), fraction(1, 2), fraction(1, 3), fraction(1, 4)),
            listOf(fraction(1, 2), fraction(2, 4), fraction(1, 3), fraction(1, 4)),
            listOf(fraction(1, 2), fraction(1, 3), fraction(2, 6), fraction(1, 4)),
            listOf(fraction(1, 2), fraction(1, 3), fraction(-1, -3), fraction(1, 4))
        )
        invalidSets.forEach { options ->
            assertThrows(IllegalArgumentException::class.java) { FractionQuestion(exercise, options) }
        }
    }

    @Test fun comparisonRequiresEverySignOnceAndUsesMathematicalValue() {
        val options = listOf(FractionAnswer.ComparisonValue.GREATER,
            FractionAnswer.ComparisonValue.LESS, FractionAnswer.ComparisonValue.EQUAL).map { FractionAnswer.Sign(it) }
        val cases = listOf(
            FractionExercise.Compare(Fraction(1, 3), Fraction(1, 2)) to FractionAnswer.ComparisonValue.LESS,
            FractionExercise.Compare(Fraction(2, 4), Fraction(1, 2)) to FractionAnswer.ComparisonValue.EQUAL,
            FractionExercise.Compare(Fraction(3, 4), Fraction(1, 2)) to FractionAnswer.ComparisonValue.GREATER
        )
        cases.forEach { (exercise, expected) ->
            assertEquals(FractionAnswer.Sign(expected), FractionQuestion(exercise, options).correctAnswer)
            assertEquals(options.reversed(), FractionQuestion(exercise, options.reversed()).options)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(cases[1].first, listOf(options[0], options[2], options[2]))
        }
    }

    @Test fun reductionRejectsSoleEquivalentButIncompletelyReducedAnswer() {
        val exercise = FractionExercise.Reduce(Fraction(6, 12))
        val wrong = listOf(fraction(1, 3), fraction(1, 4), fraction(3, 4))
        assertEquals(fraction(1, 2), FractionQuestion(exercise, listOf(fraction(1, 2)) + wrong).correctAnswer)
        listOf(fraction(2, 4), fraction(3, 6), fraction(6, 12)).forEach { incomplete ->
            assertThrows(IllegalArgumentException::class.java) {
                FractionQuestion(exercise, listOf(incomplete) + wrong)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(exercise, listOf(fraction(1, 2), fraction(2, 4), fraction(1, 3), fraction(1, 4)))
        }
    }

    @Test fun arithmeticAlsoRequiresReducedCorrectOption() {
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(1, 4), Fraction(1, 4)),
                listOf(fraction(2, 4), fraction(1, 3), fraction(1, 4), fraction(3, 4)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(3, 4), Fraction(1, 4)),
                listOf(fraction(2, 4), fraction(1, 3), fraction(1, 4), fraction(3, 4)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Add(FractionArithmeticLevel.HARD, Fraction(1, 2), Fraction(3, 4)),
                listOf(fraction(10, 8), fraction(1, 3), fraction(1, 4), fraction(3, 4)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Subtract(FractionArithmeticLevel.HARD, Fraction(3, 4), Fraction(1, 2)),
                listOf(fraction(2, 8), fraction(1, 3), fraction(1, 2), fraction(3, 4)))
        }
    }

    @Test fun conversionRejectsIncompleteCorrectPartAndEquivalentMixedOptions() {
        val exercise = FractionExercise.ToMixedNumber(Fraction(14, 4))
        val correct = FractionAnswer.MixedNumber(3, Fraction(1, 2))
        val wrong = listOf(1, 2, 4).map { FractionAnswer.MixedNumber(it, Fraction(1, 2)) }
        assertEquals(correct, FractionQuestion(exercise, listOf(correct) + wrong).correctAnswer)
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(exercise, listOf(FractionAnswer.MixedNumber(3, Fraction(2, 4))) + wrong)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(exercise, listOf(correct, FractionAnswer.MixedNumber(3, Fraction(2, 4)), wrong[0], wrong[1]))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(exercise, listOf(correct, wrong[0], FractionAnswer.MixedNumber(1, Fraction(2, 4)), wrong[1]))
        }
    }

    @Test fun mixedDistractorsDoNotOverflowOrAcquireNewWholePartBounds() {
        val exercise = FractionExercise.ToMixedNumber(Fraction(3, 2))
        val correct = FractionAnswer.MixedNumber(1, Fraction(1, 2))
        val large = FractionAnswer.MixedNumber(Int.MAX_VALUE, Fraction(1, 2))
        val different = FractionAnswer.MixedNumber(Int.MAX_VALUE, Fraction(1, 3))
        val other = FractionAnswer.MixedNumber(4, Fraction(1, 2))
        assertEquals(correct, FractionQuestion(exercise, listOf(correct, large, different, other)).correctAnswer)
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(exercise, listOf(correct, large,
                FractionAnswer.MixedNumber(Int.MAX_VALUE, Fraction(2, 4)), other))
        }
    }

    @Test fun wrongRuntimeFormatsAreRejectedDespiteErasedGenericLists() {
        // Simulates a caller crossing an unchecked boundary; production validation uses no unchecked cast.
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.IdentifyFraction(4, 2), erasedOptions<FractionAnswer.FractionValue>(
                fraction(1, 2), fraction(1, 3), fraction(1, 4), FractionAnswer.Integer(3)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.IdentifyNumerator(Fraction(1, 2)), erasedOptions<FractionAnswer.Integer>(
                FractionAnswer.Integer(1), FractionAnswer.Integer(2), FractionAnswer.Integer(3), fraction(1, 2)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Compare(Fraction(1, 2), Fraction(1, 3)), erasedOptions<FractionAnswer.Sign>(
                FractionAnswer.Sign(FractionAnswer.ComparisonValue.GREATER),
                FractionAnswer.Sign(FractionAnswer.ComparisonValue.LESS), FractionAnswer.Integer(0)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.ToMixedNumber(Fraction(3, 2)), erasedOptions<FractionAnswer.MixedNumber>(
                FractionAnswer.MixedNumber(1, Fraction(1, 2)), FractionAnswer.MixedNumber(2, Fraction(1, 2)),
                FractionAnswer.MixedNumber(3, Fraction(1, 2)), fraction(3, 2)))
        }
    }

    @Test fun emptyAndOversizedOptionSetsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.IdentifyDenominator(Fraction(1, 2)), emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.IdentifyDenominator(Fraction(1, 2)),
                (1..5).map { FractionAnswer.Integer(it) })
        }
        assertThrows(IllegalArgumentException::class.java) {
            FractionQuestion(FractionExercise.Compare(Fraction(1, 2), Fraction(1, 3)),
                listOf(FractionAnswer.ComparisonValue.LESS, FractionAnswer.ComparisonValue.EQUAL,
                    FractionAnswer.ComparisonValue.GREATER, FractionAnswer.ComparisonValue.LESS).map { FractionAnswer.Sign(it) })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <A : FractionAnswer> erasedOptions(vararg answers: FractionAnswer): List<A> = answers.toList() as List<A>
}
