package com.appscentric.donot.touch.myphone.antitheft.model

data class Language(
    val name: String,
    val flag: Int,
    val code: String,
    var isSelected: Boolean = false // Add isSelected property
)