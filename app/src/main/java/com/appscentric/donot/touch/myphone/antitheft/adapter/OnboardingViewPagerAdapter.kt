package com.appscentric.donot.touch.myphone.antitheft.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.dialog.OnboardingFragment

class OnboardingViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val context: Context
) : FragmentStateAdapter(fragmentActivity) {

    private val slides = listOf(
        OnboardingSlide(R.string.onboarding_slide4_title, R.string.onboarding_slide4_desc, R.drawable.boarding_four),
        OnboardingSlide(R.string.onboarding_slide3_title, R.string.onboarding_slide3_desc, R.drawable.bording_two),
        OnboardingSlide(R.string.onboarding_slide2_title, R.string.onboarding_slide2_desc, R.drawable.boarding_three),
        OnboardingSlide(R.string.onboarding_slide1_title, R.string.onboarding_slide1_desc, R.drawable.bording_one),
        OnboardingSlide(R.string.onboarding_slide5_title, R.string.onboarding_slide5_desc, R.drawable.bording_five),
//        OnboardingSlide(R.string.onboarding_slide6_title, R.string.onboarding_slide6_desc, R.drawable.bording_six),
        OnboardingSlide(R.string.onboarding_slide7_title, R.string.onboarding_slide7_desc, R.drawable.bording_seven)
    )

    override fun createFragment(position: Int): Fragment {
        val slide = slides[position]
        return OnboardingFragment.newInstance(
            context.resources.getString(slide.titleResId),
            context.resources.getString(slide.descResId),
            slide.imageResId
        )
    }

    override fun getItemCount(): Int {
        return slides.size
    }
}

data class OnboardingSlide(
    val titleResId: Int,
    val descResId: Int,
    val imageResId: Int
)