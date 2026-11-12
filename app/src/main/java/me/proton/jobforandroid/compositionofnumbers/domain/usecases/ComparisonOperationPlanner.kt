package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import kotlin.random.Random

enum class ComparisonOperationGroup { ARITHMETIC, TABULAR, MIXED }

object ComparisonOperationPlanner {
    fun planNormal(random: Random): List<ComparisonOperation> =
        mutableListOf<ComparisonOperation>().apply {
            for (operation in ComparisonOperation.entries) {
                repeat(9) { add(operation) }
            }
            shuffle(random)
        }

    fun planHard(random: Random): List<ComparisonOperationGroup> =
        mutableListOf<ComparisonOperationGroup>().apply {
            for (group in ComparisonOperationGroup.entries) {
                repeat(10) { add(group) }
            }
            shuffle(random)
        }

    /** Reorders completed questions without changing any question or session quota. */
    internal fun shuffleReadyQuestions(
        questions: List<ComparisonQuestion>, random: Random
    ): List<ComparisonQuestion> {
        require(questions.isNotEmpty())
        val level = questions.first().level
        require(level == ComparisonLevel.NORMAL || level == ComparisonLevel.HARD)
        require(questions.all { it.level == level })
        val groups = questions.groupBy { question ->
            if (level == ComparisonLevel.NORMAL) {
                listOf(question.left, question.right)
                    .filterIsInstance<ComparisonSide.Expression>().single().operation.name
            } else {
                val left = question.left as ComparisonSide.Expression
                val right = question.right as ComparisonSide.Expression
                val leftArithmetic = left.operation == ComparisonOperation.ADDITION ||
                    left.operation == ComparisonOperation.SUBTRACTION
                val rightArithmetic = right.operation == ComparisonOperation.ADDITION ||
                    right.operation == ComparisonOperation.SUBTRACTION
                when {
                    leftArithmetic && rightArithmetic -> ComparisonOperationGroup.ARITHMETIC.name
                    !leftArithmetic && !rightArithmetic -> ComparisonOperationGroup.TABULAR.name
                    else -> ComparisonOperationGroup.MIXED.name
                }
            }
        }.values.map { it.shuffled(random) }
        check(groups.size == if (level == ComparisonLevel.NORMAL) 4 else 3)
        val rounds = groups.first().size
        check(groups.all { it.size == rounds })

        val ordered = ArrayList<ComparisonQuestion>(questions.size)
        var previousGroup = -1
        repeat(rounds) { round ->
            val groupOrder = groups.indices.shuffled(random).toMutableList()
            if (groupOrder.first() == previousGroup) {
                val replacement = groupOrder.indexOfFirst { it != previousGroup }
                val first = groupOrder[0]
                groupOrder[0] = groupOrder[replacement]
                groupOrder[replacement] = first
            }
            for (groupIndex in groupOrder) ordered.add(groups[groupIndex][round])
            previousGroup = groupOrder.last()
        }
        return ordered
    }
}
