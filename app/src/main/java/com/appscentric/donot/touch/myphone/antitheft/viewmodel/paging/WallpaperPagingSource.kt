package com.appscentric.donot.touch.myphone.antitheft.viewmodel.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.model.Wallpaper
import com.appscentric.donot.touch.myphone.antitheft.model.WallpaperItem

class WallpaperPagingSource(
    private val wallpapers: List<Wallpaper>,
    private val preferencesManager: PreferencesManager
) : PagingSource<Int, WallpaperItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WallpaperItem> {
        val page = params.key ?: 0
        val fromIndex = page * params.loadSize
        val toIndex = minOf(fromIndex + params.loadSize, wallpapers.size)

        // Safeguard against out-of-bound errors
        if (fromIndex >= wallpapers.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = if (page == 0) null else page - 1,
                nextKey = null
            )
        }

        // Store the result of the ad purchase check
        val isRemoveAdsPurchased = preferencesManager.isRemoveAdsPurchased()

        val wallpapersList = mutableListOf<WallpaperItem>()

        wallpapers.subList(fromIndex, toIndex).forEachIndexed { index, wallpaper ->
            val isRewarded = if (!isRemoveAdsPurchased) shouldBeRewarded(index + fromIndex) else false
            wallpapersList.add(WallpaperItem.WallpaperData(wallpaper.copy(isRewarded = false)))
        }

        if (!isRemoveAdsPurchased && page == 0 && wallpapersList.size >= 4) {
            wallpapersList.add(3, WallpaperItem.NativeAd)
        }

        return LoadResult.Page(
            data = wallpapersList,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (toIndex >= wallpapers.size) null else page + 1
        )
    }

    override fun getRefreshKey(state: PagingState<Int, WallpaperItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    private fun shouldBeRewarded(index: Int): Boolean {
        // Set rewarded true for items above the 10th position (index 10)
        return index >= 10
    }
}