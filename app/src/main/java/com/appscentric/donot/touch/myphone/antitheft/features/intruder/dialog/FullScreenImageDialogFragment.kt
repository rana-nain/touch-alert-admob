package com.appscentric.donot.touch.myphone.antitheft.features.intruder.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentFullScreenImageDialogBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.io.File

class FullScreenImageDialogFragment : DialogFragment() {

    private var _binding: FragmentFullScreenImageDialogBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.DialogTheme

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"

        // Method to create an instance of the dialog fragment with the image path as an argument
        fun newInstance(imagePath: String): FullScreenImageDialogFragment {
            val fragment = FullScreenImageDialogFragment()
            val args = Bundle().apply {
                putString(ARG_IMAGE_PATH, imagePath)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenImageDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { dismiss() }

        // Retrieve the image path from arguments and load it using Glide
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        imagePath?.let {
            Glide.with(requireContext())
                .load(File(it))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageViewFullScreen)

        }

        // Set up close button listener (ensure correct button ID in XML)
        binding.materialButton2.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
