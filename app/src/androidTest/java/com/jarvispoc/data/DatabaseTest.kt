package com.jarvispoc.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvispoc.data.database.JarvisDatabase
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.data.entity.MemoryEntity
import com.jarvispoc.data.entity.SessionEntity
import com.jarvispoc.data.entity.TaskEntity
import com.jarvispoc.data.repository.AppRepository
import com.jarvispoc.data.repository.MemoryRepository
import com.jarvispoc.data.repository.SessionRepository
import com.jarvispoc.data.repository.TaskRepository
import com.jarvispoc.memory.MemoryCategory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: JarvisDatabase
    private lateinit var memoryRepo: MemoryRepository
    private lateinit var appRepo: AppRepository
    private lateinit var sessionRepo: SessionRepository
    private lateinit var taskRepo: TaskRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java).build()
        memoryRepo = MemoryRepository(db.memoryDao())
        appRepo = AppRepository(db.appDao())
        sessionRepo = SessionRepository(db.sessionDao())
        taskRepo = TaskRepository(db.taskDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadMemory() = runBlocking {
        val memory = MemoryEntity(
            id = UUID.randomUUID().toString(),
            content = "I prefer COD",
            category = MemoryCategory.PREFERENCE,
            source = "Agent",
            appId = null,
            importance = 1,
            confidence = 1.0f,
            expiresAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        memoryRepo.insert(memory)
        val retrieved = memoryRepo.getById(memory.id)
        assertNotNull(retrieved)
        assertEquals(memory.content, retrieved?.content)
    }

    @Test
    fun writeAndReadApp() = runBlocking {
        val app = AppEntity(
            id = "app_1",
            packageName = "com.blinkit",
            displayName = "Blinkit",
            versionName = "1.0",
            versionCode = 1L,
            installed = true,
            enabled = true,
            firstSeenAt = 0L,
            lastSeenAt = 0L,
            updatedAt = 0L
        )
        appRepo.insertApp(app)
        val retrieved = appRepo.getApp("app_1")
        assertNotNull(retrieved)
        assertEquals(app.packageName, retrieved?.packageName)
    }

    @Test
    fun verifyPersistenceAcrossReconstruction() = runBlocking {
        val session = SessionEntity(
            id = "session_1",
            userId = "user_1",
            startedAt = 1000L,
            lastActiveAt = 1000L,
            status = "ACTIVE",
            summary = null,
            activeTaskId = null
        )
        sessionRepo.insert(session)
        
        // Simulate object graph destruction
        val newSessionRepo = SessionRepository(db.sessionDao())
        
        val retrieved = newSessionRepo.getById("session_1")
        assertNotNull(retrieved)
        assertEquals("ACTIVE", retrieved?.status)
    }
}
