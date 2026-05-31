package com.clock3.pet

import com.clock3.pet.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmModelTest {

    @Test
    fun repeatType_fromValue() {
        assertEquals(Alarm.RepeatType.ONCE, Alarm.RepeatType.fromValue("once"))
        assertEquals(Alarm.RepeatType.DAILY, Alarm.RepeatType.fromValue("daily"))
        assertEquals(Alarm.RepeatType.WORKDAYS, Alarm.RepeatType.fromValue("workdays"))
        assertEquals(Alarm.RepeatType.WEEKEND, Alarm.RepeatType.fromValue("weekend"))
        assertEquals(Alarm.RepeatType.CUSTOM, Alarm.RepeatType.fromValue("custom"))
    }

    @Test
    fun repeatType_fromValue_unknown() {
        assertEquals(Alarm.RepeatType.ONCE, Alarm.RepeatType.fromValue("unknown"))
    }

    @Test
    fun getNextTriggerTime_invalidTimeFormat() {
        val alarm = Alarm(label = "test", time = "invalid", repeatType = Alarm.RepeatType.ONCE)
        assertNull(alarm.getNextTriggerTime())
    }

    @Test
    fun toEntity_and_fromEntity_roundTrip() {
        val alarm = Alarm(
            id = 1,
            label = "Test Alarm",
            time = "08:30",
            content = "Wake up",
            repeatType = Alarm.RepeatType.WORKDAYS,
            repeatDays = listOf(1, 2, 3, 4, 5),
            enabled = true
        )
        val entity = alarm.toEntity()
        val restored = Alarm.fromEntity(entity)
        assertEquals(alarm.id, restored.id)
        assertEquals(alarm.label, restored.label)
        assertEquals(alarm.time, restored.time)
        assertEquals(alarm.content, restored.content)
        assertEquals(alarm.repeatType, restored.repeatType)
        assertEquals(alarm.repeatDays, restored.repeatDays)
        assertEquals(alarm.enabled, restored.enabled)
    }

    @Test
    fun toExportMap_and_fromExportMap_roundTrip() {
        val alarm = Alarm(
            id = 1,
            label = "Test",
            time = "09:00",
            content = "Meeting",
            repeatType = Alarm.RepeatType.CUSTOM,
            repeatDays = listOf(1, 3, 5),
            enabled = false
        )
        val exportMap = alarm.toExportMap()
        val restored = Alarm.fromExportMap(exportMap)
        assertEquals(alarm.label, restored.label)
        assertEquals(alarm.time, restored.time)
        assertEquals(alarm.content, restored.content)
        assertEquals(alarm.repeatType, restored.repeatType)
        assertEquals(alarm.repeatDays, restored.repeatDays)
        assertEquals(alarm.enabled, restored.enabled)
    }
}
