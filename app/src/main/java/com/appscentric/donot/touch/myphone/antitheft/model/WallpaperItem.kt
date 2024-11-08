package com.appscentric.donot.touch.myphone.antitheft.model

sealed class WallpaperItem {
    data class WallpaperData(var wallpaper: Wallpaper) : WallpaperItem()
    data object NativeAd : WallpaperItem()
}