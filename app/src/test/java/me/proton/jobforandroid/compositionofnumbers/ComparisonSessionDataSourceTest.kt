package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.data.ComparisonSessionDataSource
import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonCanonicalKey
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestionValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComparisonSessionDataSourceTest {
    private val source = ComparisonSessionDataSource()

    @Test
    fun providesCompleteSetsForEveryLevel() {
        val perSign = mapOf(
            ComparisonLevel.TEST to 8,
            ComparisonLevel.EASY to 15,
            ComparisonLevel.NORMAL to 12,
            ComparisonLevel.HARD to 10
        )
        for ((level, count) in perSign) {
            val questions = source.generateQuestions(level, Random(1234))
            assertEquals(count * 3, questions.size)
            assertTrue(questions.all { it.level == level && ComparisonQuestionValidator.isValid(it) })
            for (sign in ComparisonSign.entries) {
                assertEquals(count, questions.count { it.correctSign == sign })
            }
            assertEquals(questions.size, questions.map(ComparisonCanonicalKey::of).toSet().size)
        }
    }

    @Test
    fun sequentialRequestsAreIndependent() {
        for (level in ComparisonLevel.entries) {
            val first = source.generateQuestions(level, Random(17))
            source.generateQuestions(level, Random(29))
            val second = source.generateQuestions(level, Random(17))
            assertNotSame(first, second)
            assertEquals(first, second)
        }
    }

    @Test
    fun sameSeedIsReproducibleAcrossDataSourceInstances() {
        for (level in ComparisonLevel.entries) {
            assertEquals(
                source.generateQuestions(level, Random(901)),
                ComparisonSessionDataSource().generateQuestions(level, Random(901))
            )
        }
    }

    @Test
    fun existingArithmeticOperationsRemainAvailable() {
        source.generateQuestions(ComparisonLevel.TEST, Random(1))
        val repository = GameRepositoryImpl
        for (level in Level.entries) {
            val settings = repository.getGameSettings(level)
            val maximum = settings.maxSumValue
            val questions = listOf(
                repository.generateQuestion(maximum, 6),
                repository.generateSubtractionQuestion(maximum, 6),
                repository.generateMultiplicationQuestion(level, 6),
                repository.generateDivisionQuestion(level, 6)
            )
            assertEquals(Question.Operation.entries, questions.map { it.operation })
            assertTrue(questions.all { it.options.size == 6 && it.rightAnswer in it.options })
        }
    }
}
