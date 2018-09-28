/*
 * Project: 24ClockWidget
 * Copyright (C) 2009 ralfoide gmail com,
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

package ca.jvsh.reflectius;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ClockService extends Service
{

	private static boolean		DEBUG		= false;
	private static String		TAG			= "ClockService";

	private static final Object	kLock		= new Object();
	private static int[]		sCachedIds	= null;
	private static ClockThread	sThread;

	private static final long	DELAY_MS	= 30 * 1000;

	@Override
	public IBinder onBind(Intent intent)
	{
		if (DEBUG)
			Log.d(TAG, "onBind");
		return null;
	}

	public static void start(Context context, int[] appWidgetIds)
	{
		ClockThread ct;
		synchronized (kLock)
		{
			ct = sThread;
			ClockService.sCachedIds = appWidgetIds;
		}
		if (ct == null)
		{
			Intent i = new Intent(context, ClockService.class);
			context.startService(i);
		}
		else
		{
			ct.wakeUp();
		}
	}

	public static void stop()
	{
		synchronized (kLock)
		{
			if (sThread != null)
			{
				sThread.setCompleted();
				sThread = null;
			}
		}
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		if (DEBUG)
			Log.d(TAG, "onStart, thread=" + ((sThread == null) ? "none" : sThread.toString()));

		synchronized (kLock)
		{
			if (sThread == null)
			{
				sThread = new ClockThread((ReflectiusApp) getApplicationContext());
				sThread.start();
			}
			else
			{
				sThread.wakeUp();
			}
		}
	}

	private static class ClockThread extends BaseThread
	{

		private ReflectiusApp			mApp;
		private AppWidgetManager	mAWMan;
		private int					mNotHomeDelay	= 0;
		private long				mLastMinute;

		public ClockThread(ReflectiusApp app)
		{
			super("ClockThread");
			mApp = app;
			mAWMan = AppWidgetManager.getInstance(mApp);

			mLastMinute = 0;
		}

		@Override
		protected void runIteration()
		{

			long now = System.currentTimeMillis();

			if (!mApp.isScreenOn())
			{
				if (DEBUG)
					Log.d(TAG, "loop, screen on=>off, stopping");
				// stop the thread if the screen is not running
				synchronized (kLock)
				{
					setCompleted();
					sThread = null;
					return;
				}
			}

			long waitNext = 0;
			boolean needUpdate = false;

			if (mApp.currentTaskIsHome())
			{
				mNotHomeDelay = 0;
				mLastMinute = 0;
				needUpdate = true;
				waitNext = 1000;

			}
			else
			{
				// check again in 10 seconds if not in home
				if (mNotHomeDelay < 30)
					mNotHomeDelay += 5;
				if (DEBUG)
					Log.d(TAG, "loop, not home, wait " + Integer.toString(mNotHomeDelay));

				// however we still perform an update every minute
				long minute = now / DELAY_MS;
				long nextMinute = DELAY_MS - (now - (minute * DELAY_MS));
				if (mLastMinute == 0)
				{
					mLastMinute = minute;
				}
				else if (mLastMinute != minute)
				{
					mLastMinute = minute;
					needUpdate = true;
				}

				waitNext = mNotHomeDelay * 1000 /*ms*/;
				if (nextMinute < waitNext)
				{
					if (DEBUG)
						Log.d(TAG, "minute wait: " + Long.toString(nextMinute));
					waitNext = nextMinute;
				}
			}

			// perform an update
			if (needUpdate && mAWMan != null)
			{
				int[] ids;
				synchronized (kLock)
				{
					ids = sCachedIds;
				}
				if (ids == null)
				{
					ids = mAWMan.getAppWidgetIds(new ComponentName(mApp, ClockWidgetReceiver.class));
					if (ids != null)
					{
						synchronized (kLock)
						{
							sCachedIds = ids;
						}
					}
				}
				if (ids != null && ids.length > 0)
				{
					// Update remote view
					mApp.updateRemoteView(mAWMan, ids);

				}
			}

			if (waitNext > 0)
			{
				waitNext -= System.currentTimeMillis() - now;
				if (DEBUG)
					Log.d(TAG, "waitNext: " + Long.toString(waitNext));
				waitFor(waitNext);
			}
		}
	}
}
