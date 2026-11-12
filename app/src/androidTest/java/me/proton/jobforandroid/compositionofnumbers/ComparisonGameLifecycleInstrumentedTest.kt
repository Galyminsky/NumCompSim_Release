package me.proton.jobforandroid.compositionofnumbers

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.proton.jobforandroid.compositionofnumbers.presentation.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComparisonGameLifecycleInstrumentedTest {

    @Test
    fun leavingBeforeNextTimerTickDoesNotTouchDestroyedView() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openComparisonGame(R.id.ncs_v2_comparison_hard_level)
            onView(withId(R.id.ncs_v2_comparison_game_back_button)).perform(click())
            onView(withId(R.id.ncs_v2_comparison_level_start_button)).check(matches(isDisplayed()))
            Thread.sleep(1_500L)
            onView(withId(R.id.ncs_v2_comparison_level_start_button)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun recreatingGameViewDoesNotDeliverOldTimerTickToNewView() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openComparisonGame(R.id.ncs_v2_comparison_hard_level)
            scenario.recreate()
            onView(withId(R.id.ncs_v2_comparison_game_time_value)).check(matches(isDisplayed()))
            Thread.sleep(1_500L)
            onView(withId(R.id.ncs_v2_comparison_game_time_value)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun timerExpiryNavigatesToResultWithoutStaleTick() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openComparisonGame(R.id.ncs_v2_comparison_test_level)
            Thread.sleep(17_000L)
            onView(withId(R.id.ncs_v2_comparison_result_retry_button)).check(matches(isDisplayed()))
            Thread.sleep(1_500L)
            onView(withId(R.id.ncs_v2_comparison_result_retry_button)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun leavingDuringAnswerFeedbackDoesNotTouchDestroyedView() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openComparisonGame(R.id.ncs_v2_comparison_hard_level)
            scenario.onActivity { activity ->
                activity.findViewById<android.view.View>(R.id.ncs_v2_comparison_less_button)
                    .performClick()
                activity.findViewById<android.view.View>(R.id.ncs_v2_comparison_game_back_button)
                    .performClick()
            }
            onView(withId(R.id.ncs_v2_comparison_level_start_button)).check(matches(isDisplayed()))
            Thread.sleep(1_000L)
            onView(withId(R.id.ncs_v2_comparison_level_start_button)).check(matches(isDisplayed()))
        }
    }

    private fun openComparisonGame(levelId: Int) {
        onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
        onView(withId(R.id.ncs_v2_hub_comparison_card)).perform(click())
        onView(withId(R.id.ncs_v2_comparison_rules_ready_button)).perform(click())
        onView(withId(levelId)).perform(click())
        onView(withId(R.id.ncs_v2_comparison_level_start_button)).perform(click())
        onView(withId(R.id.ncs_v2_comparison_game_time_value)).check(matches(isDisplayed()))
    }
}
