package com.appscentric.donot.touch.myphone.antitheft.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.wallpaperResources
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.wallpaperResourcesPremium
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.paging.WallpaperPagingSource

class WallpaperViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {
    private val wallpaperResourcesToUse = if (preferencesManager.isRemoveAdsPurchased()) {
        wallpaperResourcesPremium
    } else {
        wallpaperResources
    }

    val wallpapers = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
        WallpaperPagingSource(wallpaperResourcesToUse, preferencesManager)
    }.flow.cachedIn(viewModelScope)
}
