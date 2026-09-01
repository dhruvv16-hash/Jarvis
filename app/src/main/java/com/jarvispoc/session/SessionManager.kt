package com.jarvispoc.session

interface SessionManager {
    suspend fun getActiveSession(): Session
}
