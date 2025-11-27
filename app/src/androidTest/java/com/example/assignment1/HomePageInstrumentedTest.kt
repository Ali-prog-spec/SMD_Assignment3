package com.example.assignment1

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.*
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.bumptech.glide.Glide.init
import com.example.assignment1.R
import com.example.assignment1.UI.View_profile
import com.example.assignment1.UI.home_page
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After

@RunWith(AndroidJUnit4::class)
@LargeTest
class HomePageInstrumentedTest {

    @Before
    fun setup() {
        init() // Espresso Intents Init
        ActivityScenario.launch(home_page::class.java) // launch Activity
    }

    @After
    fun cleanup() {
        release() // release Intents
    }

    @Test
    fun appLaunchesSuccessfully() {
        onView(withId(R.id.main))
            .check(matches(isDisplayed()))
    }

    @Test
    fun storiesRecyclerViewVisible() {
        onView(withId(R.id.storyRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun postsRecyclerViewVisible() {
        onView(withId(R.id.postRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavigationVisible() {
        onView(withId(R.id.customNavBar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickProfileNavigatesToViewProfile() {
        onView(withId(R.id.nav_profile)).perform(click())

        intended(hasComponent(View_profile::class.java.name))
    }
}
