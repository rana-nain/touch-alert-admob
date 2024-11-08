package com.appscentric.donot.touch.myphone.antitheft.model

import android.os.Parcelable
import androidx.annotation.RawRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class Wallpaper(@RawRes val resourceId: Int, var isRewarded: Boolean) : Parcelable
