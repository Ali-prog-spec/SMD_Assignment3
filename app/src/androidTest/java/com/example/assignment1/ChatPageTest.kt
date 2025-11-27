package com.example.assignment1

import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.assignment1.R
import com.example.assignment1.UI.chat_paage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatPageTest {

    private fun launchChatActivity() {
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            chat_paage::class.java
        ).apply {
            putExtra("receiverId", "TestUser123")
            putExtra("receiverName", "Test User")
        }
        ActivityScenario.launch<chat_paage>(intent)
    }

    @Test
    fun test_UIElements_Appear() {
        launchChatActivity()

        onView(withId(R.id.searchEditText)).check(matches(isDisplayed()))
        onView(withId(R.id.avatarText)).check(matches(isDisplayed()))
        onView(withId(R.id.chatRecyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.v1)).check(matches(isDisplayed())) // ✅ send button
        onView(withId(R.id.i1)).check(matches(isDisplayed())) // ✅ gallery button
    }

    @Test
    fun test_SendTextMessage() {
        launchChatActivity()

        // Type a message
        onView(withId(R.id.s1)).perform(typeText("Hello Espresso!"), closeSoftKeyboard())

        // Click Send button
        onView(withId(R.id.v1)).perform(click())

        // Assert message list visible
        onView(withId(R.id.chatRecyclerView)).check(matches(isDisplayed()))
    }

    @Test
    fun test_BackButton() {
        launchChatActivity()

        // Click back icon
        onView(withId(R.id.back_icon)).perform(click())
    }
}
