package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.databinding.LayoutIntruderSelfieBinding
import com.appscentric.donot.touch.myphone.antitheft.features.intruder.dialog.FullScreenImageDialogFragment
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CapturedImageAdapter(private val listener: OnImageClickListener) : ListAdapter<File, CapturedImageAdapter.ImageViewHolder>(DiffCallback()) {

    // ViewHolder with view binding
    class ImageViewHolder(val binding: LayoutIntruderSelfieBinding) : RecyclerView.ViewHolder(binding.root)

    // Define an interface for the click listener
    interface OnImageClickListener {
        fun onImageClick(imagePath: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = LayoutIntruderSelfieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageFile = getItem(position)

        // Load image with Glide
        Glide.with(holder.binding.imageViewIntruder.context)
            .load(imageFile)
            .into(holder.binding.imageViewIntruder)

        // Display formatted date and time
        holder.binding.textViewIntruder.text = formatDateTime(imageFile.lastModified())

        // Set up the click listener to notify the fragment
        holder.binding.imageViewIntruder.setOnClickListener {
            listener.onImageClick(imageFile.path)
        }
    }

    // Utility function to format date and time
    private fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return dateFormat.format(timestamp)
    }

    // Custom DiffUtil to efficiently handle list updates
    class DiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.lastModified() == newItem.lastModified()
        }
    }
}