package com.appscentric.donot.touch.myphone.antitheft.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargeFragment
import com.appscentric.donot.touch.myphone.antitheft.features.sounds.SoundsFragment
import com.appscentric.donot.touch.myphone.antitheft.features.wallpaper.WallpaperFragment

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SoundsFragment()
            1 -> WallpaperFragment()
            2 -> ChargeFragment()
            else -> SoundsFragment()
        }
    }

    companion object {
        const val NUM_TABS = 3
    }
}