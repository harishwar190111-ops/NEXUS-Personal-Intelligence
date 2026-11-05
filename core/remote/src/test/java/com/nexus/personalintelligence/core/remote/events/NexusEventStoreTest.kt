package com.nexus.personalintelligence.core.remote.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NexusEventStoreTest {

    @Test
    fun testAddEventAndListNewestFirst() {
        val store = NexusEventStore(maxCapacity = 10)
        val event1 = NexusEvent(
            id = "evt-1",
            title = "Pairing Initiated",
            message = "Mobile requested pairing",
            timestamp = 1000L
        )
        val event2 = NexusEvent(
            id = "evt-2",
            title = "Pairing Approved",
            message = "TV approved pairing request",
            timestamp = 2000L
        )
        val event3 = NexusEvent(
            id = "evt-3",
            title = "Command Executed",
            message = "DPAD_CENTER pressed",
            timestamp = 1500L
        )

        store.addEvent(event1)
        store.addEvent(event2)
        store.addEvent(event3)

        val listed = store.listEvents()
        assertEquals(3, listed.size)
        // Listed in descending order of timestamp: event2 (2000), event3 (1500), event1 (1000)
        assertEquals("evt-2", listed[0].id)
        assertEquals("evt-3", listed[1].id)
        assertEquals("evt-1", listed[2].id)
    }

    @Test
    fun testBoundedCapacityEviction() {
        val store = NexusEventStore(maxCapacity = 3)
        assertEquals(0, store.size())

        store.addEvent(NexusEvent(id = "e1", title = "Event 1", message = "m1", timestamp = 100L))
        store.addEvent(NexusEvent(id = "e2", title = "Event 2", message = "m2", timestamp = 200L))
        store.addEvent(NexusEvent(id = "e3", title = "Event 3", message = "m3", timestamp = 300L))
        assertEquals(3, store.size())

        // 4th event should evict oldest (e1)
        store.addEvent(NexusEvent(id = "e4", title = "Event 4", message = "m4", timestamp = 400L))
        assertEquals(3, store.size())

        val ids = store.listEvents().map { it.id }
        assertFalse("Oldest event e1 should have been evicted", ids.contains("e1"))
        assertTrue("e2, e3, e4 should remain", ids.containsAll(listOf("e2", "e3", "e4")))
    }

    @Test
    fun testMarkAsReadAndUnreadCount() {
        val store = NexusEventStore(maxCapacity = 5)
        val e1 = NexusEvent(id = "e1", title = "E1", message = "m1")
        val e2 = NexusEvent(id = "e2", title = "E2", message = "m2")
        val e3 = NexusEvent(id = "e3", title = "E3", message = "m3")

        store.addEvent(e1)
        store.addEvent(e2)
        store.addEvent(e3)

        assertEquals(3, store.unreadCount())

        // Mark e2 as read
        val marked = store.markAsRead("e2")
        assertTrue(marked)
        assertEquals(2, store.unreadCount())

        // Filter unread only
        val unreadList = store.listEvents(unreadOnly = true)
        assertEquals(2, unreadList.size)
        assertFalse(unreadList.any { it.id == "e2" })

        // Mark all as read
        store.markAllAsRead()
        assertEquals(0, store.unreadCount())
        assertEquals(0, store.listEvents(unreadOnly = true).size)
        assertEquals(3, store.listEvents(unreadOnly = false).size)

        // Non-existent ID returns false
        assertFalse(store.markAsRead("unknown-id"))
    }

    @Test
    fun testClearAll() {
        val store = NexusEventStore(maxCapacity = 5)
        store.addEvent(NexusEvent(id = "e1", title = "E1", message = "m1"))
        store.addEvent(NexusEvent(id = "e2", title = "E2", message = "m2"))
        assertEquals(2, store.size())

        store.clearAll()
        assertEquals(0, store.size())
        assertEquals(0, store.listEvents().size)
    }
}
