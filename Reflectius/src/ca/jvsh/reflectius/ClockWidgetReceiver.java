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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class ClockWidgetReceiver extends AppWidgetProvider
{

	/** Name of intent to broadcast to activate this receiver. */
	public static final String	ACTION_USER_CLOCK	= "ca.jvsh.intent.action.ACTION_USER_CLOCK";
	//public static final String	ACTION_HOUR_CHIME	= "ca.jvsh.intent.action.ACTION_HOUR_CHIME";
	public static final String	AUTHORITY			= "ca.jvsh.reflectius";
	public static final Uri		CONTENT_URI			= Uri.parse("content://" + AUTHORITY + "/wid");

	private static boolean		DEBUG				= false;
	private static String		TAG					= "ClockWidgetReceiver";

	private ReflectiusApp			mApp;

	public ClockWidgetReceiver()
	{
		// Nothing here. This is constructed for *each* call.
	}

	private ReflectiusApp getApp(Context context)
	{
		if (mApp == null)
		{
			mApp = (ReflectiusApp) context.getApplicationContext();
		}
		return mApp;
	}

	

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{

		// appWidgetIds contains the ids of the *new* updates. We need all
		// widgets, even existing ones.
		appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, this.getClass()));

		// Nothing to update? We shouldn't be here.
		if (appWidgetIds == null || appWidgetIds.length == 0)
		{
			if (DEBUG)
				Log.d(TAG, "onUpdate , *no* widgets!");
			return;
		}

		if (DEBUG)
			Log.d(TAG, String.format("onUpdate, %d widgets", appWidgetIds.length));

		ClockService.start(context, appWidgetIds);
	}

	@Override
	public void onEnabled(Context context)
	{
		if (DEBUG)
			Log.d(TAG, String.format("onEnabled"));
	}

	@Override
	public void onDisabled(Context context)
	{
		if (DEBUG)
			Log.d(TAG, String.format("onDisabled"));

		ClockService.stop();
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds)
	{
		if (DEBUG)
			Log.d(TAG, String.format("onDeleted"));
		super.onDeleted(context, appWidgetIds);
	}
}
