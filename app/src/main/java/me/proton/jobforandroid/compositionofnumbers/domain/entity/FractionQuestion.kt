package me.proton.jobforandroid.compositionofnumbers.domain.entity

/**
 * An exercise and its displayed options. Invariant A binds options to the exercise's answer format.
 * Construction checks formats, mathematical uniqueness and a single correct option.
 * Pedagogical plausibility of distractors is not inferred here.
 */
class FractionQuestion<A : FractionAnswer>(val exercise: FractionExercise<A>, options: List<A>) {
    val options: List<A> = options.toList()
    val correctAnswer: A get() = exercise.correctAnswer

    init {
        require(this.options.size == if (exercise is FractionExercise.Compare) 3 else 4) {
            "Comparison requires three options; other exercises require four"
        }
        val expected = correctAnswer
        require(this.options.all { sameFormat(it, expected) }) {
            "Every option must have the exercise's answer format"
        }
        for (index in this.options.indices) {
            for (otherIndex in 0 until index) {
                require(!equivalent(this.options[index], this.options[otherIndex])) {
                    "Options must not be identical or mathematically equivalent"
                }
            }
        }
        val matching = this.options.filter { equivalent(it, expected) }
        require(matching.size == 1) { "Exactly one mathematically correct option is required" }
        // TEST accepts equivalent fraction records. Other correct fraction/mixed answers
        // must match the reduced representation already guaranteed by the exercise.
        require(exercise is FractionExercise.IdentifyFraction || matching.single() == expected) {
            "The correct option must use the exercise's reduced answer representation"
        }
    }

    private fun sameFormat(answer: FractionAnswer, expected: FractionAnswer): Boolean = when (expected) {
        is FractionAnswer.Integer -> answer is FractionAnswer.Integer
        is FractionAnswer.FractionValue -> answer is FractionAnswer.FractionValue
        is FractionAnswer.MixedNumber -> answer is FractionAnswer.MixedNumber
        is FractionAnswer.Sign -> answer is FractionAnswer.Sign
    }

    private fun equivalent(first: FractionAnswer, second: FractionAnswer): Boolean = when (first) {
        is FractionAnswer.Integer -> second is FractionAnswer.Integer && first.value == second.value
        is FractionAnswer.FractionValue -> second is FractionAnswer.FractionValue &&
            first.value.isEquivalentTo(second.value)
        // Proper fractional parts are in (0, 1), so equal mixed values require equal whole parts.
        // Comparing parts avoids overflowing Int when a distractor's whole part is large.
        is FractionAnswer.MixedNumber -> second is FractionAnswer.MixedNumber &&
            first.wholePart == second.wholePart && first.fractionalPart.isEquivalentTo(second.fractionalPart)
        is FractionAnswer.Sign -> second is FractionAnswer.Sign && first.value == second.value
    }
}
