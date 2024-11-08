package com.appscentric.donot.touch.myphone.antitheft.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.ItemWallpaperBinding
import com.appscentric.donot.touch.myphone.antitheft.databinding.LargeNativeAdItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.SquareNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.model.Wallpaper
import com.appscentric.donot.touch.myphone.antitheft.model.WallpaperItem
import com.bumptech.glide.Glide

class WallpaperAdapter(
    private val onItemClick: (Wallpaper) -> Unit,
    private val onPlayClickSound: () -> Unit
) : PagingDataAdapter<WallpaperItem, RecyclerView.ViewHolder>(WallpaperDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is WallpaperItem.WallpaperData -> R.layout.item_wallpaper
            is WallpaperItem.NativeAd -> R.layout.gnt_large_template_view
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_wallpaper -> WallpaperViewHolder(
                ItemWallpaperBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            R.layout.gnt_large_template_view -> AdViewHolder(
                LargeNativeAdItemViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is WallpaperItem.WallpaperData -> (holder as WallpaperViewHolder).bind(item.wallpaper)
            is WallpaperItem.NativeAd -> (holder as AdViewHolder).bind()
            null -> TODO()
        }
    }

    inner class WallpaperViewHolder(private val binding: ItemWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(wallpaper: Wallpaper) {

            Glide.with(binding.root.context)
                .load(wallpaper.resourceId)
                .into(binding.imageView)

            binding.adTextView.visibility = if (wallpaper.isRewarded) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onPlayClickSound()
                onItemClick(wallpaper)
            }
        }
    }

    inner class AdViewHolder(private val binding: LargeNativeAdItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // Bind your native ad here
            SquareNativeAdManager.getAd()?.let { ad ->
                binding.templateView.apply {
                    setNativeAd(ad)
                    visibility = View.VISIBLE
                }
            } ?: run {
                binding.templateView.visibility = View.GONE
            }
        }
    }

    fun updateItem(position: Int, updatedWallpaper: Wallpaper) {
        val currentItem = getItem(position) as? WallpaperItem.WallpaperData
        currentItem?.let {
            it.wallpaper = updatedWallpaper
            notifyItemChanged(position)
        }
    }
}

class WallpaperDiffCallback : DiffUtil.ItemCallback<WallpaperItem>() {
    override fun areItemsTheSame(oldItem: WallpaperItem, newItem: WallpaperItem): Boolean {
        return (oldItem is WallpaperItem.WallpaperData && newItem is WallpaperItem.WallpaperData && oldItem.wallpaper.resourceId == newItem.wallpaper.resourceId) ||
                (oldItem is WallpaperItem.NativeAd && newItem is WallpaperItem.NativeAd)
    }

    override fun areContentsTheSame(oldItem: WallpaperItem, newItem: WallpaperItem): Boolean {
        return oldItem == newItem
    }
}