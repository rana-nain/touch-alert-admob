package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.os.Environment
import androidx.lifecycle.ViewModel
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.STORAGE_FOLDER_NAME
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class ImageViewModel : ViewModel() {

    private val _images = MutableStateFlow<List<File>>(emptyList())
    val images: StateFlow<List<File>> get() = _images

    init {
        loadImages()
    }

    // Load images from the specified folder
    fun loadImages() {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appFolder = File(downloadsFolder, STORAGE_FOLDER_NAME)
        if (appFolder.exists()) {
            val imageFiles = appFolder.listFiles { file -> file.extension == "jpg" }?.toList() ?: emptyList()
            _images.value = imageFiles
        } else {
            _images.value = emptyList()
        }
    }

    // Add new image to the list and notify observers
    fun addImage(newImage: File) {
        val updatedList = _images.value.toMutableList().apply {
            add(0, newImage)  // Add the new image at the top (or at the end if preferred)
        }
        _images.value = updatedList  // Trigger StateFlow update
    }

    // Delete all images in the specified folder and refresh the image list
    fun clearImages() {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appFolder = File(downloadsFolder, STORAGE_FOLDER_NAME)

        // Check if folder exists and delete each file
        appFolder.listFiles()?.forEach { file ->
            if (file.extension == "jpg") {
                file.delete()
            }
        }

        // Reload images to refresh the list
        loadImages()
    }
}