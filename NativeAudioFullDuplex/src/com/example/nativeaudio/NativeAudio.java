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

package com.example.nativeaudio;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class NativeAudio extends Activity
{

	//static final String TAG = "NativeAudio";

	static final int	CLIP_NONE		= 0;

	static final int	CLIP_PLAYBACK	= 4;


	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.main);

		// initialize native audio system

		createEngine();
		createBufferQueueAudioPlayer();


		((Button) findViewById(R.id.record)).setOnClickListener(new OnClickListener()
		{
			boolean	created	= false;

			public void onClick(View view)
			{
				if (!created)
				{
					created = createAudioRecorder();
				}
				if (created)
				{
					startRecording();
				}
			}
		});

		((Button) findViewById(R.id.playback)).setOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				// ignore the return value
				selectClip(CLIP_PLAYBACK, 3);
			}
		});

	}

	/** Called when the activity is about to be destroyed. */
	@Override
	protected void onPause()
	{
		// turn off all audio
		selectClip(CLIP_NONE, 0);
		super.onPause();
	}

	/** Called when the activity is about to be destroyed. */
	@Override
	protected void onDestroy()
	{
		shutdown();
		super.onDestroy();
	}

	/** Native methods, implemented in jni folder */
	public static native void createEngine();

	public static native void createBufferQueueAudioPlayer();

	public static native boolean createAssetAudioPlayer(AssetManager assetManager, String filename);

	// true == PLAYING, false == PAUSED


	public static native boolean selectClip(int which, int count);


	public static native boolean createAudioRecorder();

	public static native void startRecording();

	public static native void shutdown();

	/** Load jni .so on initialization */
	static
	{
		System.loadLibrary("native-audio-jni");
	}

}
