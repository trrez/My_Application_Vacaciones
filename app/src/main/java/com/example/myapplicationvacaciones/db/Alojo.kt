package com.example.myapplicationvacaciones.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Alojo(
    @PrimaryKey(autoGenerate = true) val id:Int,
    var lugar: String,
    var imagen:String,
    var lat_long: String,
    var orden:Int,
    var costo_alojamiento: Int,
    var costo_traslado: Int,
    var comentarios: String
)
