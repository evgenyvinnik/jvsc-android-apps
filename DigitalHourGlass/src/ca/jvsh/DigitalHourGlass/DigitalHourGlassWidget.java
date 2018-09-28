package ca.jvsh.DigitalHourGlass;

import android.app.Activity;
import android.app.PendingIntent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

public class DigitalHourGlassWidget extends AppWidgetProvider
{

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds)
	{

	}

	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action))
		{

			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget);

			Intent AlarmClockIntent = new Intent(Intent.ACTION_MAIN)
					.addCategory(Intent.CATEGORY_LAUNCHER).setComponent(
							new ComponentName("com.android.alarmclock",
									"com.android.alarmclock.AlarmClock"));
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
					AlarmClockIntent, 0);
			views.setOnClickPendingIntent(R.id.ViewMain, pendingIntent);

			AppWidgetManager
					.getInstance(context)
					.updateAppWidget(
							intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS),
							views);
		}
	}
}