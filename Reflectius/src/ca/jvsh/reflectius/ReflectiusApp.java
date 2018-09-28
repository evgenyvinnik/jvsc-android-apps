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

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningTaskInfo;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import ca.jvsh.reflectius.prefs.PrefsValues;

public class ReflectiusApp extends Application
{

	private static boolean			DEBUG				= false;
	private static String			TAG					= "ReflectiusApp";

	/**
	 * Bitmaps for the digits: 0-9.
	 * The index {@link #kColonIndex} (10) is the colon.
	 */
	private Bitmap[]				mDigits				= null;
	private static final int		kColonIndex			= 10;
	private static final int		kDigitsViewIds[]	= { R.id.bmp0, R.id.bmp1, R.id.bmp2, R.id.bmp3, R.id.bmp4, R.id.bmp5, R.id.bmp6, R.id.bmp7 };

	private BroadcastReceiver		mStickyReceiver;
	private final SimpleDateFormat	mSimpleDateFormat[]	= new SimpleDateFormat[2];

	private static Object			sLock				= new Object();

	/** Assume screen is on unless told it's not */
	private boolean					mScreenOn			= true;
	/** Media player, playing the current sound (we don't play more than 1 sound at once) */
	private MediaPlayer				mPlayer;
	/** Telephony manager. Could be null. */
	private TelephonyManager		mTelephonyMan;

	/** The last PrefsValues accessed, cached for a given widget id.
	 * This is an obvious optim since most of the time we'll access the same
	 * prefs over and over.
	 */
	private PrefsValues				mLastPrefValues;
	private boolean					mFirstStart			= true;

	@Override
	public void onCreate()
	{
		super.onCreate();

		mTelephonyMan = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		ClockService.start(getApplicationContext(), null);
		registerScreenStateReceiver();
		//initPeriodicAlarm();
	}

	@Override
	public void onTerminate()
	{
		if (mPlayer != null)
		{
			MediaPlayer p;
			synchronized (sLock)
			{
				p = mPlayer;
				if (p != null)
					mPlayer = null;
			}
			if (p != null)
				p.release();
		}

		removeScreenStateReceiver();
		super.onTerminate();
	}

	public boolean isFirstStart()
	{
		return mFirstStart;
	}

	public void setFirstStart(boolean firstStart)
	{
		mFirstStart = firstStart;
	}

