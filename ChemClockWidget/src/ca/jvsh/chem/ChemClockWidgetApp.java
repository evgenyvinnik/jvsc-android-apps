package ca.jvsh.chem;

import java.util.Hashtable;
import java.util.List;

import android.app.ActivityManager;
import android.app.Application;
import android.app.ActivityManager.RunningTaskInfo;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ChemClockWidgetApp extends Application
{
	private static ChemClockWidgetApp					self;

	private BroadcastReceiver							mStickyReceiver;

	/** Assume screen is on unless told it's not */
	private boolean										mScreenOn	= true;

	private boolean										mFirstStart	= true;

	private static Hashtable<Integer, ChemClockView>	views		= new Hashtable<Integer, ChemClockView>();

	@Override
	public void onCreate()
	{
		super.onCreate();

		self = this;

		ClockService.start(getApplicationContext(), null);
		registerScreenStateReceiver();
	}

	@Override
	public void onTerminate()
	{
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
	* Updates the clock display for the given widget ids
	*/
	public void updateRemoteView(AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		if (appWidgetIds == null || appWidgetManager == null)
			return;

		for (int i = 0; i < appWidgetIds.length; i++)
		{

			if (!views.containsKey(appWidgetIds[i]))
			{
				views.put(appWidgetIds[i], new ChemClockView(this, appWidgetIds[i]));
			}
			views.get(appWidgetIds[i]).Redraw(appWidgetManager);
		}
	}

	public void DeleteWidget(int widgetId)
	{
		if (views.containsKey(widgetId))
		{
			views.remove(widgetId);
		}
	}

	public boolean currentTaskIsHome()
	{

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
				if ("ca.jvsh.lightcycle.ChangeBrightnessActivity".equals(clz))
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

	public static Context getApplication()
	{
		return self;
	}

}
