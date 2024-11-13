package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.databinding.LayoutIntruderSelfieBinding
import com.appscentric.donot.touch.myphone.antitheft.room.IntruderImage
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CapturedImageAdapter(private val images: List<IntruderImage>) :
    RecyclerView.Adapter<CapturedImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: LayoutIntruderSelfieBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = LayoutIntruderSelfieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val capturedImage = images[position]

        // Bind data to views using binding
        Glide.with(holder.binding.imageViewIntruder.context)
            .load(capturedImage.imagePath)
            .into(holder.binding.imageViewIntruder)

        holder.binding.textViewIntruder.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
            Date(capturedImage.capturedAt)
        )
    }

    override fun getItemCount() = images.size
}

