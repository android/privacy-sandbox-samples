package com.example.adservices.samples.fledge.sampleapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.common.collect.ImmutableList

internal class ToggleAdapter(toggles: List<Toggle>) :
    RecyclerView.Adapter<ToggleAdapter.ViewHolder>() {
    private val mToggles: ImmutableList<Toggle> = ImmutableList.copyOf(toggles)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_option, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val switchView = holder.switch
        val toggle = mToggles[position]
        switchView.setOnCheckedChangeListener { buttonView: CompoundButton, checked: Boolean ->
            setOptionChecked(
                buttonView,
                toggle,
                checked
            )
        }
        switchView.text = toggle.label
    }

    override fun getItemCount(): Int {
        return mToggles.size
    }

    private fun setOptionChecked(buttonView: CompoundButton, toggle: Toggle, checked: Boolean) {
        Log.v(TAG, String.format("Option %s is checked %s", toggle.label, checked))
        val canToggle = toggle.onSwitchToggle(checked)
        if (canToggle) {
            buttonView.isChecked = checked
        }
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val switch: SwitchMaterial = itemView.findViewById(R.id.switch_view)
    }

    companion object {
        private const val TAG = "OptionAdapter"
    }
}
