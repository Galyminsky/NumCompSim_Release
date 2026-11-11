package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonCanonicalKey
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestionValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import kotlin.random.Random

/** Bounded, range-derived candidate search for complete HARD sessions. */
internal object ComparisonHardSessionGenerator {
    private const val CANDIDATES_PER_VALUE_AND_DIFFICULTY = 6
    private val level = ComparisonLevel.HARD

    fun generate(random: Random): List<ComparisonQuestion> {
        val signs = ComparisonSignPlanner.plan(level, random)
        val distances = ComparisonDistancePlanner.plan(level, signs, random)
        val groups = ComparisonOperationPlanner.planHard(random)
        val complex = (List(42) { true } + List(18) { false }).shuffled(random)
        val arithmetic = buildArithmeticExpressions()
        val mixedArithmetic = buildMixedArithmeticExpressions()
        val tabular = buildTabularExpressions()
        val arithmeticPools = difficulties(arithmetic, random)
        val mixedArithmeticPools = difficulties(mixedArithmetic, random)
        val tabularPools = difficulties(tabular, random)
        val multiplicationPools = difficulties(tabular.filter {
            it.operation == ComparisonOperation.MULTIPLICATION
        }, random)
        val divisionPools = difficulties(tabular.filter {
            it.operation == ComparisonOperation.DIVISION
        }, random)
        val usedKeys = HashSet<String>()
        val usedExpressions = HashSet<ComparisonSide.Expression>()

        return signs.indices.map { index ->
            val group = groups[index]
            val first = when (group) {
                ComparisonOperationGroup.ARITHMETIC -> arithmeticPools.getValue(complex[2 * index])
                ComparisonOperationGroup.TABULAR -> tabularPools.getValue(complex[2 * index])
                ComparisonOperationGroup.MIXED -> mixedArithmeticPools.getValue(complex[2 * index])
            }
            val secondDifficulty = complex[2 * index + 1]
            val secondChoices = when (group) {
                ComparisonOperationGroup.ARITHMETIC -> listOf(arithmeticPools.getValue(secondDifficulty))
                ComparisonOperationGroup.TABULAR -> listOf(tabularPools.getValue(secondDifficulty))
                ComparisonOperationGroup.MIXED -> if (random.nextBoolean()) {
                    listOf(multiplicationPools.getValue(secondDifficulty), divisionPools.getValue(secondDifficulty))
                } else {
                    listOf(divisionPools.getValue(secondDifficulty), multiplicationPools.getValue(secondDifficulty))
                }
            }
            val firstOnLeft = random.nextBoolean()
            var selected: ComparisonQuestion? = null
            for (allowExpressionReuse in listOf(false, true)) {
                if (selected != null) break
                for (second in secondChoices) {
                    selected = findQuestion(first, second, group, signs[index], distances[index],
                        firstOnLeft, allowExpressionReuse, usedKeys, usedExpressions, random)
                    if (selected != null) break
                }
            }
            val question = checkNotNull(selected) { "No valid HARD question for the planned slot" }
            check(usedKeys.add(ComparisonCanonicalKey.of(question)))
            usedExpressions.add(question.left as ComparisonSide.Expression)
            usedExpressions.add(question.right as ComparisonSide.Expression)
            question
        }
    }

    private data class Pool(
        val ordered: List<ComparisonSide.Expression>,
        val byValue: Map<Int, List<ComparisonSide.Expression>>
    )

    private fun difficulties(
        expressions: List<ComparisonSide.Expression>, random: Random
    ): Map<Boolean, Pool> = listOf(false, true).associateWith { complex ->
        val ordered = expressions.filter {
            (ComparisonDifficultyClassifier.classify(level, it) == ComparisonDifficulty.COMPLEX) == complex
        }.shuffled(random)
        Pool(ordered, ordered.groupBy { it.value })
    }

    private fun findQuestion(
        first: Pool,
        second: Pool,
        group: ComparisonOperationGroup,
        sign: ComparisonSign,
        category: ComparisonDistance?,
        firstOnLeft: Boolean,
        allowExpressionReuse: Boolean,
        usedKeys: Set<String>,
        usedExpressions: Set<ComparisonSide.Expression>,
        random: Random
    ): ComparisonQuestion? {
        if (first.ordered.isEmpty() || second.ordered.isEmpty()) return null
        val start = random.nextInt(first.ordered.size)
        val distances = distanceRange(group, category, second.ordered.first().operation)
        val distanceCount = distances.count()
        val distanceStart = random.nextInt(distanceCount)
        for (offset in first.ordered.indices) {
            val a = first.ordered[(start + offset) % first.ordered.size]
            if (!allowExpressionReuse && a in usedExpressions) continue
            for (distanceOffset in 0 until distanceCount) {
                val distance = distances.first + (distanceStart + distanceOffset) % distanceCount
                val target = when {
                    sign == ComparisonSign.EQUAL -> a.value
                    (sign == ComparisonSign.LESS) == firstOnLeft -> a.value + distance
                    else -> a.value - distance
                }
                for (b in second.byValue[target].orEmpty()) {
                    if (!allowExpressionReuse && b in usedExpressions) continue
                    val question = if (firstOnLeft) ComparisonQuestion(level, a, b) else ComparisonQuestion(level, b, a)
                    if (question.correctSign != sign || !ComparisonQuestionValidator.isValid(question) ||
                        ComparisonDistanceClassifier.classify(question) != category ||
                        ComparisonCanonicalKey.of(question) in usedKeys) continue
                    return question
                }
            }
        }
        return null
    }

