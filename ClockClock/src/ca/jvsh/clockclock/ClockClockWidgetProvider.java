package ca.jvsh.clockclock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class ClockClockWidgetProvider extends AppWidgetProvider
{

	public ClockClockWidgetProvider()
	{
		// Nothing here.
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		//our widget does not have any specific actions
		super.onReceive(context, intent);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		restartService(context);
	}

	private void restartService(Context context)
	{
		// appWidgetIds contains the ids of the *new* updates. We need all
		// widgets, even existing ones.
		int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, this.getClass()));
		// Nothing to update? We shouldn't be here.
		if (appWidgetIds == null || appWidgetIds.length == 0)
		{
			ClockService.stop();
			return;
		}
		ClockService.start(context, appWidgetIds);
	}

	@Override
	public void onEnabled(Context context)
	{
	}

	@Override
	public void onDisabled(Context context)
	{
		ClockService.stop();
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds)
	{
		super.onDeleted(context, appWidgetIds);
		for (int x : appWidgetIds)
		{
			((ClockClockWidgetApp) context.getApplicationContext()).DeleteWidget(x);
		}

		restartService(context);
	}

}
