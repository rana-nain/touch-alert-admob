package com.appscentric.donot.touch.myphone.antitheft.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IntruderImageDao {
    @Insert
    suspend fun insertImage(image: IntruderImage)

    @Query("SELECT * FROM intruder_images ORDER BY captured_at DESC")
    suspend fun getAllImages(): List<IntruderImage>
}


