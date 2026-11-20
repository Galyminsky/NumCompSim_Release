package me.proton.jobforandroid.compositionofnumbers

import android.os.SystemClock
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.proton.jobforandroid.compositionofnumbers.presentation.MainActivity
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FractionsCriticalFlowInstrumentedTest {
    @Test fun hubToRealTimeoutResultAndRepeatedEntry() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
            onView(withId(R.id.ncs_v2_hub_fractions_card)).perform(scrollTo(), click())
            startTest()
            waitFor(R.id.fractions_instruction)
            scenario.recreate() // Activity recreation, not process death.
            waitFor(R.id.fractions_instruction)
            waitFor(R.id.fractions_repeat, 40_000)
            destination(scenario, R.id.fractionsResultFragment)
            onView(withId(R.id.fractions_choose_again)).perform(click())
            startTest()
            waitFor(R.id.fractions_instruction)
            destination(scenario, R.id.fractionsGameFragment)
        }
    }

    @Test fun toolbarAndSystemBackReturnToLevelsAndAllowFreshEntry() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
            onView(withId(R.id.ncs_v2_hub_fractions_card)).perform(scrollTo(), click())
            startTest()
            waitFor(R.id.fractions_instruction)
            onView(withId(R.id.fractions_back)).perform(click())
            destination(scenario, R.id.fractionsLevelFragment)
            startTest()
            waitFor(R.id.fractions_instruction)
            Espresso.pressBack()
            destination(scenario, R.id.fractionsLevelFragment)
            waitFor(R.id.fractions_level_test)
        }
    }

    private fun startTest() {
        onView(withId(R.id.fractions_level_test)).perform(scrollTo(), click())
        onView(withId(R.id.fractions_rules_action)).perform(scrollTo(), click())
        waitFor(R.id.fractions_start)
        onView(withId(R.id.fractions_start)).perform(click())
    }

    private fun destination(scenario: ActivityScenario<MainActivity>, expected: Int) {
        scenario.onActivity { activity ->
            val host = activity.supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment
            assertEquals(expected, host.navController.currentDestination?.id)
        }
    }

    private fun waitFor(id: Int, timeout: Long = 10_000) {
        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription() = "wait for fractions view $id within $timeout ms"
            override fun perform(controller: UiController, view: View) {
                val end = SystemClock.uptimeMillis() + timeout
                do {
                    if (view.rootView.findViewById<View>(id) != null) return
                    controller.loopMainThreadForAtLeast(50)
                } while (SystemClock.uptimeMillis() < end)
                throw AssertionError("Fractions view $id did not appear")
            }
        })
        onView(withId(id)).check(matches(isDisplayed()))
    }
}
