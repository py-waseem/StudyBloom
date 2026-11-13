package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.icon.WeekdayAlias
import com.example.icon.WeekdayChangeReceiver
import com.example.icon.WeekdayIconManager
import com.example.icon.WeekdayIconScheduler
import com.example.icon.WeekdayIconWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class DynamicWeekdayIconTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        WeekdayIconManager.clearTestOverrides()
    }

    @After
    fun tearDown() {
        WeekdayIconManager.clearTestOverrides()
    }

    private fun instantFor(
        year: Int,
        monthZeroBased: Int,
        dayOfMonth: Int,
        hour: Int = 12,
        minute: Int = 0,
        timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    ): Long {
        return Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun all_seven_weekdays_map_and_switch_with_exactly_one_enabled_alias() {
        val utc = TimeZone.getTimeZone("UTC")
        // 2026-11-09 is Monday -> through 2026-11-15 Sunday
        val weekCases = listOf(
            Pair(9, WeekdayAlias.MONDAY to "MON"),
            Pair(10, WeekdayAlias.TUESDAY to "TUE"),
            Pair(11, WeekdayAlias.WEDNESDAY to "WED"),
            Pair(12, WeekdayAlias.THURSDAY to "THU"),
            Pair(13, WeekdayAlias.FRIDAY to "FRI"),
            Pair(14, WeekdayAlias.SATURDAY to "SAT"),
            Pair(15, WeekdayAlias.SUNDAY to "SUN")
        )

        for ((dayOfMonth, expected) in weekCases) {
            val (expectedAlias, expectedAbbr) = expected
            val instant = instantFor(2026, Calendar.NOVEMBER, dayOfMonth, 10, 30, utc)

            WeekdayIconManager.syncLauncherIconForInstant(context, instant, utc)

            assertEquals(expectedAlias, WeekdayIconManager.getActiveAliasInPackageManager(context))
            assertEquals(expectedAbbr, WeekdayIconManager.getLastSyncedAbbreviation(context))
            assertEquals(
                "Only 1 launcher alias may be enabled on $expectedAbbr",
                1,
                WeekdayIconManager.getEnabledAliasesCount(context)
            )
            // Calling again for the same day must be an idempotent no-op
            assertFalse(WeekdayIconManager.syncLauncherIconForInstant(context, instant + 3_600_000L, utc))
        }
    }

    @Test
    fun handles_month_year_and_leap_year_boundaries_accurately() {
        val utc = TimeZone.getTimeZone("UTC")

        // Month rollover: Jan 31, 2026 (Saturday) -> Feb 1, 2026 (Sunday)
        WeekdayIconManager.syncLauncherIconForInstant(context, instantFor(2026, Calendar.JANUARY, 31, 23, 59, utc), utc)
        assertEquals(WeekdayAlias.SATURDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        WeekdayIconManager.syncLauncherIconForInstant(context, instantFor(2026, Calendar.FEBRUARY, 1, 0, 1, utc), utc)
        assertEquals(WeekdayAlias.SUNDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        // Year rollover: Dec 31, 2026 (Thursday) -> Jan 1, 2027 (Friday)
        WeekdayIconManager.syncLauncherIconForInstant(context, instantFor(2026, Calendar.DECEMBER, 31, 23, 55, utc), utc)
        assertEquals(WeekdayAlias.THURSDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        WeekdayIconManager.syncLauncherIconForInstant(context, instantFor(2027, Calendar.JANUARY, 1, 0, 5, utc), utc)
        assertEquals(WeekdayAlias.FRIDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        // Leap year: Feb 28, 2024 (Wednesday) -> Feb 29, 2024 (Thursday) -> Mar 1, 2024 (Friday)
        WeekdayIconManager.syncLauncherIconForInstant(context, instantFor(2024, Calendar.FEBRUARY, 28, 12, 0, utc), utc)
        assertEquals(WeekdayAlias.WEDNESDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        WeekdayIconManager.syncLauncherIconForInstant(context, instantFor(2024, Calendar.FEBRUARY, 29, 12, 0, utc), utc)
        assertEquals(WeekdayAlias.THURSDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        WeekdayIconManager.syncLauncherIconForInstant(context, instantFor(2024, Calendar.MARCH, 1, 12, 0, utc), utc)
        assertEquals(WeekdayAlias.FRIDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))
    }

    @Test
    fun timezone_change_crossing_calendar_day_updates_icon_according_to_local_zone() {
        val laZone = TimeZone.getTimeZone("America/Los_Angeles")
        val tokyoZone = TimeZone.getTimeZone("Asia/Tokyo")

        // 2026-11-12 20:00 America/Los_Angeles (Thursday) == 2026-11-13 13:00 Asia/Tokyo (Friday)
        val sameInstant = instantFor(2026, Calendar.NOVEMBER, 12, 20, 0, laZone)

        WeekdayIconManager.syncLauncherIconForInstant(context, sameInstant, laZone)
        assertEquals(WeekdayAlias.THURSDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))
        assertEquals("THU", WeekdayIconManager.getLastSyncedAbbreviation(context))

        WeekdayIconManager.syncLauncherIconForInstant(context, sameInstant, tokyoZone)
        assertEquals(WeekdayAlias.FRIDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))
        assertEquals("FRI", WeekdayIconManager.getLastSyncedAbbreviation(context))
        assertEquals(1, WeekdayIconManager.getEnabledAliasesCount(context))
    }

    @Test
    fun reboot_date_time_and_timezone_broadcasts_and_worker_restore_correct_weekday_icon() = runBlocking {
        val utc = TimeZone.getTimeZone("UTC")
        val fridayInstant = instantFor(2026, Calendar.NOVEMBER, 13, 9, 0, utc)
        WeekdayIconManager.debugOverrideTimestampMillis = fridayInstant
        WeekdayIconManager.debugOverrideTimeZone = utc

        // Start intentionally on Monday
        WeekdayIconManager.applyWeekdayAlias(context, WeekdayAlias.MONDAY)
        assertEquals(WeekdayAlias.MONDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        // Simulate BOOT_COMPLETED receiver -> restores Friday (FRI)
        val receiver = WeekdayChangeReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(WeekdayAlias.FRIDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        // Advance clock to Saturday and simulate TIMEZONE_CHANGED + DATE_CHANGED
        val saturdayInstant = instantFor(2026, Calendar.NOVEMBER, 14, 0, 5, utc)
        WeekdayIconManager.debugOverrideTimestampMillis = saturdayInstant
        receiver.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        assertEquals(WeekdayAlias.SATURDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))

        // Advance clock to Sunday and run WorkManager WeekdayIconWorker
        val sundayInstant = instantFor(2026, Calendar.NOVEMBER, 15, 0, 2, utc)
        WeekdayIconManager.debugOverrideTimestampMillis = sundayInstant
        val worker = TestListenableWorkerBuilder<WeekdayIconWorker>(context).build()
        val workResult = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), workResult)
        assertEquals(WeekdayAlias.SUNDAY, WeekdayIconManager.getActiveAliasInPackageManager(context))
        assertEquals(1, WeekdayIconManager.getEnabledAliasesCount(context))

        // Verify next midnight delay stays positive and bounded within 24h + 60s buffer
        val delayMillis = WeekdayIconScheduler.computeDelayUntilNextMidnightMillis(sundayInstant, utc)
        assertTrue(delayMillis in 60_000L..86_460_000L)
    }

    @Test
    fun all_seven_icon_resources_exist_and_main_activity_remains_enabled_and_launchable() {
        val days = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        val res = context.resources
        val pkg = context.packageName

        assertNotEquals(0, res.getIdentifier("ic_launcher_cream_background", "drawable", pkg))
        for (day in days) {
            assertNotEquals("Missing foreground $day", 0, res.getIdentifier("ic_fg_asset_$day", "drawable", pkg))
            assertNotEquals("Missing mipmap ic_launcher_$day", 0, res.getIdentifier("ic_launcher_$day", "mipmap", pkg))
            assertNotEquals("Missing round mipmap ic_launcher_${day}_round", 0, res.getIdentifier("ic_launcher_${day}_round", "mipmap", pkg))
        }

        // MainActivity itself must never be disabled when switching weekday aliases
        val pm = context.packageManager
        val mainActivityState = pm.getComponentEnabledSetting(ComponentName(pkg, "com.example.MainActivity"))
        assertNotEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, mainActivityState)

        // Verify MainActivity launches and resumes cleanly, syncing today's icon
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.pause().resume()
        assertEquals(1, WeekdayIconManager.getEnabledAliasesCount(context))
        assertFalse(controller.get().isFinishing)
    }
}
