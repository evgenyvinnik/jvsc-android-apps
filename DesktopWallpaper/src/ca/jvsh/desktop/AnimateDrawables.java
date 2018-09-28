/*
 * Copyright (C) 2008 The Android Open Source Project
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

package ca.jvsh.desktop;

import ca.jvsh.desktop.R;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.animation.*;
import android.os.Bundle;
import android.view.View;

public class AnimateDrawables extends GraphicsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SampleView(this));
    }
    
    private static class SampleView extends View {
        private AnimateDrawable mDrawable;
        private AnimateDrawable mDrawable1;
        public SampleView(Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);
            {
	            Drawable dr = context.getResources().getDrawable(R.drawable.beach);
	            dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
	            
	            Animation an = new TranslateAnimation(0, 100, 0, 200);
	            an.setDuration(5000);
	            an.setRepeatCount(-1);
	            an.initialize(10, 10, 10, 10);
	            
	            mDrawable = new AnimateDrawable(dr, an);
	            an.startNow();
            }
            {
	            Drawable dr1 = context.getResources().getDrawable(R.drawable.icon);
	            dr1.setBounds(0, 0, dr1.getIntrinsicWidth(), dr1.getIntrinsicHeight());
	            
	            Animation an1 = new TranslateAnimation(0, 100, 0,200);
	            an1.setDuration(10000);
	            an1.setRepeatCount(-1);
	            an1.initialize(10, 10, 10, 10);
	            
	            mDrawable1 = new AnimateDrawable(dr1, an1);
	            an1.startNow();
            }
        }
        
        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);

            mDrawable.draw(canvas);
            mDrawable1.draw(canvas);
            invalidate();
        }
    }
}

