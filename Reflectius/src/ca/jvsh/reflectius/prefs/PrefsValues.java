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

package ca.jvsh.reflectius.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ca.jvsh.reflectius.R;

/**
 * Access to pref values.
 * Some of the prefs (well currently all of them) depend on the current
 * widget ids. Some other prefs (currently none) are global to the app.
 */
public class PrefsValues
{

	// Widget prefs
	public static final String	KEY_USE_12_HOURS_MODE	= "use_12_hours_mode";

	// Global prefs
	public static final String	KEY_DETECT_HOME			= "detect_home";
	public static final String	KEY_DISMISS_INTRO		= "dismiss_intro";

	private SharedPreferences	mWidgetPrefs;
	private SharedPreferences	mGlobalPrefs;
	private final Context		mContext;
	private final int			mWidgetId;

	/**
	 * Creates a {@link PrefsValues} to access widget-specific prefs
	 * as well as global ones.
	 */
	public PrefsValues(Context context, int widgetId)
	{
		mContext = context;
		mWidgetId = widgetId;

		mGlobalPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (widgetId >= 0)
		{
			// This does String.format("clock_%02d", widgetId) with a bit of an optim
			StringBuilder sb = new StringBuilder("clock_00000");
			for (int i = widgetId, n = sb.length() - 1; i > 0; i /= 10, n--)
			{
				sb.setCharAt(n, (char) ('0' + (i % 10)));
			}

			mWidgetPrefs = context.getSharedPreferences(sb.toString(), Context.MODE_PRIVATE);
		}
	}

	public int getWidgetId()
	{
		return mWidgetId;
	}

	public boolean getDefault(String key)
	{
		if (key.equals(KEY_USE_12_HOURS_MODE))
		{
			return Boolean.valueOf(mContext.getString(R.string.default_use_12_hours_mode));
		}

		return key.equals(KEY_DETECT_HOME);
	}

	public boolean getWidget(String key)
	{
		if (mWidgetPrefs != null && mWidgetPrefs.contains(key))
		{
			return mWidgetPrefs.getBoolean(key, false);
		}
		return getDefault(key);
	}

	public boolean getGlobal(String key)
	{
		if (mGlobalPrefs != null && mGlobalPrefs.contains(key))
		{
			return mGlobalPrefs.getBoolean(key, false);
		}
		return getDefault(key);
	}

	public boolean isWidgetPref(String key)
	{
		return key.equals(KEY_USE_12_HOURS_MODE);
	}

	public boolean get(String key)
	{
		if (isWidgetPref(key))
			return getWidget(key);
		return getGlobal(key);
	}

	public boolean set(String key, boolean value)
	{
		if (isWidgetPref(key))
			return setWidget(key, value);
		return setGlobal(key, value);
	}

	public boolean setWidget(String key, boolean value)
	{
		return mWidgetPrefs.edit().putBoolean(key, value).commit();
	}

	public boolean setGlobal(String key, boolean value)
	{
		return mGlobalPrefs.edit().putBoolean(key, value).commit();
	}

	public boolean use12HoursMode()
	{
		return getWidget(KEY_USE_12_HOURS_MODE);
	}

	public boolean detectHome()
	{
		return getGlobal(KEY_DETECT_HOME);
	}

	public void setIntroDismissed(boolean isChecked)
	{
		setGlobal(KEY_DISMISS_INTRO, isChecked);
	}

	public boolean isIntroDismissed()
	{
		return getGlobal(KEY_DISMISS_INTRO);
	}
}
