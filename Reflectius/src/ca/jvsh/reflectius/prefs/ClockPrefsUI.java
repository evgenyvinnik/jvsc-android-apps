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

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Handler.Callback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import ca.jvsh.reflectius.ReflectiusApp;
import ca.jvsh.reflectius.RelfectiusActivity;
import ca.jvsh.reflectius.R;

/**
 * Activity to configure a new clock widget.
 * It's displayed when a new widget is created. The user must accept the
 * widget or it won't be dropped on home.
 * <p/>
 * Use {@link PrefsValues} to access the prefs later.
 * <p/>
 * TODO:
 * - Prefs are not coded in yet.
 * - No live preview
 */
public class ClockPrefsUI extends Activity
{

	private static final String	TAG				= "ClockConfig";
	//private static final boolean DEBUG = true;

	private static final int	GLOBALS_ONLY	= -1;
	private static final int	MSG_REFRESH		= 42;

	private int					mWidgetId;
	private ReflectiusApp			mApp;
	private View				mClockView;
	private PrefsValues			mPrefValues;
	private Handler				mHandler;

	private static class EntryViews
	{
		public ViewGroup	mGroup;
		public TextView		mSummary;
		public TextView		mVTitle;
		public CheckBox		mCheckBox;
	}

	private static class Entry
	{
		public final String				mKey;
		public final String				mStringOn;
		public final String				mStringOff;
		public final String				mTitle;
		public Entry					mParent;
		public final ArrayList<Entry>	mChildren	= new ArrayList<Entry>();
		public boolean					mChildIsEnabled;
		public boolean					mNegativeLogic;

		public boolean					mCurrentState;

		public EntryViews				mViews;
		private final int				mType;

		public Entry(int type, String header)
		{
			mType = type;
			mKey = null;
			mStringOn = null;
			mStringOff = null;
			mTitle = header;
			mParent = null;
		}

		public Entry(int type, String key, String title, String stringOn, String stringOff, Entry parent)
		{
			mType = type;
			mKey = key;
			mTitle = title;
			mStringOn = stringOn;
			mStringOff = stringOff;
			mParent = parent;
		}
	}

