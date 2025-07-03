package com.example.eglatracker

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class TrackingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun trackingTab_isDisplayed() {
        // Verify the default selected tab text "Tracking" appears
        composeRule.onNodeWithText("Tracking").assertIsDisplayed()
    }
}