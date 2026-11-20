package me.proton.jobforandroid.compositionofnumbers.domain.entity

/** Session identity of mathematical conditions; option order and answer representation are irrelevant. */
object FractionCanonicalKey {
    fun of(exercise: FractionExercise<out FractionAnswer>): String = when (exercise) {
        is FractionExercise.IdentifyFraction -> "TEST:${value(Fraction(exercise.shadedParts, exercise.totalParts))}"
        // EASY uniqueness spans both component exercises and preserves the original record.
        is FractionExercise.IdentifyNumerator -> "EASY:${record(exercise.source)}"
        is FractionExercise.IdentifyDenominator -> "EASY:${record(exercise.source)}"
        is FractionExercise.Compare -> "NORMAL:COMPARE:${unordered(exercise.left, exercise.right)}"
        is FractionExercise.Reduce -> "NORMAL:REDUCE:${value(exercise.source)}"
        is FractionExercise.Add -> "${exercise.level}:ADD:${unordered(exercise.left, exercise.right)}"
        is FractionExercise.Subtract -> "${exercise.level}:SUBTRACT:${value(exercise.left)}|${value(exercise.right)}"
        is FractionExercise.ToMixedNumber -> "HARD:TO_MIXED_NUMBER:${value(exercise.source)}"
    }

    private fun value(fraction: Fraction): String = record(fraction.reduced())

    private fun record(fraction: Fraction): String = "${fraction.numerator}/${fraction.denominator}"

    private fun unordered(left: Fraction, right: Fraction): String {
        val first = value(left)
        val second = value(right)
        return if (first <= second) "$first|$second" else "$second|$first"
    }
}
