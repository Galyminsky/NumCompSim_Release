package me.proton.jobforandroid.compositionofnumbers

import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.proton.jobforandroid.compositionofnumbers.presentation.MainActivity
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalFlowsInstrumentedTest {

    @Test
    fun launchMainActivityAutonomously() {
        val tag = "AutonomousLaunch"
        val startedAt = SystemClock.elapsedRealtime()
        var phase = "TEST_BEGIN"
        var scenario: ActivityScenario<MainActivity>? = null
        var primaryFailure: Throwable? = null
        var uiConfirmed = false
        var closeConfirmed = false
        fun record(event: String) {
            Log.i(tag, "launchMainActivityAutonomously +${SystemClock.elapsedRealtime() - startedAt}ms $event")
        }

        record(phase)
        try {
            phase = "ACTIVITY_LAUNCH"
            record("LAUNCH_CALL MainActivity")
            val launched = ActivityScenario.launch(MainActivity::class.java)
            scenario = launched
            record("LAUNCH_RETURN state=${launched.state}")

            phase = "LIFECYCLE_CHECK"
            assertEquals("MainActivity must be RESUMED after launch", Lifecycle.State.RESUMED, launched.state)
            record("LIFECYCLE_CONFIRMED state=${launched.state}")

            phase = "WELCOME_UI_CHECK"
            assertDisplayed(R.id.ncs_v2_choose_lesson_button)
            assertDisplayed(R.id.ncs_v2_documents_button)
            launched.onActivity { activity ->
                val host = activity.supportFragmentManager
                    .findFragmentById(R.id.main_container) as NavHostFragment
                assertEquals(R.id.ncsV2WelcomeFragment, host.navController.currentDestination?.id)
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                assertTrue("MainActivity must own the focused window", activity.hasWindowFocus())
                assertTrue("MainActivity must remain alive", !activity.isFinishing && !activity.isDestroyed)
            }
            uiConfirmed = true
            record("SCREEN_CONFIRMED destination=ncsV2WelcomeFragment CTA=ncs_v2_choose_lesson_button windowFocused=true")
        } catch (failure: Throwable) {
            primaryFailure = failure
            Log.e(tag, "launchMainActivityAutonomously FAILED phase=$phase elapsedMs=${SystemClock.elapsedRealtime() - startedAt}", failure)
            throw failure
        } finally {
            try {
                val launched = scenario
                if (launched != null) {
                    record("ACTIVITY_CLOSE_CALL state=${launched.state}")
                    launched.close()
                    assertEquals("Activity must be destroyed after close", Lifecycle.State.DESTROYED, launched.state)
                    closeConfirmed = true
                    record("ACTIVITY_CLOSE_RETURN state=${launched.state}")
                } else {
                    record("ACTIVITY_CLOSE_UNAVAILABLE launch did not return a scenario")
                }
            } catch (closeFailure: Throwable) {
                Log.e(tag, "launchMainActivityAutonomously FAILED during Activity close", closeFailure)
                val originalFailure = primaryFailure
                if (originalFailure != null) originalFailure.addSuppressed(closeFailure)
                else throw closeFailure
            } finally {
                record("TEST_END uiConfirmed=$uiConfirmed closeConfirmed=$closeConfirmed")
            }
        }
    }

    @Test
    fun launchMainActivityAndShowWelcomeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            assertDisplayed(R.id.ncs_v2_choose_lesson_button)
            assertDisplayed(R.id.ncs_v2_documents_button)
        }
    }

    @Test
    fun navigateFromWelcomeToLessonSelection() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openArithmeticLessonSelection()
        }
    }

    @Test
    fun fullNavigationFlowFromWelcomeToGameAndResult() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openArithmeticGame(R.id.ncs_v2_test_level_item)
            // No answers: the real TEST timer leads deterministically to failure.
            assertDisplayed(R.id.ncs_v2_result_failure_retry_button, timeoutMs = 20_000L)
            assertDisplayed(R.id.ncs_v2_result_failure_choose_level_button)
        }
    }

    @Test
    fun recreateActivityOnWelcomeScreenAndGameScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertDisplayed(R.id.ncs_v2_choose_lesson_button)
            scenario.recreate()
            assertDisplayed(R.id.ncs_v2_choose_lesson_button)
            assertDisplayed(R.id.ncs_v2_documents_button)

            // HARD gives the recreation check time to finish without changing any timer.
            openArithmeticGame(R.id.ncs_v2_hard_level_item)
            assertGameDestination(scenario)
            scenario.recreate()
            scrollToAndAssertDisplayed(R.id.ncs_v2_game_equation)
            onView(withId(R.id.ncs_v2_game_answer_1)).perform(scrollTo())
            assertDisplayed(R.id.ncs_v2_game_answer_1)
            assertGameDestination(scenario)
        }
    }

    @Test
    fun returnFromGameToLevelSelectionWithToolbarAndSystemBack() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openArithmeticGame(R.id.ncs_v2_hard_level_item)
            onView(withId(R.id.ncs_v2_game_back_button)).perform(scrollTo(), click())
            assertDisplayed(R.id.ncs_v2_start_game_button)

            onView(withId(R.id.ncs_v2_start_game_button)).perform(click())
            scrollToAndAssertDisplayed(R.id.ncs_v2_game_equation)
            pressBack()
            assertDisplayed(R.id.ncs_v2_start_game_button)
        }
    }

    @Test
    fun multipleActivityLaunchAndCloseCycle() {
        repeat(3) {
            ActivityScenario.launch(MainActivity::class.java).use {
                assertDisplayed(R.id.ncs_v2_choose_lesson_button)
            }
        }
    }

    @Test
    fun navigateFromWelcomeThroughArithmeticRulesToLevelSelection() {
        // This checks navigation only; it makes no claim about network availability.
        ActivityScenario.launch(MainActivity::class.java).use {
            openArithmeticLevelSelection()
        }
    }

    private fun openArithmeticLessonSelection() {
        assertDisplayed(R.id.ncs_v2_choose_lesson_button)
        onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
        assertDisplayed(R.id.ncs_v2_main_hub_heading)
        onView(withId(R.id.ncs_v2_hub_arithmetic_card)).perform(scrollTo(), click())
        assertDisplayed(R.id.ncs_v2_start_addition_button)
        onView(withId(R.id.ncs_v2_addition_item)).perform(scrollTo())
        assertDisplayed(R.id.ncs_v2_addition_item)
    }

    private fun openArithmeticLevelSelection() {
        openArithmeticLessonSelection()
        onView(withId(R.id.ncs_v2_start_addition_button)).perform(click())
        scrollToAndAssertDisplayed(R.id.ncs_v2_ready_button)
        onView(withId(R.id.ncs_v2_ready_button)).perform(click())
        assertDisplayed(R.id.ncs_v2_start_game_button)
    }

    private fun openArithmeticGame(levelViewId: Int) {
        openArithmeticLevelSelection()
        onView(withId(levelViewId)).perform(scrollTo(), click())
        onView(withId(R.id.ncs_v2_start_game_button)).perform(click())
        scrollToAndAssertDisplayed(R.id.ncs_v2_game_equation)
    }

    private fun assertGameDestination(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            val host = activity.supportFragmentManager
                .findFragmentById(R.id.main_container) as NavHostFragment
            assertEquals(R.id.gameFragment, host.navController.currentDestination?.id)
        }
    }

    private fun scrollToAndAssertDisplayed(viewId: Int) {
        waitForView(viewId, timeoutMs = 10_000L, requireDisplayed = false)
        onView(withId(viewId)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    private fun assertDisplayed(viewId: Int, timeoutMs: Long = 10_000L) {
        waitForView(viewId, timeoutMs, requireDisplayed = true)
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    private fun waitForView(viewId: Int, timeoutMs: Long, requireDisplayed: Boolean) {
        // Espresso does not automatically wait for the app's delayed navigation callbacks.
        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String =
                "wait up to $timeoutMs ms for view ID $viewId (displayed=$requireDisplayed)"

            override fun perform(uiController: UiController, view: View) {
                val deadline = SystemClock.uptimeMillis() + timeoutMs
                do {
                    val target = view.rootView.findViewById<View>(viewId)
                    if (target != null && (!requireDisplayed || isDisplayed().matches(target))) return
                    uiController.loopMainThreadForAtLeast(50L)
                } while (SystemClock.uptimeMillis() < deadline)
                throw AssertionError("View ID $viewId was not ready within $timeoutMs ms")
            }
        })
    }
}
