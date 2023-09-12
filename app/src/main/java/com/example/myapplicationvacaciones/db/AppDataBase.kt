package com.example.myapplicationvacaciones.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Alojo::class], version = 3)
abstract class AppDataBase: RoomDatabase(){

    abstract fun alojoDao(): AlojoDao

    companion object {
        // Volatile asegura que sea actualizada la propiedad
        // atómicamente
        @Volatile
        private var BASE_DATOS: AppDataBase? = null
        fun getInstance(contexto: Context): AppDataBase {
            return BASE_DATOS ?: synchronized(this) {
                Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDataBase::class.java,
                    "alojo.bd"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { BASE_DATOS = it }
            }
        }
    }
}
