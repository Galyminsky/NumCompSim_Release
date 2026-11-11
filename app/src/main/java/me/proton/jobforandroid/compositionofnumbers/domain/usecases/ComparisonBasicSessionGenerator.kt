package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonCanonicalKey
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestionValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import kotlin.random.Random

object ComparisonBasicSessionGenerator {
    /** Builds a complete session before it can be presented to the player. */
    fun generate(level: ComparisonLevel, random: Random): List<ComparisonQuestion> {
        if (level == ComparisonLevel.HARD) return ComparisonHardSessionGenerator.generate(random)
        if (level == ComparisonLevel.NORMAL) return generateNormal(random)
        val signs = ComparisonSignPlanner.plan(level, random)
        val distances = ComparisonDistancePlanner.plan(level, signs, random)
        val maxNumber = if (level == ComparisonLevel.TEST) 10 else 100

        val equalPairs = mutableListOf<Pair<Int, Int>>()
        val unequalPairs = linkedMapOf<ComparisonDistance?, MutableList<Pair<Int, Int>>>()
        for (low in 0..maxNumber) {
            for (high in low..maxNumber) {
                if (low == high) {
                    equalPairs.add(low to high)
                } else {
                    val sample = ComparisonQuestion(level, ComparisonSide.Number(low), ComparisonSide.Number(high))
                    val category = ComparisonDistanceClassifier.classify(sample)
                    if (level == ComparisonLevel.TEST || category != null) {
                        unequalPairs.getOrPut(category) { mutableListOf() }.add(low to high)
                    }
                }
            }
        }
        equalPairs.shuffle(random)
        unequalPairs.values.forEach { it.shuffle(random) }

        val usedKeys = HashSet<String>()
        return signs.mapIndexed { index, sign ->
            val category = distances[index]
            val pool = if (sign == ComparisonSign.EQUAL) equalPairs else
                requireNotNull(unequalPairs[category])
            check(pool.isNotEmpty()) { "No unused candidate for the planned sign and distance" }
            val pairIndex = if (level == ComparisonLevel.EASY) {
                val preferTwoDigit = random.nextInt(5) != 0
                val preferredIndex = pool.indexOfFirst { (low, high) ->
                    val bothTwoDigit = low in 10..99 && high in 10..99
                    bothTwoDigit == preferTwoDigit
                }
                if (preferredIndex >= 0) preferredIndex else 0
            } else {
                0
            }
            val (low, high) = pool.removeAt(pairIndex)
            val question = when (sign) {
                ComparisonSign.LESS, ComparisonSign.EQUAL ->
                    ComparisonQuestion(level, ComparisonSide.Number(low), ComparisonSide.Number(high))
                ComparisonSign.GREATER ->
                    ComparisonQuestion(level, ComparisonSide.Number(high), ComparisonSide.Number(low))
            }
            check(question.correctSign == sign)
            check(ComparisonQuestionValidator.isValid(question))
            check(level == ComparisonLevel.TEST || ComparisonDistanceClassifier.classify(question) == category)
            check(usedKeys.add(ComparisonCanonicalKey.of(question)))
            question
        }
    }

