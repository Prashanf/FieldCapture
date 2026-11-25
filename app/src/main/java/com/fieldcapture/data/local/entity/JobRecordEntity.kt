package com.fieldcapture.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_records")
data class JobRecordEntity(
    @PrimaryKey val jobId: String,
    val createdAt: String,
    val technicianId: String,
    val locationJson: String,
    val sessionJson: String,
    val lineItemsJson: String,
    val notesJson: String,
    val fullRecordJson: String
)
