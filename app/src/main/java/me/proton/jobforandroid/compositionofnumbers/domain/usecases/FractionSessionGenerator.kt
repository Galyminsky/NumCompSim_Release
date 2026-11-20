package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionCanonicalKey
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import kotlin.random.Random

/** Builds a complete validated question set synchronously; it does not start or own a timer. */
object FractionSessionGenerator {
    enum class Level { TEST, EASY, NORMAL, HARD }

    fun generate(level: Level, random: Random): List<FractionQuestion<out FractionAnswer>> {
        val questions = mutableListOf<FractionQuestion<out FractionAnswer>>()
        val used = HashSet<String>()
        fun unused(exercise: FractionExercise<out FractionAnswer>): Boolean = FractionCanonicalKey.of(exercise) !in used
        fun append(question: FractionQuestion<out FractionAnswer>) {
            check(used.add(FractionCanonicalKey.of(question.exercise))) { "Duplicate fraction session question" }
            questions.add(question)
        }

        when (level) {
            Level.TEST -> repeat(8) { append(FractionTestQuestionGenerator.generate(random, ::unused)) }
            Level.EASY -> {
                repeat(8) {
                    append(FractionEasyQuestionGenerator.generate(FractionEasyQuestionGenerator.ExerciseType.NUMERATOR,
                        random, ::unused))
                }
                repeat(7) {
                    append(FractionEasyQuestionGenerator.generate(FractionEasyQuestionGenerator.ExerciseType.DENOMINATOR,
                        random, ::unused))
                }
            }
            Level.NORMAL -> {
                for (sign in FractionAnswer.ComparisonValue.values()) {
                    append(FractionNormalQuestionGenerator.generate(FractionNormalQuestionGenerator.ExerciseType.COMPARE,
                        random) { exercise ->
                        exercise is FractionExercise.Compare && exercise.correctAnswer.value == sign && unused(exercise)
                    })
                }
                for (type in listOf(FractionNormalQuestionGenerator.ExerciseType.REDUCE,
                    FractionNormalQuestionGenerator.ExerciseType.ADD, FractionNormalQuestionGenerator.ExerciseType.SUBTRACT)) {
                    repeat(3) { append(FractionNormalQuestionGenerator.generate(type, random, ::unused)) }
                }
            }
            Level.HARD -> {
                repeat(3) {
                    append(FractionHardQuestionGenerator.generate(FractionHardQuestionGenerator.ExerciseType.ADD,
                        random, ::unused))
                }
                repeat(3) {
                    append(FractionHardQuestionGenerator.generate(FractionHardQuestionGenerator.ExerciseType.SUBTRACT,
                        random, ::unused))
                }
                repeat(4) {
                    append(FractionHardQuestionGenerator.generate(FractionHardQuestionGenerator.ExerciseType.TO_MIXED_NUMBER,
                        random, ::unused))
                }
            }
        }
        // No partially built list escapes on any failure above.
        return questions.shuffled(random)
    }
}
