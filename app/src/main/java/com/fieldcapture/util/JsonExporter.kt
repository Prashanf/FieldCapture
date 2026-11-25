package com.fieldcapture.util

import android.content.Context
import com.fieldcapture.domain.model.JobRecord
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    fun exportJobRecord(jobRecord: JobRecord): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val fileName = "job_record_${jobRecord.jobId}_${System.currentTimeMillis()}.json"
        val file = File(exportDir, fileName)
        file.writeText(gson.toJson(jobRecord))
        return file
    }

    fun toJsonString(jobRecord: JobRecord): String {
        return gson.toJson(jobRecord)
    }

    fun loadJobRecord(file: File): JobRecord? {
        return try {
            gson.fromJson(file.readText(), JobRecord::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun listExportedRecords(): List<File> {
        val exportDir = File(context.filesDir, "exports")
        return exportDir.listFiles()
            ?.filter { it.name.startsWith("job_record_") && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