    private fun generateNormal(random: Random): List<ComparisonQuestion> {
        val level = ComparisonLevel.NORMAL
        val signs = ComparisonSignPlanner.plan(level, random)
        val distances = ComparisonDistancePlanner.plan(level, signs, random)
        val operations = ComparisonOperationPlanner.planNormal(random)
        val complexSlots = (List(22) { true } + List(14) { false }).shuffled(random)
        val expressions = buildNormalExpressions(random)
        val usedKeys = HashSet<String>()
        val usedExpressions = HashSet<ComparisonSide.Expression>()

        return signs.indices.map { index ->
            val operation = operations[index]
            val complex = complexSlots[index]
            val sign = signs[index]
            val category = distances[index]
            val pool = requireNotNull(expressions[operation to complex])
            val start = random.nextInt(pool.size)
            val expressionLeft = random.nextBoolean()
            val distanceRange = normalDistanceRange(operation, category)
            val distanceStart = if (distanceRange == null) 0 else random.nextInt(distanceRange.count())
            var chosen: ComparisonQuestion? = null

            // Prefer a new expression; reusing one with a different comparison is a soft fallback.
            for (allowExpressionReuse in listOf(false, true)) {
                if (chosen != null) break
                for (offset in pool.indices) {
                    val expression = pool[(start + offset) % pool.size]
                    if (!allowExpressionReuse && expression in usedExpressions) continue
                    val distanceCount = distanceRange?.count() ?: 1
                    for (distanceOffset in 0 until distanceCount) {
                        val distance = if (distanceRange == null) 0 else
                            distanceRange.first + (distanceStart + distanceOffset) % distanceCount
                        val number = when {
                            sign == ComparisonSign.EQUAL -> expression.value
                            (sign == ComparisonSign.LESS) == expressionLeft -> expression.value + distance
                            else -> expression.value - distance
                        }
                        val question = if (expressionLeft) {
                            ComparisonQuestion(level, expression, ComparisonSide.Number(number))
                        } else {
                            ComparisonQuestion(level, ComparisonSide.Number(number), expression)
                        }
                        if (!ComparisonQuestionValidator.isValid(question) || question.correctSign != sign ||
                            ComparisonDistanceClassifier.classify(question) != category) continue
                        val key = ComparisonCanonicalKey.of(question)
                        if (key in usedKeys) continue
                        usedKeys.add(key)
                        usedExpressions.add(expression)
                        chosen = question
                        break
                    }
                    if (chosen != null) break
                }
            }
            checkNotNull(chosen) { "No valid NORMAL question for the planned slot" }
        }
    }

    private fun buildNormalExpressions(random: Random): Map<Pair<ComparisonOperation, Boolean>, List<ComparisonSide.Expression>> {
        val all = mutableListOf<ComparisonSide.Expression>()
        for (first in 1..99) {
            for (second in first..(100 - first)) {
                if (first + second >= 20) {
                    all.add(ComparisonSide.Expression(first, ComparisonOperation.ADDITION, second))
                }
            }
        }
        for (first in 3..100) {
            for (second in 1..(first - 2)) {
                all.add(ComparisonSide.Expression(first, ComparisonOperation.SUBTRACTION, second))
            }
        }
        for (first in 2..9) {
            for (second in first..9) {
                all.add(ComparisonSide.Expression(first, ComparisonOperation.MULTIPLICATION, second))
            }
        }
        for (divisor in 2..9) {
            for (quotient in 2..9) {
                all.add(ComparisonSide.Expression(divisor * quotient, ComparisonOperation.DIVISION, divisor))
            }
        }
        val groups = linkedMapOf<Pair<ComparisonOperation, Boolean>, MutableList<ComparisonSide.Expression>>()
        for (expression in all) {
            val complex = ComparisonDifficultyClassifier.classify(ComparisonLevel.NORMAL, expression) ==
                ComparisonDifficulty.COMPLEX
            groups.getOrPut(expression.operation to complex) { mutableListOf() }.add(expression)
        }
        groups.values.forEach { it.shuffle(random) }
        return groups
    }

    private fun normalDistanceRange(
        operation: ComparisonOperation,
        category: ComparisonDistance?
    ): IntRange? {
        if (category == null) return null
        return when (operation) {
            ComparisonOperation.ADDITION, ComparisonOperation.SUBTRACTION -> when (category) {
                ComparisonDistance.CLOSE -> 1..5
                ComparisonDistance.MEDIUM -> 6..15
                ComparisonDistance.FAR -> 16..35
            }
            ComparisonOperation.MULTIPLICATION -> when (category) {
                ComparisonDistance.CLOSE -> 1..4
                ComparisonDistance.MEDIUM -> 5..12
                ComparisonDistance.FAR -> 13..25
            }
            ComparisonOperation.DIVISION -> when (category) {
                ComparisonDistance.CLOSE -> 1..2
                ComparisonDistance.MEDIUM -> 3..5
                ComparisonDistance.FAR -> 6..9
            }
        }
    }
}
