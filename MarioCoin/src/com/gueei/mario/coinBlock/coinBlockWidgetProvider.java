package com.gueei.mario.coinBlock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class coinBlockWidgetProvider extends AppWidgetProvider {
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int x : appWidgetIds) {
			((CoinBlockWidgetApp) context.getApplicationContext()).DeleteWidget(x);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i=0; i<appWidgetIds.length; i++)
		{
			((CoinBlockWidgetApp) context.getApplicationContext()).UpdateWidget(appWidgetIds[i]);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if (intent.getAction().startsWith("com.gueei")) {
			int id = intent.getIntExtra("widgetId", 0);
			((CoinBlockWidgetApp) context.getApplicationContext()).GetView(id).OnClick();
		}
	}
}
