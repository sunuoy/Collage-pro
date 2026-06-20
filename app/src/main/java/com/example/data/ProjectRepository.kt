package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<CollageProject>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Int): CollageProject? {
        return projectDao.getProjectById(id)
    }

    suspend fun insert(project: CollageProject): Long {
        return projectDao.insertProject(project)
    }

    suspend fun update(project: CollageProject) {
        projectDao.updateProject(project)
    }

    suspend fun delete(project: CollageProject) {
        projectDao.deleteProject(project)
    }

    suspend fun markAllAsSynced() {
        projectDao.markAllAsSynced()
    }
}
