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
class CriticalFlowsInstrumentedTest {

    @Test
    fun launchMainActivityAndShowWelcomeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.ncs_v2_choose_lesson_button)).check(matches(isDisplayed()))
            onView(withId(R.id.ncs_v2_documents_button)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun navigateFromWelcomeToLessonSelection() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_addition_button)).check(matches(isDisplayed()))
            onView(withId(R.id.ncs_v2_addition_item)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun fullNavigationFlowFromWelcomeToGame() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_addition_button)).check(matches(isDisplayed()))

            onView(withId(R.id.ncs_v2_start_addition_button)).perform(click())
            onView(withId(R.id.ncs_v2_ready_button)).check(matches(isDisplayed()))

            onView(withId(R.id.ncs_v2_ready_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_game_button)).check(matches(isDisplayed()))

            onView(withId(R.id.ncs_v2_start_game_button)).perform(click())
            onView(withId(R.id.ncs_v2_game_equation)).check(matches(isDisplayed()))
            onView(withId(R.id.ncs_v2_game_answer_1)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun recreateActivityOnWelcomeScreenAndGameScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.ncs_v2_choose_lesson_button)).check(matches(isDisplayed()))
            scenario.recreate()
            onView(withId(R.id.ncs_v2_choose_lesson_button)).check(matches(isDisplayed()))

            onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_addition_button)).perform(click())
            onView(withId(R.id.ncs_v2_ready_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_game_button)).check(matches(isDisplayed()))

            scenario.recreate()
            onView(withId(R.id.ncs_v2_start_game_button)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun returnFromGameToLevelSelection() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_addition_button)).perform(click())
            onView(withId(R.id.ncs_v2_ready_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_game_button)).perform(click())

            onView(withId(R.id.ncs_v2_game_back_button)).check(matches(isDisplayed()))
            onView(withId(R.id.ncs_v2_game_back_button)).perform(click())

            onView(withId(R.id.ncs_v2_start_game_button)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun multipleActivityLaunchAndCloseCycle() {
        repeat(3) {
            ActivityScenario.launch(MainActivity::class.java).use {
                onView(withId(R.id.ncs_v2_choose_lesson_button)).check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun offlineAppLaunchAndNavigation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.ncs_v2_choose_lesson_button)).perform(click())
            onView(withId(R.id.ncs_v2_start_addition_button)).check(matches(isDisplayed()))
        }
    }
}
