package com.appscentric.donot.touch.myphone.antitheft.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class HomeItem(
    @StringRes val nameResId: Int,
    @DrawableRes val thumbnail: Int,
    @RawRes val anim: Int,
    @RawRes val sound: Int,
    var position: Int,
    var isSound: Boolean,
    var isRewarded: Boolean
) : Parcelable {
    override fun toString(): String {
        return "HomeItem(nameResId=$nameResId, thumbnail=$thumbnail, anim=$anim, sound=$sound, position=$position, isSound=$isSound, isRewarded=$isRewarded)"
    }
}