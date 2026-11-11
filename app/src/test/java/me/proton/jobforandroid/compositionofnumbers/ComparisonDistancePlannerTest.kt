package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDistance
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDistancePlanner
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonSignPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComparisonDistancePlannerTest {
    private fun signs(level: ComparisonLevel) = ComparisonSignPlanner.plan(level, Random(42))
    private fun plan(level: ComparisonLevel, seed: Int) =
        ComparisonDistancePlanner.plan(level, signs(level), Random(seed))

    @Test fun allLevelsHaveCorrectSizeAndCategoryQuotas() {
        for (level in ComparisonLevel.entries) {
            val signs = signs(level)
            val categories = plan(level, 20261111)
            assertEquals(signs.size, categories.size)
            for (index in signs.indices) {
                if (signs[index] == ComparisonSign.EQUAL) assertNull(categories[index])
            }
            when (level) {
                ComparisonLevel.TEST -> assertTrue(categories.all { it == null })
                ComparisonLevel.EASY -> {
                    assertEquals(45, categories.size)
                    assertEquals(9, categories.count { it == ComparisonDistance.CLOSE })
                    assertEquals(setOf(10, 11), setOf(
                        categories.count { it == ComparisonDistance.MEDIUM },
                        categories.count { it == ComparisonDistance.FAR }
                    ))
                }
                ComparisonLevel.NORMAL -> {
                    assertEquals(36, categories.size)
                    assertEquals(7, categories.count { it == ComparisonDistance.CLOSE })
                    assertEquals(setOf(8, 9), setOf(
                        categories.count { it == ComparisonDistance.MEDIUM },
                        categories.count { it == ComparisonDistance.FAR }
                    ))
                }
                ComparisonLevel.HARD -> {
                    assertEquals(30, categories.size)
                    assertEquals(6, categories.count { it == ComparisonDistance.CLOSE })
                    assertEquals(7, categories.count { it == ComparisonDistance.MEDIUM })
                    assertEquals(7, categories.count { it == ComparisonDistance.FAR })
                }
            }
        }
    }

    @Test fun sameSeedReproducesAssignments() {
        for (level in ComparisonLevel.entries) {
            assertEquals(plan(level, 77), plan(level, 77))
        }
    }

    @Test fun differentSeedsChangeOrderWithoutChangingFixedQuotas() {
        for (level in listOf(ComparisonLevel.EASY, ComparisonLevel.NORMAL, ComparisonLevel.HARD)) {
            val plans = (1..10).map { plan(level, it) }
            assertTrue(plans.toSet().size > 1)
            val closeCount = if (level == ComparisonLevel.EASY) 9 else if (level == ComparisonLevel.NORMAL) 7 else 6
            for (categories in plans) {
                assertEquals(closeCount, categories.count { it == ComparisonDistance.CLOSE })
                assertEquals(signs(level).count { it != ComparisonSign.EQUAL }, categories.count { it != null })
            }
            assertNotEquals(plans.first(), plans.last())
        }
    }

    @Test fun bothRoundingVariantsOccurForEasyAndNormal() {
        for ((level, expected) in listOf(
            ComparisonLevel.EASY to setOf(10, 11),
            ComparisonLevel.NORMAL to setOf(8, 9)
        )) {
            val mediumCounts = (0..100).map { seed ->
                plan(level, seed).count { it == ComparisonDistance.MEDIUM }
            }.toSet()
            assertEquals(expected, mediumCounts)
        }
    }

    @Test fun lessAndGreaterOrientationsDoNotAffectAssignments() {
        for (level in ComparisonLevel.entries) {
            val original = signs(level)
            val reversed = original.map { sign -> when (sign) {
                ComparisonSign.LESS -> ComparisonSign.GREATER
                ComparisonSign.GREATER -> ComparisonSign.LESS
                ComparisonSign.EQUAL -> ComparisonSign.EQUAL
            } }
            assertEquals(
                ComparisonDistancePlanner.plan(level, original, Random(123)),
                ComparisonDistancePlanner.plan(level, reversed, Random(123))
            )
        }
    }

    @Test fun assignmentsAreNotFixedBlocksOrCycles() {
        for (level in listOf(ComparisonLevel.EASY, ComparisonLevel.NORMAL, ComparisonLevel.HARD)) {
            val assignments = plan(level, 20261111).filterNotNull()
            val blocks = ComparisonDistance.entries.flatMap { category ->
                List(assignments.count { it == category }) { category }
            }
            val cycle = List(assignments.size) { ComparisonDistance.entries[it % 3] }
            assertNotEquals(blocks, assignments)
            assertNotEquals(cycle, assignments)
        }
    }

    @Test fun rejectsIncompleteSignPlans() {
        assertThrows(IllegalArgumentException::class.java) {
            ComparisonDistancePlanner.plan(ComparisonLevel.EASY,
                List(45) { ComparisonSign.LESS }, Random(1))
        }
    }
}
