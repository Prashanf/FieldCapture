package com.fieldcapture.sync

import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.network.ConnectivityObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityObserverTest {

    @Test
    fun `queue worker names are consistent`() {
        assertEquals("extraction_queue_processing", ExtractionQueueWorker.WORK_NAME)
        assertEquals("extraction_queue_periodic", ExtractionQueueWorker.PERIODIC_WORK_NAME)
    }

    @Test
    fun `status enum has all expected values`() {
        val statuses = ConnectivityObserver.Status.entries
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(ConnectivityObserver.Status.AVAILABLE))
        assertTrue(statuses.contains(ConnectivityObserver.Status.UNAVAILABLE))
        assertTrue(statuses.contains(ConnectivityObserver.Status.LOSING))
        assertTrue(statuses.contains(ConnectivityObserver.Status.LOST))
    }

    @Test
    fun `capture status transitions are valid`() {
        val validTransitions = mapOf(
            CaptureStatus.CAPTURED to setOf(CaptureStatus.EXTRACTING, CaptureStatus.QUEUED),
            CaptureStatus.QUEUED to setOf(CaptureStatus.EXTRACTING),
            CaptureStatus.EXTRACTING to setOf(
                CaptureStatus.EXTRACTED,
                CaptureStatus.EXTRACTION_FAILED,
                CaptureStatus.QUEUED
            ),
            CaptureStatus.EXTRACTION_FAILED to setOf(CaptureStatus.QUEUED, CaptureStatus.EXTRACTING),
            CaptureStatus.EXTRACTED to setOf(CaptureStatus.SYNCED)
        )

        for (status in CaptureStatus.entries) {
            if (status != CaptureStatus.SYNCED) {
                assertTrue(
                    "Missing transitions for $status",
                    validTransitions.containsKey(status)
                )
            }
        }
    }
}
