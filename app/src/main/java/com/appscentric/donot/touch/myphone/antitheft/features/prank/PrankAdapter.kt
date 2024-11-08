package com.appscentric.donot.touch.myphone.antitheft.features.prank

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.adapter.HomeItemAdapter.OnItemSelectedListener
import com.appscentric.donot.touch.myphone.antitheft.databinding.PrankItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.model.PrankData
import com.bumptech.glide.Glide

class PrankAdapter(
    private val items: List<PrankData>,
    private val listener: OnItemSelectedListener
) :
    RecyclerView.Adapter<PrankAdapter.ItemViewHolder>() {

    interface OnItemSelectedListener {
        fun onItemSelected(item: PrankData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            PrankItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(private val binding: PrankItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PrankData) {
            with(binding) {
                backgroundView.setBackgroundResource(item.backgroundId)

                Glide.with(binding.root.context)
                    .load(item.resourceId)
                    .into(mainImageView)

                buttonView.setBackgroundResource(item.buttonId)

                itemView.setOnClickListener {
                    listener.onItemSelected(item)
                }
            }
        }
    }
}