    private fun distanceRange(
        group: ComparisonOperationGroup, category: ComparisonDistance?, secondOperation: ComparisonOperation
    ): IntRange {
        if (category == null) return 0..0
        return when (group) {
            ComparisonOperationGroup.ARITHMETIC -> when (category) {
                ComparisonDistance.CLOSE -> 1..10
                ComparisonDistance.MEDIUM -> 11..50
                ComparisonDistance.FAR -> 51..200
            }
            ComparisonOperationGroup.TABULAR -> when (category) {
                ComparisonDistance.CLOSE -> 1..5
                ComparisonDistance.MEDIUM -> 6..20
                ComparisonDistance.FAR -> 21..80
            }
            ComparisonOperationGroup.MIXED -> if (secondOperation == ComparisonOperation.MULTIPLICATION) {
                when (category) {
                    ComparisonDistance.CLOSE -> 1..5
                    ComparisonDistance.MEDIUM -> 6..15
                    ComparisonDistance.FAR -> 16..40
                }
            } else {
                when (category) {
                    ComparisonDistance.CLOSE -> 1..2
                    ComparisonDistance.MEDIUM -> 3..6
                    ComparisonDistance.FAR -> 7..18
                }
            }
        }
    }

    private fun buildArithmeticExpressions(): List<ComparisonSide.Expression> {
        val result = mutableListOf<ComparisonSide.Expression>()
        val counts = HashMap<Triple<ComparisonOperation, Boolean, Int>, Int>()
        for (value in 101..1000) {
            for (first in 100..minOf(999, value - 1)) {
                val second = value - first
                if (second >= 100 && first > second) continue
                addCapped(result, counts, ComparisonSide.Expression(first, ComparisonOperation.ADDITION, second))
                if (cappedBoth(counts, ComparisonOperation.ADDITION, value)) break
            }
        }
        for (value in 100..999) {
            for (first in (value + 1)..1000) {
                addCapped(result, counts,
                    ComparisonSide.Expression(first, ComparisonOperation.SUBTRACTION, first - value))
                if (cappedBoth(counts, ComparisonOperation.SUBTRACTION, value)) break
            }
        }
        return result
    }

    private fun buildMixedArithmeticExpressions(): List<ComparisonSide.Expression> {
        val result = mutableListOf<ComparisonSide.Expression>()
        val counts = HashMap<Triple<ComparisonOperation, Boolean, Int>, Int>()
        for (value in 2..100) {
            for (first in 1..(value / 2)) {
                addCapped(result, counts,
                    ComparisonSide.Expression(first, ComparisonOperation.ADDITION, value - first))
                if (cappedBoth(counts, ComparisonOperation.ADDITION, value)) break
            }
            for (first in (value + 1)..1000) {
                addCapped(result, counts,
                    ComparisonSide.Expression(first, ComparisonOperation.SUBTRACTION, first - value))
                if (cappedBoth(counts, ComparisonOperation.SUBTRACTION, value)) break
            }
        }
        return result
    }

    private fun buildTabularExpressions(): List<ComparisonSide.Expression> = buildList {
        for (first in 2..10) {
            for (second in first..10) {
                add(ComparisonSide.Expression(first, ComparisonOperation.MULTIPLICATION, second))
            }
        }
        for (divisor in 2..10) {
            for (quotient in 2..10) {
                add(ComparisonSide.Expression(divisor * quotient, ComparisonOperation.DIVISION, divisor))
            }
        }
    }

    private fun addCapped(
        result: MutableList<ComparisonSide.Expression>,
        counts: MutableMap<Triple<ComparisonOperation, Boolean, Int>, Int>,
        expression: ComparisonSide.Expression
    ) {
        val complex = ComparisonDifficultyClassifier.classify(level, expression) == ComparisonDifficulty.COMPLEX
        val key = Triple(expression.operation, complex, expression.value)
        val count = counts[key] ?: 0
        if (count < CANDIDATES_PER_VALUE_AND_DIFFICULTY) {
            result.add(expression)
            counts[key] = count + 1
        }
    }

    private fun cappedBoth(
        counts: Map<Triple<ComparisonOperation, Boolean, Int>, Int>,
        operation: ComparisonOperation, value: Int
    ): Boolean = (counts[Triple(operation, false, value)] ?: 0) >= CANDIDATES_PER_VALUE_AND_DIFFICULTY &&
        (counts[Triple(operation, true, value)] ?: 0) >= CANDIDATES_PER_VALUE_AND_DIFFICULTY
}
