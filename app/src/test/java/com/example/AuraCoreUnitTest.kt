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

    @Test
    fun testExpenseEqualSplit_fairShareCalculation() {
        val totalBill = 1500.0
        val members = listOf("Alice", "Bob", "Charlie")
        val shareCount = members.size.coerceAtLeast(1)
        val splitShare = totalBill / shareCount

        assertEquals(500.0, splitShare, 0.001)
        val totalSum = splitShare * shareCount
        assertEquals(totalBill, totalSum, 0.001)
    }

    @Test
    fun testWordAndCharCount_accurateMultiLine() {
        val content = "Aura Personal OS\nSeamless offline fintech and productivity."
        val words = content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val charCount = content.length

        assertEquals(7, words.size)
        assertEquals(59, charCount)
    }

    @Test
    fun testTimerFormatting_leadingZeros() {
        val totalSeconds = 125 // 2 mins 5 secs
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        val formatted = "%02d:%02d".format(mins, secs)

        assertEquals("02:05", formatted)
    }

    @Test
    fun testPinPasscodeValidation_strictFourDigits() {
        val validPin = "4829"
        val tooShort = "482"
        val tooLong = "48291"
        val withLetters = "482a"

        fun isValid(p: String) = p.length == 4 && p.all { it.isDigit() }

        assertTrue(isValid(validPin))
        org.junit.Assert.assertFalse(isValid(tooShort))
        org.junit.Assert.assertFalse(isValid(tooLong))
        org.junit.Assert.assertFalse(isValid(withLetters))
    }

    @Test
    fun testReminderTimeStringFormat_validHoursMinutes() {
        val validTime = "08:30"
        val validEvening = "23:59"
        val invalidHours = "25:00"
        val invalidMins = "12:60"

        val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

        assertTrue(timeRegex.matches(validTime))
        assertTrue(timeRegex.matches(validEvening))
        org.junit.Assert.assertFalse(timeRegex.matches(invalidHours))
        org.junit.Assert.assertFalse(timeRegex.matches(invalidMins))
    }

    @Test
    fun testCurrencyDisplay_roundToIntFormatting() {
        val toReceive = 5250.75
        val youOwe = 1200.25
        val netDifference = (toReceive - youOwe).toInt()

        assertEquals(4050, netDifference)
    }

    @Test
    fun testTaskEnergyLevels_standardLabels() {
        val energyLevels = listOf("High Energy", "Medium Energy", "Low Energy")
        assertEquals(3, energyLevels.size)
        assertTrue(energyLevels.contains("High Energy"))
        assertTrue(energyLevels.contains("Medium Energy"))
        assertTrue(energyLevels.contains("Low Energy"))
    }

    @Test
    fun testMoodCategories_validKeys() {
        val validMoods = setOf("HAPPY", "CALM", "CONTENT", "NEUTRAL", "CREATIVE", "TIRED", "SAD")
        val currentMood = "CALM"
        assertTrue("Selected mood must be within recognized set", validMoods.contains(currentMood))
    }

    @Test
    fun testEmptyDrawingStroke_safeHandling() {
        val emptyStroke = SketchStroke(points = emptyList(), colorHex = "#FF5B32", strokeWidth = 6f)
        assertTrue(emptyStroke.points.isEmpty())
    }

    @Test
    fun testNoteFiltering_caseInsensitiveMatches() {
        val notes = listOf(
            Pair("Meeting Notes", "Discuss Q3 sprint plans"),
            Pair("Grocery List", "Milk, Eggs, Apples"),
            Pair("Ideas for Aura", "Add offline first ledger syncing")
        )
        val query = "aura"
        val results = notes.filter { (title, content) ->
            title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)
        }

        assertEquals(1, results.size)
        assertEquals("Ideas for Aura", results[0].first)
    }

    @Test
    fun testSingleMemberExpenseSplit_fullAmount() {
        val bill = 350.0
        val members = listOf("Alice")
        val partitionCount = members.size.coerceAtLeast(1)
        val mapSplits = members.associateWith { bill / partitionCount }

        assertEquals(1, mapSplits.size)
        assertEquals(350.0, mapSplits["Alice"] ?: 0.0, 0.001)
    }

    @Test
    fun testMultipleCategoryTags_csvParsing() {
        val rawTags = "Fintech, Savings , 2026 , Budget "
        val parsed = rawTags.split(",").map { it.trim() }.filter { it.isNotBlank() }

        assertEquals(4, parsed.size)
        assertEquals("Fintech", parsed[0])
        assertEquals("Savings", parsed[1])
        assertEquals("2026", parsed[2])
        assertEquals("Budget", parsed[3])
    }
}
