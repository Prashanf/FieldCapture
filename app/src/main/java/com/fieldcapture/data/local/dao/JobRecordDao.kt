package com.fieldcapture.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fieldcapture.data.local.entity.JobRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobRecordDao {

    @Query("SELECT * FROM job_records WHERE jobId = :jobId LIMIT 1")
    fun observeJobRecord(jobId: String): Flow<JobRecordEntity?>

    @Query("SELECT * FROM job_records ORDER BY createdAt DESC")
    fun observeAllJobRecords(): Flow<List<JobRecordEntity>>

    @Query("SELECT * FROM job_records WHERE jobId = :jobId LIMIT 1")
    suspend fun getJobRecord(jobId: String): JobRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJobRecord(entity: JobRecordEntity)

    @Query("DELETE FROM job_records WHERE jobId = :jobId")
    suspend fun deleteJobRecord(jobId: String)
}
