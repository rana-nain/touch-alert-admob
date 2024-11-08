package com.appscentric.donot.touch.myphone.antitheft.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.HomeItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.databinding.NativeAdItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.bumptech.glide.Glide

class HomeItemAdapter(
    private var items: List<HomeItem>,
    private val listener: OnItemSelectedListener,
    private val preferencesManager: PreferencesManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnItemSelectedListener {
        fun onItemSelected(item: HomeItem, position: Int)
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_AD = 1
    }

    private val isAdAvailable = SmallNativeAdManager.getAd() != null

    override fun getItemViewType(position: Int): Int {
        // Show ad only if the user is not premium and the ad is available
        return if (!preferencesManager.isRemoveAdsPurchased() && isAdAvailable && position == 9) {
            VIEW_TYPE_AD
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val binding =
                    HomeItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ItemViewHolder(binding)
            }

            VIEW_TYPE_AD -> {
                val binding = NativeAdItemViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AdViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            // Adjust item position considering the ad placement
            val itemPosition =
                if (!preferencesManager.isRemoveAdsPurchased() && isAdAvailable && position > 9) position - 1 else position
            if (itemPosition in items.indices) {  // Check that the index is within bounds
                holder.bind(items[itemPosition])
            }
        } else if (holder is AdViewHolder) {
            holder.bind()
        }
    }

    override fun getItemCount(): Int {
        // Add one ad after every 9 items if an ad is available and the user is not premium
        return if (!preferencesManager.isRemoveAdsPurchased() && isAdAvailable && items.size > 9) items.size + 1 else items.size
    }

    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    fun setSelectedItem(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(private val binding: HomeItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeItem) {
            binding.apply {
                // Set text and image using item properties
                tvItemName.text = root.context.getString(item.nameResId)

                Glide.with(binding.root.context)
                    .load(item.thumbnail)
                    .into(ivItemImage)

                // Toggle visibility based on item.isSound property
//                adTextView.visibility = if (item.isRewarded) View.VISIBLE else View.GONE

                // Highlight selected item
                cardview.setBackgroundResource(
                    if (adapterPosition == selectedItemPosition)
                        R.drawable.selected_item_bg
                    else
                        R.drawable.gray_rounded_bg
                )

                // Set click listener for the image view
                ivItemImage.setOnClickListener {
                    listener.onItemSelected(item, adapterPosition)
                }
            }
        }
    }

    inner class AdViewHolder(private val binding: NativeAdItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // Bind your native ad here
            SmallNativeAdManager.getAd()?.let { ad ->
                binding.templateView.apply {
                    setNativeAd(ad)
                    visibility = View.VISIBLE
                }
            } ?: run {
                binding.templateView.visibility = View.GONE
            }
        }
    }

    fun updateItems(newItems: List<HomeItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}