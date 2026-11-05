package com.nexus.personalintelligence.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduledItemTest {

    @Test
    fun testOnceRuleCalculationAndCompletion() {
        val fixedCurrent = 1_000_000L
        val item = ScheduledItem(
            id = "item-1",
            title = "Turn off TV",
            initialTimeMs = fixedCurrent + 5000L,
            repeatRule = RepeatRule.ONCE
        )

        // Before execution
        assertEquals(fixedCurrent + 5000L, item.calculateNextRun(fixedCurrent))

        // After execution recorded
        val executed = item.copy(lastRunTimeMs = fixedCurrent + 5000L)
        assertNull("One-shot schedule must return null once executed", executed.calculateNextRun(fixedCurrent + 6000L))
    }

    @Test
    fun testDailyRepeatingNextRunCalculation() {
        val t0 = 10_000_000L
        val dayMs = 86_400_000L
        val item = ScheduledItem(
            id = "daily-1",
            title = "Morning News",
            initialTimeMs = t0,
            repeatRule = RepeatRule.DAILY
        )

        // Before first execution
        assertEquals(t0, item.calculateNextRun(t0))

        // After first run recorded at t0
        val afterRun1 = item.copy(lastRunTimeMs = t0)
        assertEquals(t0 + dayMs, afterRun1.calculateNextRun(t0 + 1000L))

        // After second run
        val afterRun2 = afterRun1.copy(lastRunTimeMs = t0 + dayMs)
        assertEquals(t0 + 2 * dayMs, afterRun2.calculateNextRun(t0 + dayMs + 1000L))
    }

    @Test
    fun testRequiresApprovalFlagRespected() {
        val fixedCurrent = 2_000_000L
        val item = ScheduledItem(
            id = "disruptive-1",
            title = "Reboot TV Stick",
            initialTimeMs = fixedCurrent + 10_000L,
            requiresApproval = true,
            isApproved = false
        )

        // Unapproved disruptive action must NOT be scheduled for execution
        assertNull("Requires-approval action must not return nextRun when unapproved", item.calculateNextRun(fixedCurrent))

        // Once approved
        val approved = item.copy(isApproved = true)
        assertEquals(fixedCurrent + 10_000L, approved.calculateNextRun(fixedCurrent))
    }

    @Test
    fun testDisabledItemReturnsNull() {
        val item = ScheduledItem(
            id = "disabled-1",
            title = "Inactive task",
            initialTimeMs = 5000L,
            isEnabled = false
        )
        assertNull(item.calculateNextRun(1000L))
    }

    @Test
    fun testStoreItemManagementAndDueQuery() {
        val store = ScheduledItemStore(maxItems = 10)
        val tNow = 5_000_000L

        val pastDue = ScheduledItem(id = "due-1", title = "Overdue task", initialTimeMs = tNow - 1000L)
        val future = ScheduledItem(id = "fut-1", title = "Future task", initialTimeMs = tNow + 10_000L)
        val unapproved = ScheduledItem(id = "unapp-1", title = "Pending approval", initialTimeMs = tNow - 500L, requiresApproval = true, isApproved = false)

        store.addItem(pastDue)
        store.addItem(future)
        store.addItem(unapproved)
        assertEquals(3, store.size())

        // Query due items at tNow
        val due = store.getNextDueItems(tNow)
        assertEquals(1, due.size)
        assertEquals("due-1", due[0].id)

        // Approve unapproved item
        store.approve("unapp-1")
        val dueAfterApprove = store.getNextDueItems(tNow)
        assertEquals(2, dueAfterApprove.size)
        assertTrue(dueAfterApprove.map { it.id }.containsAll(listOf("due-1", "unapp-1")))

        // Disable pastDue
        store.setEnabled("due-1", false)
        val dueAfterDisable = store.getNextDueItems(tNow)
        assertEquals(1, dueAfterDisable.size)
        assertEquals("unapp-1", dueAfterDisable[0].id)

        // Remove
        store.removeItem("unapp-1")
        assertEquals(2, store.size())
    }
}