	private void registerScreenStateReceiver()
	{

		IntentFilter ioff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		IntentFilter ion = new IntentFilter(Intent.ACTION_SCREEN_ON);

		mStickyReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				String action = intent.getAction();

				if (Intent.ACTION_SCREEN_OFF.equals(action))
				{
					setIsScreenOn(false);
				}
				else
				{
					setIsScreenOn(true);
					// start the service
					ClockService.start(getApplicationContext(), null);
				}
			}
		};

		registerReceiver(mStickyReceiver, ioff);
		registerReceiver(mStickyReceiver, ion);
	}

	private void removeScreenStateReceiver()
	{
		if (mStickyReceiver != null)
		{
			unregisterReceiver(mStickyReceiver);
		}
	}

	/**
	 * Returns all the bitmaps for the digits: 0-9.
	 * The index 10 is the colon.
	 */
	public Bitmap[] getDigitsBmps()
	{
		if (mDigits == null)
		{
			mDigits = prepareDigitsBitmaps();
		}

		return mDigits;
	}

	private Bitmap[] prepareDigitsBitmaps()
	{

		Bitmap bmp = getResBitmap(this, R.drawable.monotextstring_v3);

		// scan the top line to find start & size
		int start[] = new int[12];
		int width[] = new int[12];
		int index = -1;
		int w = bmp.getWidth();
		for (int x = 0; x < w && index < 11; x++)
		{
			int c = bmp.getPixel(x, 0);
			if ((c & 0x0000FF00) < 0x0000AA00)
			{
				// not green: start new pixel.
				if (index >= 0)
				{
					// close last one first
					width[index] = x - start[index];
				}
				start[++index] = x;
			}
		}

		// now split them
		assert index == 10;
		Bitmap[] dest = new Bitmap[index];

		int h = bmp.getHeight();

		for (int i = 0; i < index; i++)
		{
			dest[i] = Bitmap.createBitmap(bmp, start[i], 1, // x y
					width[i], h - 1); // w h
		}

		return dest;
	}

	/**
	 * Utility method to get a Bitmap from a resource id.
	 */
	private Bitmap getResBitmap(Context context, int bmpResId)
	{

		Options opts = new Options();

		opts.inDither = false;

		// The Options.inScaled field is only available starting at API 4
		// so let's use reflection to set it safely.
		try
		{
			Field f = opts.getClass().getField("inScaled");
			try
			{
				f.setAccessible(true);
			}
			catch (SecurityException ignore)
			{
			}
			f.set(opts, Boolean.FALSE);

		}
		catch (Exception ignore)
		{
			if (DEBUG)
				Log.d(TAG, "Options.isScale=false failed", ignore);
		}

		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), bmpResId, opts);

		if (DEBUG)
			Log.d(TAG, bmp == null ? "getResBitmap => NULL" : String.format("getResBitmap => %dx%d", bmp.getWidth(), bmp.getHeight()));

		return bmp;
	}

	public SimpleDateFormat getDateFormat(boolean use24)
	{
		int index = use24 ? 1 : 0;
		if (mSimpleDateFormat[index] == null)
		{
			mSimpleDateFormat[index] = new SimpleDateFormat(use24 ? "HH:mm:ss" // 8 chars
			: "hh:mm:ssa" // 10 chars (8 time + AM/PM)
			);
		}

		return mSimpleDateFormat[index];
	}

	/**
	 * Updates the clock display for the given widget ids
	 */
	public void updateRemoteView(AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		if (appWidgetIds == null || appWidgetManager == null)
			return;

		for (int widgetId : appWidgetIds)
		{
			RemoteViews rviews = configureRemoteView(widgetId, true /*enableTouch*/);
			appWidgetManager.updateAppWidget(new int[] { widgetId }, rviews);
		}
	}

	/**
	 * Returns the PrefsValues, cached for a given widget id.
	 * This is an obvious optim since most of the time we'll access the same
	 * prefs over and over.
	 *
	 * widgetId can be -1 to get any prefs values for global prefs.
	 */
	public synchronized PrefsValues getPrefsValues(int widgetId)
	{
		PrefsValues prefs = mLastPrefValues;

		if (prefs != null && (widgetId == -1 || prefs.getWidgetId() == widgetId))
		{
			return prefs;
		}

		prefs = mLastPrefValues = new PrefsValues(this, widgetId);

		return prefs;
	}

	/**
	 * Configures an existing remote views mapping our clock widget
	 * {@link R.layout#clock_widget} with the current state of that
	 * widget id.
	 *
	 * TODO this calls for some serious optimizations, e.g. add flags to
	 * only update what changed in the views (time, am/pm, intent, etc.)
	 * and avoid the expensive date format.
	 */
	public RemoteViews configureRemoteView(int widgetId, boolean enableTouch)
	{

		PrefsValues prefs = getPrefsValues(widgetId);

		RemoteViews rviews = new RemoteViews(getPackageName(), R.layout.clock_widget);

		boolean use24 = !prefs.use12HoursMode();

		// format time (TODO too heavy, need to replace by something lightweight)
		SimpleDateFormat sdf = getDateFormat(use24); // 8 or 10 chars
		Calendar now = Calendar.getInstance();
		sdf.setCalendar(now);
		char[] digits = sdf.format(now.getTime()).toCharArray();

		// get bitmaps from main app
		Bitmap[] digitsBmps = getDigitsBmps();

		for (int i = 0; i < 8; i++)
		{
			char c = digits[i];
			if (c >= '0' && c <= '9')
			{
				c -= '0';
				rviews.setImageViewBitmap(kDigitsViewIds[i], digitsBmps[c]);
			}
			else
			{
				rviews.setImageViewBitmap(kDigitsViewIds[i], digitsBmps[ReflectiusApp.kColonIndex]);
			}
		}

		if (use24)
		{
			rviews.setViewVisibility(R.id.am_pm_bmp, View.INVISIBLE);
		}
		else
		{
			rviews.setViewVisibility(R.id.am_pm_bmp, View.VISIBLE);

			boolean isAM = digits.length > 8 && digits[8] == 'A';

			rviews.setImageViewResource(R.id.am_pm_bmp, isAM ? R.drawable.am : R.drawable.pm);
		}


		return rviews;
	}

	/**
	 * Returns true if phone state is in "idle" mode -- not ringing, not in-call.
	 */
	public boolean isCallStateIdle()
	{
		if (mTelephonyMan != null)
		{
			return mTelephonyMan.getCallState() == TelephonyManager.CALL_STATE_IDLE;
		}
		return true;
	}

	public boolean currentTaskIsHome()
	{

		// get a pref values suitable for global prefs to check if we should
		// detect home
		PrefsValues pv = getPrefsValues(-1);
		if (!pv.detectHome())
		{
			return true;
		}

		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

		List<RunningTaskInfo> runningTasks = am.getRunningTasks(2);
		for (RunningTaskInfo t : runningTasks)
		{
			if (t != null && t.numRunning > 0)
			{
				ComponentName cn = t.baseActivity;
				if (cn == null)
					continue;

				String clz = cn.getClassName();
				// Workaround: this is a phantom activity that stays on the
				// top because it is killed in a weird way.
				if ("ca.jvsh.timeriffic.utils.ChangeBrightnessActivity".equals(clz))
				{
					continue;
				}

				String pkg = cn.getPackageName();

				// TODO make this configurable
				if (pkg != null && pkg.startsWith("com.android.launcher"))
				{
					return true;
				}

				return false;
			}
		}

		return false;
	}

	public void setIsScreenOn(boolean isScreenOn)
	{
		mScreenOn = isScreenOn;
	}

	public boolean isScreenOn()
	{
		return mScreenOn;
	}


}
