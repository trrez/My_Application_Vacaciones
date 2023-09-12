package com.example.myapplicationvacaciones.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlojoDao {

    @Query("SELECT * FROM alojo ORDER BY orden")
    fun findAll(): List<Alojo>

    @Query("SELECT COUNT(*) FROM alojo")
    fun contar(): Int

    @Insert
    fun insertar(alojo: Alojo):Long

    @Update
    fun actualizar(alojo: Alojo)

    @Delete
    fun eliminar(alojo: Alojo)
}