package com.appscentric.donot.touch.myphone.antitheft.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

class Extension {
    companion object{
        fun String.colorizePart(substring: String, color: Int): SpannableString {
            val spannableString = SpannableString(this)
            val start = indexOf(substring)
            if (start >= 0) {
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    start, start + substring.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannableString
        }
    }
}