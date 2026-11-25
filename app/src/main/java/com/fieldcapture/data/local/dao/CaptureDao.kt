package com.fieldcapture.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fieldcapture.data.local.entity.CaptureEntity
import com.fieldcapture.data.local.entity.CaptureStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Query("SELECT * FROM captures WHERE jobId = :jobId ORDER BY timestamp ASC")
    fun observeCapturesForJob(jobId: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    suspend fun getCaptureById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE status IN (:statuses) ORDER BY timestamp ASC")
    suspend fun getCapturesByStatus(statuses: List<CaptureStatus>): List<CaptureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCapture(entity: CaptureEntity)

    @Query("UPDATE captures SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: CaptureStatus)

    @Query("UPDATE captures SET extractionJson = :extractionJson, status = :status WHERE id = :id")
    suspend fun updateExtraction(id: String, extractionJson: String?, status: CaptureStatus)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteCapture(id: String)
}
