/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.adservices.samples.fledge.sampleapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.common.collect.ImmutableList;

import java.util.List;

final class ToggleAdapter extends RecyclerView.Adapter<ToggleAdapter.ViewHolder> {
    private static final String TAG = "OptionAdapter";
    private final ImmutableList<Toggle> mToggles;

    public ToggleAdapter(@NonNull List<Toggle> toggles) {
        mToggles = ImmutableList.copyOf(toggles);
    }

    @NonNull
    @Override
    public ToggleAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_option, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ToggleAdapter.ViewHolder holder, int position) {
        SwitchMaterial switchView = holder.getSwitch();
        Toggle toggle = mToggles.get(position);
        switchView.setOnCheckedChangeListener(
                (buttonView, checked) -> setOptionChecked(buttonView, toggle, checked));
        switchView.setText(toggle.getLabel());
    }

    @Override
    public int getItemCount() {
        return mToggles.size();
    }

    private void setOptionChecked(CompoundButton buttonView, Toggle toggle, boolean checked) {
        Log.v(TAG, String.format("Option %s is checked %s", toggle.getLabel(), checked));
        boolean canToggle = toggle.onSwitchToggle(checked);
        if (canToggle) {
            buttonView.setChecked(checked);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final SwitchMaterial mSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mSwitch = itemView.findViewById(R.id.switch_view);
        }

        public SwitchMaterial getSwitch() {
            return mSwitch;
        }
    }
}
