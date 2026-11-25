package com.fieldcapture.data

import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.SessionMetadata
import com.fieldcapture.util.JsonExporter
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JobRecordSchemaTest {

    @Test
    fun `exported json contains required top level schema keys`() {
        val exporter = JsonExporter()
        val record = JobRecord(
            jobId = "00000000-0000-0000-0000-000000000001",
            createdAt = "2026-11-25T12:00:00Z",
            technicianId = "tech_001",
            session = SessionMetadata(startedAt = "2026-11-25T12:00:00Z")
        )

        val json = exporter.toSchemaJson(record)
        val obj = JsonParser.parseString(json).asJsonObject

        assertEquals("00000000-0000-0000-0000-000000000001", obj.get("job_id").asString)
        assertTrue(obj.has("created_at"))
        assertTrue(obj.has("technician_id"))
        assertTrue(obj.has("location"))
        assertTrue(obj.has("session"))
        assertTrue(obj.has("captures"))
        assertTrue(obj.has("line_items"))
        assertTrue(obj.has("notes"))
    }
}
