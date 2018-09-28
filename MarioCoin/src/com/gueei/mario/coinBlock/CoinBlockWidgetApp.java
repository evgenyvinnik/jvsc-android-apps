package com.gueei.mario.coinBlock;

import java.util.Hashtable;

import com.gueei.mario.coinBlock.view.CoinBlockView;

import android.os.Handler;
import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;

public class CoinBlockWidgetApp extends Application {
	private static CoinBlockWidgetApp self;
	private static Hashtable<Integer, CoinBlockView> views = new Hashtable<Integer, CoinBlockView>();
	private static DisplayMetrics metrics;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		self = this;
		Log.d("coinBlock", "Application create");
		WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
		metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		UpdateAllWidgets();
	}

	public void UpdateAllWidgets() {
		AppWidgetManager man = AppWidgetManager.getInstance(getApplication());
		views.clear();
		int[] ids = man.getAppWidgetIds(new ComponentName(getApplication(), coinBlockWidgetProvider.class));
		for (int x : ids) {
			UpdateWidget(x);
		}
	}

	public void UpdateWidget(int widgetId) {
		if (!views.containsKey(widgetId)) {
			CoinBlockView view = new CoinBlockView(this, widgetId);
			views.put(widgetId, view);
		}
	}

	public void DeleteWidget(int widgetId) {
		if(views.containsKey(widgetId))
		{
			views.remove(widgetId);
		}
	}

	public CoinBlockView GetView(int widgetId) {
		if (!views.containsKey(widgetId)) {
			CoinBlockView view = new CoinBlockView(this, widgetId);
			views.put(widgetId, view);
		}
		return views.get(widgetId);
	}

	public static Context getApplication() {
		return self;
	}

	public static DisplayMetrics getMetrics() {
		return metrics;
	}
}
