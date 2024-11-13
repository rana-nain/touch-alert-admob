package com.appscentric.donot.touch.myphone.antitheft.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [IntruderImage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun intruderImageDao(): IntruderImageDao
}