package com.appscentric.donot.touch.myphone.antitheft.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.databinding.LanguageItemViewBinding
import com.appscentric.donot.touch.myphone.antitheft.model.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val listener: OnLanguageSelectedListener
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    interface OnLanguageSelectedListener {
        fun onLanguageSelected(language: Language)
    }

    private var lastCheckedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = LanguageItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    inner class LanguageViewHolder(private val binding: LanguageItemViewBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.radioButton.setOnClickListener { handleItemClick() }
            binding.root.setOnClickListener { handleItemClick() }
        }

        private fun handleItemClick() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION && position != lastCheckedPosition) {
                lastCheckedPosition = position
                listener.onLanguageSelected(languages[position])
                notifyDataSetChanged()
            }
        }

        fun bind(language: Language) {
            binding.languageName.text = language.name
            binding.languageFlag.setImageResource(language.flag)
            binding.radioButton.isChecked = language.isSelected
        }
    }

}