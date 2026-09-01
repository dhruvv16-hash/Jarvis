package com.jarvispoc.data.manager

import com.jarvispoc.data.entity.SessionEntity
import com.jarvispoc.data.repository.SessionRepository
import java.util.UUID

class SessionManager(private val repository: SessionRepository) {
    suspend fun createSession(userId: String): SessionEntity {
        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            startedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            status = "ACTIVE",
            summary = null,
            activeTaskId = null
        )
        repository.insert(session)
        return session
    }

    suspend fun getActiveSession(): SessionEntity? {
        return repository.getActiveSession()
    }

    suspend fun closeSession(sessionId: String) {
        val session = repository.getById(sessionId)
        if (session != null) {
            repository.insert(session.copy(status = "COMPLETED", lastActiveAt = System.currentTimeMillis()))
        }
    }
}
