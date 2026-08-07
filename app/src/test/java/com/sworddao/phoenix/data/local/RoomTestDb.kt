package com.sworddao.phoenix.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

object RoomTestDb {
    fun create(): PhoenixDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, PhoenixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
}