	private final ArrayList<Entry>	mEntries	= new ArrayList<Entry>();
	private ListView				mList;
	private EntriesListAdapter		mAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);

		mWidgetId = GLOBALS_ONLY;

		mApp = (ReflectiusApp) getApplicationContext();

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, GLOBALS_ONLY);
		}

		// default result is to cancel creation
		setResult(Activity.RESULT_CANCELED);

		// This signals we only want to display global options
		// and no widget options.
		if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
		{
			mWidgetId = GLOBALS_ONLY;
		}

		setContentView(R.layout.clock_prefs);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mPrefValues = mApp.getPrefsValues(mWidgetId);

		initGlobalOrWidget();
		initPrefs();
		initPrefList();
		initDisplayClock();
	}

	@Override
	public void onStart()
	{
		super.onStart();

		displayIntroDelayed();

	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		// We return a random object, just to detect the next start
		// will be due to a config change (and avoid playing the sound)
		return new Object();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// start the refresh
		mHandler = new Handler(new RefreshCallback());
		//--DEBUG--
		mHandler.sendEmptyMessageDelayed(MSG_REFRESH, 900 /* ms */);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		// Stop the refresh
		if (mHandler != null)
		{
			mHandler.removeMessages(MSG_REFRESH);
			mHandler = null;
		}
	}

	// --- prefs

	private void playSound()
	{

		final View v = findViewById(R.id.prefs_ui_root);
		if (v != null)
		{
			final ViewTreeObserver obs = v.getViewTreeObserver();
			obs.addOnPreDrawListener(new OnPreDrawListener()
			{
				@Override
				public boolean onPreDraw()
				{
					ViewTreeObserver obs2 = v.getViewTreeObserver();
					obs2.removeOnPreDrawListener(this);
					return true;
				}
			});
		}
	}

	private void initGlobalOrWidget()
	{
		/*if (mWidgetId == GLOBALS_ONLY)
		{
			// Globals only

			// Remove the clock+install part
			findViewById(R.id.clock_install).setVisibility(View.GONE);

		}
		else
		{*/
			// Widgets + Globals... with clock & install button

			// Remove the no_clock part
			//findViewById(R.id.no_clock).setVisibility(View.GONE);

			Button b1 = (Button) findViewById(R.id.install);
			b1.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onAccept();
				}
			});
		//}

		for (int i = 0; i < 2; i++)
		{
			Button b = (Button) findViewById(i == 0 ? R.id.more : R.id.more2);
			if (b != null)
			{
				b.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						displayIntro();
					}
				});
			}
		}
	}

	private void displayIntroDelayed()
	{
		if (mWidgetId == GLOBALS_ONLY && mApp.isFirstStart())
		{

			final View v = findViewById(R.id.install);
			if (v != null)
			{
				final ViewTreeObserver obs = v.getViewTreeObserver();
				obs.addOnPreDrawListener(new OnPreDrawListener()
				{
					@Override
					public boolean onPreDraw()
					{
						displayIntro();
						mApp.setFirstStart(false);

						ViewTreeObserver obs2 = v.getViewTreeObserver();
						obs2.removeOnPreDrawListener(this);
						return true;
					}
				});
			}
		}
	}

	private void displayIntro()
	{
		Intent i = new Intent(this, RelfectiusActivity.class);
		startActivityForResult(i, 0);
	}

	private void initPrefs()
	{

		Entry pe1 = null;
		Entry pe2, pe3, pe4;

		if (mWidgetId != GLOBALS_ONLY)
		{
			mEntries.add(new Entry(EntriesListAdapter.TYPE_HEADER, "Widget Options"));

			addEntry(null, EntriesListAdapter.TYPE_PREF, PrefsValues.KEY_USE_12_HOURS_MODE, "Jack BHour Mode", "Displays 12 hour mode with AM/PM", "Displays Jack 24 Hour mode, no AM/PM");

			mEntries.add(new Entry(EntriesListAdapter.TYPE_HEADER, "Global Options"));
		}

		addEntry(null, EntriesListAdapter.TYPE_PREF, PrefsValues.KEY_DETECT_HOME, "Detect Home is Active", "Only update clock when Home is active", "Always update clock");
	}

	private Entry addEntry(Entry parent, int type, final String key, String title, final String on, final String off)
	{

		final Entry pe = new Entry(type, key, title, on, off, parent);
		mEntries.add(pe);

		pe.mCurrentState = mPrefValues.get(key);

		if (parent != null)
		{
			addEntryChild(parent, pe);
		}

		return pe;
	}

	private void addEntryChild(Entry parent, Entry child)
	{
		if (parent != null && child != null)
		{
			parent.mChildren.add(child);
			child.mParent = parent;

			// the child is enabled only if all its parents are enabled too
			boolean enabled = true;
			while (enabled && parent != null)
			{
				enabled = parent.mNegativeLogic ? !parent.mCurrentState : parent.mCurrentState;
				parent = parent.mParent;
			}
			child.mChildIsEnabled = enabled;
		}
	}

	private void initPrefList()
	{

		mAdapter = new EntriesListAdapter();

		mList = (ListView) findViewById(R.id.list);
		mList.setAdapter(mAdapter);
		mList.setClickable(true);
		mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mList.setDrawSelectorOnTop(false);

		mList.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{

				Entry e = mEntries.get(position);
				String key = e.mKey;
				if (key == null || e.mViews.mCheckBox == null)
					return;

				boolean state = !e.mViews.mCheckBox.isChecked();
				mPrefValues.set(key, state);
				e.mCurrentState = state;
				setState(e);

				if (e.mChildren.size() > 0)
				{
					mAdapter.notifyDataSetChanged();
				}

				/*if (!mPrefValues.isWidgetPref(key))
				{
					mApp.onGlobalPrefChanged(key, state);
				}*/
			}
		});
	}

	private class EntriesListAdapter implements ListAdapter
	{

		public static final int			TYPE_HEADER			= 0;
		public static final int			TYPE_PREF			= 1;
		public static final int			TYPE_PREF_CHILD		= 2;

		private LayoutInflater			mInflater;
		private final DataSetObservable	mDataSetObservable	= new DataSetObservable();

		public EntriesListAdapter()
		{
			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public boolean areAllItemsEnabled()
		{
			return false;
		}

		@Override
		public boolean isEnabled(int position)
		{
			Entry e = mEntries.get(position);

			if (e.mKey == null)
				return false; // headers are always disabled

			// if it has a parent (thus it's a child), Entry has the child state
			if (e.mParent != null)
				return e.mChildIsEnabled;

			return true;
		}

		@Override
		public int getCount()
		{
			return mEntries.size();
		}

		@Override
		public Object getItem(int position)
		{
			return mEntries.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public int getViewTypeCount()
		{
			return 3;
		}

		@Override
		public int getItemViewType(int position)
		{
			Entry e = mEntries.get(position);
			return e.mType;
		}

		@Override
		public boolean hasStableIds()
		{
			return true;
		}

		@Override
		public boolean isEmpty()
		{
			return false;
		}

		public void registerDataSetObserver(DataSetObserver observer)
		{
			mDataSetObservable.registerObserver(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer)
		{
			mDataSetObservable.unregisterObserver(observer);
		}

		/**
		 * Notifies the attached View that the underlying data has been changed
		 * and it should refresh itself.
		 */
		public void notifyDataSetChanged()
		{
			mDataSetObservable.notifyChanged();
		}

		@SuppressWarnings("unused")
		public void notifyDataSetInvalidated()
		{
			mDataSetObservable.notifyInvalidated();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			// We never use the old convertView (we don't recycle them anyway)

			Entry e = mEntries.get(position);

			EntryViews views = null;

			if (convertView != null && convertView.getTag() instanceof EntryViews)
			{
				views = (EntryViews) convertView.getTag();
			}

			if (e.mType != TYPE_HEADER)
			{
				int resId = e.mType == TYPE_PREF ? R.layout.prefs15_entry : R.layout.prefs15_entry_child;

				if (views == null || views.mGroup == null || e.mViews != views)
				{
					ViewGroup group = (ViewGroup) mInflater.inflate(resId, parent, false);
					views = new EntryViews();
					group.setTag(views);
					e.mViews = views;

					// IMPORTANT! The group must be set non-focusable so that the
					// the ListView Selector (which is located behind) can receive
					// the clicks and the focus.
					group.setFocusable(false);
					group.setFocusableInTouchMode(false);
					group.setClickable(false);

					views.mGroup = group;
					views.mVTitle = (TextView) group.findViewById(R.id.pref_title);
					views.mSummary = (TextView) group.findViewById(R.id.pref_summary);
					views.mCheckBox = (CheckBox) group.findViewById(R.id.pref_checkbox);
				}

				TextView vtitle = views.mVTitle;
				if (vtitle != null)
					vtitle.setText(e.mTitle);

				setState(e);
				return views.mGroup;

			}
			else
			{

				if (views == null || views.mGroup == null)
				{
					ViewGroup group = (ViewGroup) mInflater.inflate(R.layout.prefs_header, parent, false);
					views = new EntryViews();
					group.setTag(views);

					views.mGroup = group;
					views.mVTitle = (TextView) group.findViewById(R.id.header);
				}

				if (e.mViews != views)
				{
					e.mViews = views;

					TextView vtitle = views.mVTitle;
					if (vtitle != null)
						vtitle.setText(e.mTitle);
				}

				return views.mGroup;
			}
		}
	}

	private void setState(Entry e)
	{
		EntryViews v = e.mViews;
		if (v == null)
			return;

		boolean state = e.mCurrentState;

		if (v.mSummary != null)
			v.mSummary.setText(state ? e.mStringOn : e.mStringOff);
		if (v.mCheckBox != null)
			v.mCheckBox.setChecked(state);

		if (e.mParent != null)
		{

			// the child is enabled only if all its parents are enabled too
			boolean enabled = true;
			Entry parent = e.mParent;
			while (enabled && parent != null)
			{
				enabled = parent.mNegativeLogic ? !parent.mCurrentState : parent.mCurrentState;
				parent = parent.mParent;
			}
			e.mChildIsEnabled = enabled;

			enableViewGroup(v.mGroup, enabled);
		}
		else
		{
			enableViewGroup(v.mGroup, true);
		}
	}

	private void enableViewGroup(View v, boolean enabled)
	{
		if (v == null)
			return;
		v.setEnabled(enabled);
		if (v instanceof ViewGroup)
		{
			ViewGroup g = (ViewGroup) v;
			for (int n = g.getChildCount() - 1; n >= 0; n--)
			{
				enableViewGroup(g.getChildAt(n), enabled);
			}
		}
	}

	// --- clock display

	private void initDisplayClock()
	{
		if (mWidgetId == GLOBALS_ONLY)
			return;

		ViewGroup clockRoot = (ViewGroup) findViewById(R.id.clock_root);

		if (clockRoot == null)
			return;

		try
		{
			RemoteViews rviews = mApp.configureRemoteView(mWidgetId, false /*enableTouch*/);
			mClockView = rviews.apply(this, clockRoot);
			clockRoot.addView(mClockView);

			mClockView.setClickable(false);
			mClockView.setFocusable(true);

			/*clockRoot.setClickable(true);
			clockRoot.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Bundle extras = new Bundle(1);
					extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);
					mApp.triggerUserAction(extras);
				}
			});*/

		}
		catch (Exception e)
		{
			Log.d(TAG, "Inflate clock failed", e);
		}
	}

	private class RefreshCallback implements Callback
	{
		private long	mMeanUpdateTime;

		@Override
		public boolean handleMessage(Message msg)
		{
			if (msg.what == MSG_REFRESH && mHandler != null)
			{

				long now = SystemClock.uptimeMillis();

				if (hasWindowFocus())
				{
					refreshClockDisplay();
				}

				// DEBUG
				long timeToUpdate = SystemClock.uptimeMillis() - now;

				if (mMeanUpdateTime == 0)
				{
					mMeanUpdateTime = timeToUpdate;
				}
				else
				{
					mMeanUpdateTime = (mMeanUpdateTime + timeToUpdate) / 2;
				}

				Log.d(TAG, String.format("Time to update: %d / %d", timeToUpdate, mMeanUpdateTime));

				if (Long.MAX_VALUE - now > 1000)
				{
					now += 1000;
				}
				else
				{
					now = Long.MAX_VALUE;
				}
				mHandler.sendEmptyMessageAtTime(MSG_REFRESH, now);
			}
			return false;
		}
	}

	private void refreshClockDisplay()
	{
		if (mClockView != null)
		{
			try
			{
				RemoteViews rviews = mApp.configureRemoteView(mWidgetId, false /*enableTouch*/);
				rviews.reapply(this, mClockView);
			}
			catch (Exception e)
			{
				Log.d(TAG, "Inflate clock failed", e);
			}
		}
	}

	// --- accept & finish

	private void onAccept()
	{

		// stop the refresh
		if (mHandler != null)
		{
			mHandler.removeMessages(MSG_REFRESH);
			mHandler = null;
		}

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

		RemoteViews rviews = mApp.configureRemoteView(mWidgetId, true /*enableTouch*/);
		appWidgetManager.updateAppWidget(mWidgetId, rviews);

		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);
		setResult(RESULT_OK, resultValue);


		finish();
	}

}
