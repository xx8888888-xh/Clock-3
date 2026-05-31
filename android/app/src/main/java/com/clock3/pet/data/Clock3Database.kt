package com.clock3.pet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.clock3.pet.data.dao.AlarmDao
import com.clock3.pet.data.dao.CountdownDao
import com.clock3.pet.data.dao.PetDao
import com.clock3.pet.data.entity.AlarmEntity
import com.clock3.pet.data.entity.CountdownEntity
import com.clock3.pet.data.entity.PetEntity

@Database(
    entities = [
        AlarmEntity::class,
        CountdownEntity::class,
        PetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class Clock3Database : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun countdownDao(): CountdownDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: Clock3Database? = null

        fun getDatabase(context: Context): Clock3Database {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    Clock3Database::class.java,
                    "clock3_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
