package ca.jvsh.lightcycle;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class ClockService extends Service
{
	private static final Object	kLock		= new Object();
	private static int[]		sCachedIds	= null;
	private static ClockThread	sThread;

	private static final long	DELAY_MS	= 30 * 1000;

	@Override
	public IBinder onBind(Intent intent)
	{
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

		synchronized (kLock)
		{
			if (sThread == null)
			{
				sThread = new ClockThread((LightCycleClockWidgetApp) getApplicationContext());
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

		private LightCycleClockWidgetApp	mApp;
		private AppWidgetManager			mAWMan;
		private int							mNotHomeDelay	= 0;
		private long						mLastMinute;

		public ClockThread(LightCycleClockWidgetApp app)
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
				waitNext = 40;

			}
			else
			{
				// check again in 10 seconds if not in home
				if (mNotHomeDelay < 30)
					mNotHomeDelay += 5;

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

				waitNext = mNotHomeDelay * 40 /*ms*/;
				if (nextMinute < waitNext)
				{
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
					ids = mAWMan.getAppWidgetIds(new ComponentName(mApp, LightCycleClockWidgetProvider.class));
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
				waitFor(waitNext);
			}
		}
	}
}
