package com.fieldcapture.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.fieldcapture.data.local.dao.CaptureDao
import com.fieldcapture.data.local.dao.JobRecordDao
import com.fieldcapture.data.local.entity.CaptureEntity
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.data.local.entity.JobRecordEntity

class FieldCaptureTypeConverters {
    @TypeConverter
    fun fromCaptureStatus(status: CaptureStatus): String = status.name

    @TypeConverter
    fun toCaptureStatus(value: String): CaptureStatus = CaptureStatus.valueOf(value)
}

@Database(
    entities = [CaptureEntity::class, JobRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(FieldCaptureTypeConverters::class)
abstract class FieldCaptureDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun jobRecordDao(): JobRecordDao
}
