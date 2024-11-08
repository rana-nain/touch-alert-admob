package com.appscentric.donot.touch.myphone.antitheft.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.DetailsItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.bumptech.glide.Glide

class ItemDetailsAdapter(
    private var items: List<HomeItem>,
    private val listener: OnItemSelectedListener
) : RecyclerView.Adapter<ItemDetailsAdapter.ItemViewHolder>() {

    interface OnItemSelectedListener {
        fun onItemSelected(item: HomeItem, adapterPosition: Int)
    }

    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = DetailsItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun setSelectedItem(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    fun updateItems(newItems: List<HomeItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(private val binding: DetailsItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeItem, position: Int) {
            binding.apply {
                ivItemImage.setImageResource(item.thumbnail)
                // Toggle visibility based on item.isSound property
                adTextView.visibility = if (item.isRewarded) View.VISIBLE else View.GONE

                // Highlight selected item
                frameLayout.setBackgroundResource(
                    if (adapterPosition == selectedItemPosition)
                        R.drawable.selected_item_bg
                    else
                        R.drawable.gray_rounded_bg
                )

                ivItemImage.setOnClickListener {
                    // Update selected item position and notify adapter of the change
                    selectedItemPosition = adapterPosition
                    notifyDataSetChanged()
                    listener.onItemSelected(item,adapterPosition)
                }
            }
        }
    }
}