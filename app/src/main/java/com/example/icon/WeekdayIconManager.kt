package com.example.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.BuildConfig
import java.util.Calendar
import java.util.TimeZone

enum class WeekdayAlias(
    val calendarDayOfWeek: Int,
    val abbreviation: String,
    val aliasClassName: String,
    val defaultEnabledInManifest: Boolean = false
) {
    MONDAY(Calendar.MONDAY, "MON", "com.example.LauncherMon", defaultEnabledInManifest = true),
    TUESDAY(Calendar.TUESDAY, "TUE", "com.example.LauncherTue"),
    WEDNESDAY(Calendar.WEDNESDAY, "WED", "com.example.LauncherWed"),
    THURSDAY(Calendar.THURSDAY, "THU", "com.example.LauncherThu"),
    FRIDAY(Calendar.FRIDAY, "FRI", "com.example.LauncherFri"),
    SATURDAY(Calendar.SATURDAY, "SAT", "com.example.LauncherSat"),
    SUNDAY(Calendar.SUNDAY, "SUN", "com.example.LauncherSun");

    fun componentName(packageName: String): ComponentName =
        ComponentName(packageName, aliasClassName)

    companion object {
        fun fromCalendarDayOfWeek(dayOfWeek: Int): WeekdayAlias {
            return entries.firstOrNull { it.calendarDayOfWeek == dayOfWeek } ?: MONDAY
        }
    }
}

object WeekdayIconManager {
    private const val PREFS_NAME = "studybloom_weekday_icon_prefs"
    private const val KEY_ACTIVE_ABBREVIATION = "active_weekday_abbreviation"
    private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"

    /**
     * Optional debug-only clock override for local verification without waiting 7 days.
     * Ignored automatically in release builds (!BuildConfig.DEBUG).
     */
    @Volatile
    internal var debugOverrideTimestampMillis: Long? = null

    @Volatile
    internal var debugOverrideTimeZone: TimeZone? = null

    fun currentEffectiveTimestampMillis(): Long {
        if (BuildConfig.DEBUG) {
            debugOverrideTimestampMillis?.let { return it }
        }
        return System.currentTimeMillis()
    }

    fun currentEffectiveTimeZone(): TimeZone {
        if (BuildConfig.DEBUG) {
            debugOverrideTimeZone?.let { return it }
        }
        return TimeZone.getDefault()
    }

    fun resolveWeekday(
        nowMillis: Long = currentEffectiveTimestampMillis(),
        timeZone: TimeZone = currentEffectiveTimeZone()
    ): WeekdayAlias {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }
        return WeekdayAlias.fromCalendarDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
    }

    /**
     * Checks the current device local date & timezone and updates launcher activity-alias
     * component states only when needed.
     *
     * @return true if component state was modified, false if already up to date or skipped safely.
     */
    fun syncLauncherIcon(context: Context): Boolean {
        return syncLauncherIconForInstant(
            context = context,
            nowMillis = currentEffectiveTimestampMillis(),
            timeZone = currentEffectiveTimeZone()
        )
    }

    internal fun syncLauncherIconForInstant(
        context: Context,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val targetAlias = resolveWeekday(nowMillis = nowMillis, timeZone = timeZone)
        return applyWeekdayAlias(context, targetAlias, nowMillis)
    }

    @Synchronized
    internal fun applyWeekdayAlias(
        context: Context,
        targetAlias: WeekdayAlias,
        recordedTimestamp: Long = currentEffectiveTimestampMillis()
    ): Boolean {
        val appContext = context.applicationContext ?: context
        val pm = appContext.packageManager
        val pkg = appContext.packageName

        val targetComponent = targetAlias.componentName(pkg)
        val targetAlreadyEnabled = isEffectivelyEnabled(pm, targetComponent, targetAlias.defaultEnabledInManifest)
        val allOthersDisabled = WeekdayAlias.entries
            .filter { it != targetAlias }
            .all { other ->
                !isEffectivelyEnabled(pm, other.componentName(pkg), other.defaultEnabledInManifest)
            }

        if (targetAlreadyEnabled && allOthersDisabled) {
            saveSyncedMetadata(appContext, targetAlias, recordedTimestamp)
            return false
        }

        // CRITICAL SAFETY ORDER:
        // Always enable the new target launcher alias FIRST with DONT_KILL_APP before disabling
        // previously active aliases, ensuring there is always at least one enabled launcher entry
        // and never touching MainActivity or the application itself.
        var changed = false
        if (!targetAlreadyEnabled) {
            val enabledOk = setComponentStateSafely(
                pm = pm,
                componentName = targetComponent,
                newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            )
            if (!enabledOk) {
                return false
            }
            changed = true
        }

        for (alias in WeekdayAlias.entries) {
            if (alias == targetAlias) continue
            val comp = alias.componentName(pkg)
            if (isEffectivelyEnabled(pm, comp, alias.defaultEnabledInManifest)) {
                val disabledOk = setComponentStateSafely(
                    pm = pm,
                    componentName = comp,
                    newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                )
                if (disabledOk) {
                    changed = true
                }
            }
        }

        saveSyncedMetadata(appContext, targetAlias, recordedTimestamp)
        return changed
    }

    internal fun getActiveAliasInPackageManager(context: Context): WeekdayAlias? {
        val appContext = context.applicationContext ?: context
        val pm = appContext.packageManager
        val pkg = appContext.packageName
        return WeekdayAlias.entries.firstOrNull { alias ->
            isEffectivelyEnabled(pm, alias.componentName(pkg), alias.defaultEnabledInManifest)
        }
    }

    internal fun getEnabledAliasesCount(context: Context): Int {
        val appContext = context.applicationContext ?: context
        val pm = appContext.packageManager
        val pkg = appContext.packageName
        return WeekdayAlias.entries.count { alias ->
            isEffectivelyEnabled(pm, alias.componentName(pkg), alias.defaultEnabledInManifest)
        }
    }

    fun getLastSyncedAbbreviation(context: Context): String? {
        val appContext = context.applicationContext ?: context
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_ABBREVIATION, null)
    }

    internal fun clearTestOverrides() {
        debugOverrideTimestampMillis = null
        debugOverrideTimeZone = null
    }

    private fun isEffectivelyEnabled(
        pm: PackageManager,
        componentName: ComponentName,
        defaultEnabledInManifest: Boolean
    ): Boolean {
        return try {
            when (pm.getComponentEnabledSetting(componentName)) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
                else -> defaultEnabledInManifest
            }
        } catch (_: Exception) {
            defaultEnabledInManifest
        }
    }

    private fun setComponentStateSafely(
        pm: PackageManager,
        componentName: ComponentName,
        newState: Int
    ): Boolean {
        return try {
            pm.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun saveSyncedMetadata(
        context: Context,
        alias: WeekdayAlias,
        timestamp: Long
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_ABBREVIATION, alias.abbreviation)
            .putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp)
            .apply()
    }
}
