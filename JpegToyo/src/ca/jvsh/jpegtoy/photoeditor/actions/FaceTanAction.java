/*
 * Copyright (C) 2010 The Android Open Source Project
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

package ca.jvsh.jpegtoy.photoeditor.actions;

import android.content.Context;
import android.util.AttributeSet;

import ca.jvsh.jpegtoy.photoeditor.filters.FaceTanFilter;

/**
 * An action handling face tanning effect.
 */
public class FaceTanAction extends EffectAction {

    private static final float DEFAULT_SCALE = 0.5f;

    private ScaleSeekBar scalePicker;

    public FaceTanAction(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void doBegin() {
        final FaceTanFilter filter = new FaceTanFilter();

        scalePicker = factory.createScalePicker(EffectToolFactory.ScalePickerType.GENERIC);
        scalePicker.setOnScaleChangeListener(new ScaleSeekBar.OnScaleChangeListener() {

            @Override
            public void onProgressChanged(float progress, boolean fromUser) {
                if (fromUser) {
                    filter.setScale(progress);
                    notifyFilterChanged(filter, true);
                }
            }
        });
        scalePicker.setProgress(DEFAULT_SCALE);

        filter.setScale(DEFAULT_SCALE);
        notifyFilterChanged(filter, true);
    }

    @Override
    public void doEnd() {
        scalePicker.setOnScaleChangeListener(null);
    }
}
