/*
 * Copyright (C) 2009 Teleca Poland Sp. z o.o. <android@teleca.com>
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

package ca.jvsh.translator;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

/**
 * Singleton with hooks to Player and Download Service
 * 
 * @author Lukasz Wisniewski
 */
public class TranslatorApplication extends Application
{

	/**
	 * Tag used for DDMS logging
	 */
	public static String					TAG	= "jamendo";

	/**
	 * Singleton pattern
	 */
	private static TranslatorApplication	instance;

	public static TranslatorApplication getInstance()
	{
		return instance;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;
	}

	/**
	 * Retrieves application's version number from the manifest
	 * 
	 * @return
	 */
	public String getVersion()
	{
		String version = "0.0.0";

		PackageManager packageManager = getPackageManager();
		try
		{
			PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
			version = packageInfo.versionName;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}

		return version;
	}

	/**
	 * Since 0.9.8.7 we embrace "bindless" PlayerService thus this adapter. No
	 * big need of code refactoring, we just wrap sending intents around defined
	 * interface
	 * 
	 * @author Lukasz Wisniewski
	 */
	/*private class IntentPlayerEngine implements PlayerEngine
	{

		@Override
		public Playlist getPlaylist()
		{
			return mPlaylist;
		}

		@Override
		public boolean isPlaying()
		{
			if (mServicePlayerEngine == null)
			{
				// service does not exist thus no playback possible
				return false;
			}
			else
			{
				return mServicePlayerEngine.isPlaying();
			}
		}

		@Override
		public void next()
		{
			if (mServicePlayerEngine != null)
			{
				playlistCheck();
				mServicePlayerEngine.next();
			}
			else
			{
				startAction(PlayerService.ACTION_NEXT);
			}
		}

		@Override
		public void openPlaylist(Playlist playlist)
		{
			mPlaylist = playlist;
			if (mServicePlayerEngine != null)
			{
				mServicePlayerEngine.openPlaylist(playlist);
			}
		}

		@Override
		public void pause()
		{
			if (mServicePlayerEngine != null)
			{
				mServicePlayerEngine.pause();
			}
		}

		@Override
		public void play()
		{
			if (mServicePlayerEngine != null)
			{
				playlistCheck();
				mServicePlayerEngine.play();
			}
			else
			{
				startAction(PlayerService.ACTION_PLAY);
			}
		}

		@Override
		public void prev()
		{
			if (mServicePlayerEngine != null)
			{
				playlistCheck();
				mServicePlayerEngine.prev();
			}
			else
			{
				startAction(PlayerService.ACTION_PREV);
			}
		}

		@Override
		public void setListener(PlayerEngineListener playerEngineListener)
		{
			mPlayerEngineListener = playerEngineListener;
			// we do not want to set this listener if Service
			// is not up and a new listener is null
			if (mServicePlayerEngine != null || mPlayerEngineListener != null)
			{
				startAction(PlayerService.ACTION_BIND_LISTENER);
			}
		}

		@Override
		public void skipTo(int index)
		{
			if (mServicePlayerEngine != null)
			{
				mServicePlayerEngine.skipTo(index);
			}
		}

		@Override
		public void stop()
		{
			startAction(PlayerService.ACTION_STOP);
			// stopService(new Intent(JamendoApplication.this,
			// PlayerService.class));
		}

		private void startAction(String action)
		{
			Intent intent = new Intent(TranslatorApplication.this, PlayerService.class);
			intent.setAction(action);
			startService(intent);
		}

		*//**
		* This is required if Player Service was binded but playlist was not
		* passed from Application to Service and one of buttons: play, next,
		* prev is pressed
		*/
	/*
	private void playlistCheck()
	{
	if (mServicePlayerEngine != null)
	{
		if (mServicePlayerEngine.getPlaylist() == null && mPlaylist != null)
		{
			mServicePlayerEngine.openPlaylist(mPlaylist);
		}
	}
	}

	@Override
	public void setPlaybackMode(PlaylistPlaybackMode aMode)
	{
	mPlaylist.setPlaylistPlaybackMode(aMode);
	}

	@Override
	public PlaylistPlaybackMode getPlaybackMode()
	{
	return mPlaylist.getPlaylistPlaybackMode();
	}

	@Override
	public void forward(int time)
	{
	if (mServicePlayerEngine != null)
	{
		mServicePlayerEngine.forward(time);
	}

	}

	@Override
	public void rewind(int time)
	{
	if (mServicePlayerEngine != null)
	{
		mServicePlayerEngine.rewind(time);
	}

	}

	}*/

}
