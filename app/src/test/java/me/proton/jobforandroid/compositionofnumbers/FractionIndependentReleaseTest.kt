package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.*
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/** Independent integer oracle: no production reduction, equality or math validator. */
class FractionIndependentReleaseTest {
    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    private fun rational(n: Long, d: Long): Pair<Long, Long> {
        val g = gcd(n, d)
        return n / g to d / g
    }
    private fun value(f: Fraction) = rational(f.numerator.toLong(), f.denominator.toLong())
    private fun value(a: FractionAnswer): Any = when (a) {
        is FractionAnswer.Integer -> a.value
        is FractionAnswer.Sign -> a.value.comparison
        is FractionAnswer.FractionValue -> value(a.value)
        is FractionAnswer.MixedNumber -> rational(
            a.wholePart.toLong() * a.fractionalPart.denominator + a.fractionalPart.numerator,
            a.fractionalPart.denominator.toLong())
    }

    @Test fun generatedAnswersAgreeWithIndependentIntegerArithmetic() {
        for (level in FractionSessionGenerator.Level.values()) for (seed in listOf(0, 17, 31, 97)) {
            for (q in FractionSessionGenerator.generate(level, Random(seed))) {
                val sources = when (val e = q.exercise) {
                    is FractionExercise.IdentifyFraction -> listOf(Fraction(e.shadedParts, e.totalParts))
                    is FractionExercise.IdentifyNumerator -> listOf(e.source)
                    is FractionExercise.IdentifyDenominator -> listOf(e.source)
                    is FractionExercise.Reduce -> listOf(e.source)
                    is FractionExercise.ToMixedNumber -> listOf(e.source)
                    is FractionExercise.Compare -> listOf(e.left, e.right)
                    is FractionExercise.Add -> listOf(e.left, e.right)
                    is FractionExercise.Subtract -> listOf(e.left, e.right)
                }
                val max = when (level) {
                    FractionSessionGenerator.Level.TEST -> 7
                    FractionSessionGenerator.Level.EASY -> 10
                    FractionSessionGenerator.Level.NORMAL -> 12
                    FractionSessionGenerator.Level.HARD -> 15
                }
                sources.forEach {
                    assertTrue(it.denominator in 2..max)
                    assertTrue(it.numerator > 0)
                    if (q.exercise !is FractionExercise.ToMixedNumber) assertTrue(it.numerator < it.denominator)
                }
                val expected: Any = when (val e = q.exercise) {
                    is FractionExercise.IdentifyFraction -> value(sources.single())
                    is FractionExercise.IdentifyNumerator -> e.source.numerator
                    is FractionExercise.IdentifyDenominator -> e.source.denominator
                    is FractionExercise.Reduce -> value(e.source)
                    is FractionExercise.ToMixedNumber -> value(e.source)
                    is FractionExercise.Compare -> (e.left.numerator.toLong() * e.right.denominator)
                        .compareTo(e.right.numerator.toLong() * e.left.denominator)
                    else -> {
                        val a = sources[0]; val b = sources[1]
                        val d = a.denominator.toLong() * b.denominator
                        val n = a.numerator.toLong() * b.denominator +
                            (if (q.exercise is FractionExercise.Add) 1 else -1) * b.numerator.toLong() * a.denominator
                        assertTrue(n > 0 && n % d != 0L)
                        if (level == FractionSessionGenerator.Level.NORMAL) assertTrue(n < d)
                        assertTrue(d / gcd(a.denominator.toLong(), b.denominator.toLong()) <=
                            if (level == FractionSessionGenerator.Level.NORMAL) 24 else 60)
                        if (level == FractionSessionGenerator.Level.HARD) assertNotEquals(a.denominator, b.denominator)
                        rational(n, d)
                    }
                }
                assertEquals(expected, value(q.correctAnswer))
                assertEquals(1, q.options.count { value(it) == expected })
                assertEquals(q.options.size, q.options.map(::value).toSet().size)
                when (val answer = q.correctAnswer) {
                    is FractionAnswer.FractionValue -> if (q.exercise !is FractionExercise.IdentifyFraction)
                        assertEquals(1L, gcd(answer.value.numerator.toLong(), answer.value.denominator.toLong()))
                    is FractionAnswer.MixedNumber -> {
                        assertTrue(answer.wholePart in 1..3)
                        assertEquals(1L, gcd(answer.fractionalPart.numerator.toLong(), answer.fractionalPart.denominator.toLong()))
                    }
                    else -> Unit
                }
            }
        }
    }
}
