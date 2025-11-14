# Bug Fix: Annual Leave Settings Not Restored from Backup

## Problem
When restoring data from a backup (either local JSON or Google Drive), the annual leave settings were not being restored. This meant that previously entered permissions (annual leave days) were not being deducted from the total permission days, causing incorrect calculations.

## Root Cause
The backup/restore functionality was only saving and restoring the schedule data, but not the annual leave settings. When data was restored, the app would start with default annual leave settings (30 total days, 0 used), regardless of what had been previously used.

## Solution
Modified the data serialization/deserialization to include annual leave settings along with schedule data:

### 1. Created ScheduleData class
Added a new data class to encapsulate both schedule and annual leave settings:
```kotlin
data class ScheduleData(
    val schedule: Map<String, String>,
    val annualLeaveSettings: AnnualLeaveSettings
)
```

### 2. Updated toJson() method
Modified the [toJson](file:///C:/Users/BULUT/Desktop/Nobet_Listem/app/src/main/java/com/example/nobet/ui/calendar/ScheduleViewModel.kt#L222-L229) method in ScheduleViewModel to serialize both schedule and annual leave settings:
```kotlin
fun toJson(): String {
    val stringMap = schedule.mapKeys { it.key.toString() }.mapValues { it.value.name }
    val scheduleData = ScheduleData(
        schedule = stringMap,
        annualLeaveSettings = annualLeaveSettings
    )
    return gson.toJson(scheduleData)
}
```

### 3. Updated fromJson() method
Modified the [fromJson](file:///C:/Users/BULUT/Desktop/Nobet_Listem/app/src/main/java/com/example/nobet/ui/calendar/ScheduleViewModel.kt#L231-L270) method to deserialize both schedule and annual leave settings:
```kotlin
fun fromJson(json: String) {
    try {
        val scheduleData: ScheduleData = gson.fromJson(json, ScheduleData::class.java)
        
        // Restore schedule
        val mapped = mutableMapOf<LocalDate, ShiftType>()
        scheduleData.schedule.forEach { (key, value) ->
            try {
                val date = LocalDate.parse(key)
                val shiftType = ShiftType.valueOf(value)
                mapped[date] = shiftType
            } catch (e: Exception) {}
        }
        schedule.clear()
        schedule.putAll(mapped)
        
        // Restore annual leave settings
        annualLeaveSettings = scheduleData.annualLeaveSettings
        
        saveScheduleToPrefs()
        saveAnnualLeaveSettings()
    } catch (e: Exception) {
        // Fallback to old format for backward compatibility
        // ... existing code for old format
    }
}
```

### 4. Updated saveScheduleToPrefs() method
Modified the [saveScheduleToPrefs](file:///C:/Users/BULUT/Desktop/Nobet_Listem/app/src/main/java/com/example/nobet/ui/calendar/ScheduleViewModel.kt#L180-L191) method to save both schedule and annual leave settings:
```kotlin
private fun saveScheduleToPrefs() {
    try {
        val stringMap = schedule.mapKeys { it.key.toString() }.mapValues { it.value.name }
        val scheduleData = ScheduleData(
            schedule = stringMap,
            annualLeaveSettings = annualLeaveSettings
        )
        sharedPreferences?.edit()?.putString(SCHEDULE_KEY, gson.toJson(scheduleData))?.apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

### 5. Added backward compatibility
The updated [fromJson](file:///C:/Users/BULUT/Desktop/Nobet_Listem/app/src/main/java/com/example/nobet/ui/calendar/ScheduleViewModel.kt#L231-L270) method includes a fallback to handle old backup files that don't include annual leave settings.

### 6. Updated NotificationScheduler
Updated the [loadScheduleFromPreferences](file:///C:/Users/BULUT/Desktop/Nobet_Listem/app/src/main/java/com/example/nobet/notification/NotificationScheduler.kt#L32-L49) method in NotificationScheduler to handle the new data structure while maintaining backward compatibility.

## Testing
Added unit tests to verify:
1. Serialization and deserialization of both schedule and annual leave settings
2. Backward compatibility with old backup formats

## Impact
This fix ensures that when a backup is restored, both the schedule data and the annual leave settings are properly restored, which means that previously entered permissions will be correctly deducted from the total permission days.