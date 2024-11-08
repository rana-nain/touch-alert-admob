package com.appscentric.donot.touch.myphone.antitheft.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.HomeItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem

class HomeSoundsAdapter(
    private val items: List<HomeItem>,
    private val listener: OnSoundSelectedListener
) : RecyclerView.Adapter<HomeSoundsAdapter.ItemViewHolder>() {

    interface OnSoundSelectedListener {
        fun onSoundSelected(item: HomeItem, position: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            HomeItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    fun setSelectedItem(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(private val binding: HomeItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeItem) {
            binding.tvItemName.text = binding.root.context.getString(item.nameResId)
            binding.ivItemImage.setImageResource(item.thumbnail)

            if (position == selectedItemPosition) {
                // Apply highlight
                binding.cardview.setBackgroundResource(R.drawable.selected_item_bg)
            } else {
                // Remove highlight
                binding.cardview.setBackgroundResource(R.drawable.gray_rounded_bg)
            }

            // Set click listener to trigger onItemSelected when an item is clicked
            binding.ivItemImage.setOnClickListener {
                listener.onSoundSelected(item, adapterPosition)
            }
        }
    }
}