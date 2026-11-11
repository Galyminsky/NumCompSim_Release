package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
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
}
