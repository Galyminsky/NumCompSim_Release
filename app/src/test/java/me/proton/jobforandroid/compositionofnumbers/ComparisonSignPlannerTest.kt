package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonSignPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class ComparisonSignPlannerTest {
    private val counts = mapOf(
        ComparisonLevel.TEST to 8,
        ComparisonLevel.EASY to 15,
        ComparisonLevel.NORMAL to 12,
        ComparisonLevel.HARD to 10
    )

    @Test fun everyLevelHasExactSizeAndSignQuotas() {
        for ((level, perSign) in counts) {
            val plan = ComparisonSignPlanner.plan(level, Random(20261111))
            assertEquals(perSign * 3, plan.size)
            for (sign in ComparisonSign.entries) {
                assertEquals(perSign, plan.count { it == sign })
            }
        }
    }

    @Test fun sameSeedReproducesEachLevelPlan() {
        for (level in ComparisonLevel.entries) {
            assertEquals(
                ComparisonSignPlanner.plan(level, Random(42)),
                ComparisonSignPlanner.plan(level, Random(42))
            )
        }
    }

    @Test fun differentSeedsKeepQuotasAndCanChangeOrder() {
        for ((level, perSign) in counts) {
            val plans = (1..5).map { seed -> ComparisonSignPlanner.plan(level, Random(seed)) }
            assertNotEquals(plans[0], plans[1])
            for (plan in plans) {
                assertEquals(perSign * 3, plan.size)
                for (sign in ComparisonSign.entries) {
                    assertEquals(perSign, plan.count { it == sign })
                }
            }
        }
    }

    @Test fun outputIsNotAFixedCycleOrUnshuffledBlocks() {
        for ((level, perSign) in counts) {
            val cycle = List(perSign * 3) { ComparisonSign.entries[it % 3] }
            val blocks = ComparisonSign.entries.flatMap { sign -> List(perSign) { sign } }
            val plan = ComparisonSignPlanner.plan(level, Random(20261111))
            assertNotEquals(cycle, plan)
            assertNotEquals(blocks, plan)
        }
    }
}
