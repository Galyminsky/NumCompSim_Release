package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonOperationGroup
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonOperationPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComparisonOperationPlannerTest {
    @Test fun normalHasExactlyNineOfEachOperation() {
        val plan = ComparisonOperationPlanner.planNormal(Random(20261111))
        assertEquals(36, plan.size)
        assertEquals(ComparisonOperation.entries.toSet(), plan.toSet())
        for (operation in ComparisonOperation.entries) {
            assertEquals(9, plan.count { it == operation })
        }
    }

    @Test fun hardHasExactlyTenOfEachGroup() {
        val plan = ComparisonOperationPlanner.planHard(Random(20261111))
        assertEquals(30, plan.size)
        assertEquals(ComparisonOperationGroup.entries.toSet(), plan.toSet())
        for (group in ComparisonOperationGroup.entries) {
            assertEquals(10, plan.count { it == group })
        }
    }

    @Test fun sameSeedReproducesBothPlans() {
        assertEquals(
            ComparisonOperationPlanner.planNormal(Random(42)),
            ComparisonOperationPlanner.planNormal(Random(42))
        )
        assertEquals(
            ComparisonOperationPlanner.planHard(Random(42)),
            ComparisonOperationPlanner.planHard(Random(42))
        )
    }

    @Test fun differentSeedsProduceDiverseSequencesWithUnchangedQuotas() {
        val normalPlans = (1..5).map { ComparisonOperationPlanner.planNormal(Random(it)) }
        val hardPlans = (1..5).map { ComparisonOperationPlanner.planHard(Random(it)) }
        assertTrue(normalPlans.toSet().size > 1)
        assertTrue(hardPlans.toSet().size > 1)
        for (plan in normalPlans) {
            assertEquals(36, plan.size)
            for (operation in ComparisonOperation.entries) {
                assertEquals(9, plan.count { it == operation })
            }
        }
        for (plan in hardPlans) {
            assertEquals(30, plan.size)
            for (group in ComparisonOperationGroup.entries) {
                assertEquals(10, plan.count { it == group })
            }
        }
    }

    @Test fun plansAreNotFixedCyclesOrUnshuffledBlocks() {
        val normal = ComparisonOperationPlanner.planNormal(Random(20261111))
        val hard = ComparisonOperationPlanner.planHard(Random(20261111))
        assertNotEquals(List(36) { ComparisonOperation.entries[it % 4] }, normal)
        assertNotEquals(ComparisonOperation.entries.flatMap { op -> List(9) { op } }, normal)
        assertNotEquals(List(30) { ComparisonOperationGroup.entries[it % 3] }, hard)
        assertNotEquals(ComparisonOperationGroup.entries.flatMap { group -> List(10) { group } }, hard)
    }
}
