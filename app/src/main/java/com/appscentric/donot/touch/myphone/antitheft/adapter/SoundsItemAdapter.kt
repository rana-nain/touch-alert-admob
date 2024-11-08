package com.appscentric.donot.touch.myphone.antitheft.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.SoundsDetailsItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem

class SoundsItemAdapter(
    private val items: List<HomeItem>,
    private val listener: OnItemSelectedListener
) : RecyclerView.Adapter<SoundsItemAdapter.ItemViewHolder>() {

    interface OnItemSelectedListener {
        fun onSoundSelected(item: HomeItem)
    }

    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = SoundsDetailsItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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


    inner class ItemViewHolder(private val binding: SoundsDetailsItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeItem, position: Int) {
            binding.ivItemImage.setImageResource(item.thumbnail)

            // Highlight the item if it's selected
            if (position == selectedItemPosition) {
                // Apply highlight
                binding.frameLayout.setBackgroundResource(R.drawable.circular_selected_item_bg)
            } else {
                // Remove highlight
                binding.frameLayout.setBackgroundResource(R.drawable.gray_rounded_bg)
            }

            binding.ivItemImage.setOnClickListener {
                // Update selected item position and notify adapter of the change
                selectedItemPosition = adapterPosition
                notifyDataSetChanged()
                listener.onSoundSelected(item)
            }
        }
    }
}