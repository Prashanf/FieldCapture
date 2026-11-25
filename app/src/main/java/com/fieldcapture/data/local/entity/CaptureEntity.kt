package com.fieldcapture.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val type: String,
    val imagePath: String?,
    val noteText: String?,
    val status: CaptureStatus,
    val qualityJson: String?,
    val extractionJson: String?,
    val metadataJson: String?,
    val timestamp: String
)
