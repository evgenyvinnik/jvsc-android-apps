/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.jvsh.synarprofiler;

import ca.jvsh.synarprofiler.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class SynarProfiler extends Activity
{
	private static final String		TAG			= "SynarProfiler";
	private SharedPreferences		mSettings;
	//private SynarProfilerSettings	mFallDetectorSettings;

	/**
	 * True, when service is running.
	 */
	private boolean					mIsRunning;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.i(TAG, "[ACTIVITY] onCreate");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
	}

	@Override
	protected void onStart()
	{
		Log.i(TAG, "[ACTIVITY] onStart");
		super.onStart();
	}

	@Override
	protected void onResume()
	{
		Log.i(TAG, "[ACTIVITY] onResume");
		super.onResume();

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		// Read from preferences if the service was running on the last onPause
		mIsRunning = mSettings.getBoolean("service_running", false);

		// Start the service if this is considered to be an application start (last onPause was long ago)
		if (mIsRunning)
		{
			bindFallDetectionService();
		}
		
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putBoolean("service_running", false);
		editor.commit();
	}

	@Override
	protected void onPause()
	{
		Log.i(TAG, "[ACTIVITY] onPause");
		if (mIsRunning)
		{
			unbindFallDetectionService();
		}
		
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putBoolean("service_running", mIsRunning);
		editor.commit();

		super.onPause();
	}

	@Override
	protected void onStop()
	{
		Log.i(TAG, "[ACTIVITY] onStop");
		super.onStop();
	}

	protected void onDestroy()
	{
		Log.i(TAG, "[ACTIVITY] onDestroy");
		super.onDestroy();
	}

	protected void onRestart()
	{
		Log.i(TAG, "[ACTIVITY] onRestart");
		super.onDestroy();
	}

	private SynarProfilerService	mService;

	private ServiceConnection		mConnection	= new ServiceConnection()
												{
													public void onServiceConnected(ComponentName className, IBinder service)
													{
														mService = ((SynarProfilerService.StepBinder) service).getService();
													}

													public void onServiceDisconnected(ComponentName className)
													{
														mService = null;
													}
												};

	private void startFallDetectionService()
	{
		if (!mIsRunning)
		{
			Log.i(TAG, "[SERVICE] Start");
			mIsRunning = true;
			startService(new Intent(SynarProfiler.this,
					SynarProfilerService.class));
		}
	}

	private void bindFallDetectionService()
	{
		Log.i(TAG, "[SERVICE] Bind");
		bindService(new Intent(SynarProfiler.this,
				SynarProfilerService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	}

	private void unbindFallDetectionService()
	{
		Log.i(TAG, "[SERVICE] Unbind");
		unbindService(mConnection);
	}

	private void stopFallDetectionService()
	{
		Log.i(TAG, "[SERVICE] Stop");
		if (mService != null)
		{
			Log.i(TAG, "[SERVICE] stopService");
			stopService(new Intent(SynarProfiler.this,
					SynarProfilerService.class));
		}
		mIsRunning = false;
	}

	
	private static final int	MENU_SETTINGS	= 8;
	private static final int	MENU_QUIT		= 9;

	private static final int	MENU_STOP		= 1;
	private static final int	MENU_START		= 2;

	/* Creates the menu items */
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.clear();
		if (mIsRunning)
		{
			menu.add(0, MENU_STOP, 0, R.string.stop)
					.setIcon(android.R.drawable.ic_media_pause)
					.setShortcut('1', 'p');
		}
		else
		{
			menu.add(0, MENU_START, 0, R.string.start)
					.setIcon(android.R.drawable.ic_media_play)
					.setShortcut('1', 'p');
		}

		menu.add(0, MENU_SETTINGS, 0, R.string.settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setShortcut('8', 's')
				.setIntent(new Intent(this, Settings.class));
		menu.add(0, MENU_QUIT, 0, R.string.quit)
				.setIcon(android.R.drawable.ic_lock_power_off)
				.setShortcut('9', 'q');
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_STOP:
				unbindFallDetectionService();
				stopFallDetectionService();
				return true;
			case MENU_START:
				startFallDetectionService();
				bindFallDetectionService();
				return true;
			case MENU_QUIT:
				unbindFallDetectionService();
				stopFallDetectionService();
				finish();
				return true;
		}
		return false;
	}
}