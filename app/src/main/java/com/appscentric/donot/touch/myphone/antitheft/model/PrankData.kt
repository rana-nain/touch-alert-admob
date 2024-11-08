package com.appscentric.donot.touch.myphone.antitheft.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PrankData(
    val resourceId: Int,
    val backgroundId: Int,
    val buttonId: Int,
    val soundId: Int
) :
    Parcelable