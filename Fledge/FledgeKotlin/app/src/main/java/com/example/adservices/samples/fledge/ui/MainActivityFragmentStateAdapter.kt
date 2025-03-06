package com.example.adservices.samples.fledge.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainActivityFragmentStateAdapter(fragmentActivity: FragmentActivity) :
  FragmentStateAdapter(fragmentActivity) {
  override fun getItemCount(): Int {
    return Tab.mainActivityTabList.size
  }

  override fun createFragment(position: Int): Fragment {
    return Tab.mainActivityTabList[position].fragment
  }
}