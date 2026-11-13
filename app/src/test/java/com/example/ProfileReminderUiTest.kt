package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.StudyBloomTheme
import com.example.util.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w320dp-h640dp")
class ProfileReminderUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun session_reminders_section_displays_without_overlap_or_overflow_on_narrow_screen() {
        val profile = UserProfileEntity(
            nickname = "Learner",
            reminderEnabled = true,
            reminderBeforeMinutes = 10
        )

        composeTestRule.setContent {
            StudyBloomTheme(themeMode = "LIGHT") {
                ProfileScreen(
                    userProfile = profile,
                    onSaveProfile = {},
                    onTestNotification = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Scroll LazyColumn to the Reminders & Notifications card
        composeTestRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasTestTag("profile_test_notification_button"))
        composeTestRule.waitForIdle()

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        val titleNode = composeTestRule.onNodeWithText("Session Reminders")
        titleNode.assertIsDisplayed()

        val labelNode = composeTestRule.onNodeWithText("Reminder Interval")
        labelNode.assertIsDisplayed()
        val labelBounds = labelNode.getUnclippedBoundsInRoot()

        val intervalTags = listOf(
            "profile_reminder_interval_5m",
            "profile_reminder_interval_10m",
            "profile_reminder_interval_15m",
            "profile_reminder_interval_30m"
        )

        var previousRight = 0.dp
        intervalTags.forEach { tag ->
            val chip = composeTestRule.onNodeWithTag(tag)
            chip.assertIsDisplayed()
            val chipBounds = chip.getUnclippedBoundsInRoot()

            // Must sit strictly below the Reminder Interval label (no vertical/horizontal overlap)
            assertTrue("Chip $tag should sit below Reminder Interval label", chipBounds.top >= labelBounds.bottom)
            // Must remain completely inside screen width
            assertTrue("Chip $tag left edge should be inside screen", chipBounds.left >= rootBounds.left)
            assertTrue("Chip $tag right edge should not overflow screen", chipBounds.right <= rootBounds.right)
            // Must maintain horizontal ordering without overlapping adjacent chips
            assertTrue("Chip $tag should not overlap previous chip", chipBounds.left >= previousRight)
            // Must satisfy >= 48dp accessible touch height
            assertTrue("Chip $tag height should be at least 48dp", (chipBounds.bottom - chipBounds.top) >= 47.5.dp)

            previousRight = chipBounds.right
        }

        val testBtn = composeTestRule.onNodeWithTag("profile_test_notification_button")
        testBtn.assertIsDisplayed()
        val btnBounds = testBtn.getUnclippedBoundsInRoot()
        assertTrue("Test notification button must fit within screen", btnBounds.right <= rootBounds.right)
    }

    @Test
    fun reminder_settings_can_be_toggled_changed_and_saved() {
        var savedProfile: UserProfileEntity? = null
        var testNotificationTriggered = false

        val initialProfile = UserProfileEntity(
            nickname = "Maya",
            reminderEnabled = true,
            reminderBeforeMinutes = 10
        )

        composeTestRule.setContent {
            StudyBloomTheme(themeMode = "LIGHT") {
                ProfileScreen(
                    userProfile = initialProfile,
                    onSaveProfile = { savedProfile = it },
                    onTestNotification = { testNotificationTriggered = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Scroll LazyColumn to the Session Reminders card
        composeTestRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasTestTag("profile_test_notification_button"))
        composeTestRule.waitForIdle()

        // Toggle switch OFF -> interval chips should hide
        composeTestRule.onNodeWithTag("profile_session_reminders_switch")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reminder Interval").assertDoesNotExist()

        // Toggle switch ON -> interval chips reappear
        composeTestRule.onNodeWithTag("profile_session_reminders_switch")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reminder Interval").assertIsDisplayed()

        // Select 30m interval
        composeTestRule.onNodeWithTag("profile_reminder_interval_30m")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("30 min before").assertIsDisplayed()

        // Test notification button preserves existing notification trigger
        composeTestRule.onNodeWithTag("profile_test_notification_button")
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue("Test notification callback must be invoked", testNotificationTriggered)

        // Scroll back to top Save button and save preferences
        composeTestRule.onNode(hasScrollToIndexAction())
            .performScrollToIndex(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_save_button")
            .performClick()
        composeTestRule.waitForIdle()

        assertNotNull(savedProfile)
        assertTrue(savedProfile!!.reminderEnabled)
        assertEquals(30, savedProfile!!.reminderBeforeMinutes)
    }

    @Test
    fun room_database_saves_and_loads_reminder_preferences_accurately() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, StudyBloomDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = db.userProfileDao()
            val updated = UserProfileEntity(
                id = 1,
                reminderEnabled = false,
                reminderBeforeMinutes = 30
            )
            dao.insertProfile(updated)

            val loaded = dao.getUserProfileOnce()
            assertNotNull(loaded)
            assertFalse(loaded!!.reminderEnabled)
            assertEquals(30, loaded.reminderBeforeMinutes)

            dao.insertProfile(loaded.copy(reminderEnabled = true, reminderBeforeMinutes = 15))
            val reloaded = dao.getUserProfileOnce()
            assertTrue(reloaded!!.reminderEnabled)
            assertEquals(15, reloaded.reminderBeforeMinutes)
        } finally {
            db.close()
        }
    }

    @Test
    fun notification_helper_posts_task_reminder_unchanged() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationHelper.showTaskReminder(
            context = context,
            taskTitle = "Algorithm Practice",
            subject = "Computer Science",
            startTime = "19:00",
            notificationId = 4242
        )

        val shadowNm = shadowOf(notificationManager)
        val posted = shadowNm.getNotification(4242)
        assertNotNull("Task reminder notification should be posted", posted)
        assertEquals(NotificationHelper.CHANNEL_REMINDERS, posted.channelId)
    }
}
