// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ca.jvsh.flute.util;

import ca.jvsh.flute.util.MiscUtil;
import ca.jvsh.flute.util.WidgetFader;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Processes touch events and scrolls the screen in manual mode.
 *
 * @author John Taylor
 */
public class GestureInterpreter extends GestureDetector.SimpleOnGestureListener
{
	private static final String	TAG	= MiscUtil.getTag(GestureInterpreter.class);
	private WidgetFader[]		faders;

	public GestureInterpreter(WidgetFader[] faders)
	{
		this.faders = faders;
	}

	@Override
	public boolean onDown(MotionEvent e)
	{
		Log.d(TAG, "Tap down");

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		Log.d(TAG, "Flinging " + velocityX + ", " + velocityY);

		return true;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e)
	{
		Log.d(TAG, "Tap up");

		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		Log.d(TAG, "Double tap");
		// Bring up the controls
		for (WidgetFader fader : faders)
		{
			fader.keepActive();
		}
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e)
	{
		Log.d(TAG, "Confirmed single tap");
		return false;
	}

}
