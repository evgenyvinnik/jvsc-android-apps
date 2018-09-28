package ca.jvsh.reflectius;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class ReflectiusWidgetProvider extends AppWidgetProvider {
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int x : appWidgetIds) {
			((ReflectiusWidgetApp) context.getApplicationContext()).DeleteWidget(x);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i=0; i<appWidgetIds.length; i++)
		{
			((ReflectiusWidgetApp) context.getApplicationContext()).UpdateWidget(appWidgetIds[i]);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if (intent.getAction().startsWith("ca.jvsh.reflectius")) {
			int id = intent.getIntExtra("widgetId", 0);
			((ReflectiusWidgetApp) context.getApplicationContext()).GetView(id).OnClick();
		}
	}
}
