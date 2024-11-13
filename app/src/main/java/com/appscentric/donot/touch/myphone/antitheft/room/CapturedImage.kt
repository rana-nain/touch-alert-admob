package com.appscentric.donot.touch.myphone.antitheft.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "intruder_images")
data class IntruderImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "captured_at") val capturedAt: Long
)
