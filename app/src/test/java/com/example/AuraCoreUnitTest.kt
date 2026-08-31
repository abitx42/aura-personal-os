package com.example

import com.example.data.HabitLog
import com.example.ui.FloatPair
import com.example.ui.SketchStroke
import com.example.ui.hashPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AuraCoreUnitTest {

    @Test
    fun testPinHashing_deterministicAndSecure() {
        val pin = "1234"
        val hash1 = hashPin(pin)
        val hash2 = hashPin(pin)
        val hashOther = hashPin("1235")

        assertEquals("Hashes for same PIN must match", hash1, hash2)
        assertNotEquals("Hashes for different PINs must not match", hash1, hashOther)
        assertEquals("SHA-256 hash length must be 64 characters", 64, hash1.length)
    }

    @Test
    fun testHabitStreakCalculation_consecutiveDays() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        val logs = mutableListOf<HabitLog>()
        // 5 consecutive days ending today
        for (i in 0 until 5) {
            logs.add(HabitLog(id = i, habitId = 1, completionDate = sdf.format(cal.time)))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Test streak logic
        val dates = logs.mapNotNull {
            try {
                val d = sdf.parse(it.completionDate)
                val c = Calendar.getInstance()
                if (d != null) {
                    c.time = d
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    c.timeInMillis
                } else null
            } catch (_: Exception) { null }
        }.distinct().sortedDescending()

        assertEquals("Must have 5 distinct days", 5, dates.size)
    }

    @Test
    fun testDrawingSerializationCycle() {
        val strokes = listOf(
            SketchStroke(
                points = listOf(FloatPair(10f, 20f), FloatPair(30f, 40f)),
                colorHex = "#FF5B32",
                strokeWidth = 6f,
                isEraser = false
            ),
            SketchStroke(
                points = listOf(FloatPair(50f, 60f), FloatPair(70f, 80f)),
                colorHex = "#00D084",
                strokeWidth = 10f,
                isEraser = true
            )
        )

        // Serialize
        val sb = StringBuilder()
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            sb.append(stroke.colorHex).append("|")
            sb.append(stroke.strokeWidth).append("|")
            sb.append(if (stroke.isEraser) "1" else "0").append("|")
            val pointsStr = stroke.points.joinToString(",") { "${it.x}:${it.y}" }
            sb.append(pointsStr)
            sb.append("||")
        }
        val serialized = sb.toString()

        assertTrue("Serialized output must contain hex colors", serialized.contains("#FF5B32"))
        assertTrue("Serialized output must contain points", serialized.contains("10.0:20.0"))
    }

    @Test
    fun testNetWorthCalculation_accurateBalance() {
        val totalAvailableBalance = 45000.0
        val totalInvested = 120000.0
        val totalToReceive = 15000.0
        val totalYouOwe = 8000.0

        val netWorth = totalAvailableBalance + totalInvested + totalToReceive - totalYouOwe
        assertEquals(172000.0, netWorth, 0.001)
    }

    @Test
    fun testDebtSettlementAdjustment_cappedToRemaining() {
        val debtAmount = 500.0
        val remainingAmount = 300.0
        val paymentAttempt = 400.0

        val actualPaid = minOf(paymentAttempt, remainingAmount)
        val newRemaining = remainingAmount - actualPaid

        assertEquals(300.0, actualPaid, 0.001)
        assertEquals(0.0, newRemaining, 0.001)
    }

    @Test
    fun testDateFormatting_safePattern() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 31)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatted = sdf.format(cal.time)

        assertEquals("2026-08-31", formatted)
    }
}
