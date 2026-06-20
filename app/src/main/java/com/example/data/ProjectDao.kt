package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM collage_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<CollageProject>>

    @Query("SELECT * FROM collage_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): CollageProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: CollageProject): Long

    @Update
    suspend fun updateProject(project: CollageProject)

    @Delete
    suspend fun deleteProject(project: CollageProject)

    @Query("UPDATE collage_projects SET isSynced = 1")
    suspend fun markAllAsSynced()
}
