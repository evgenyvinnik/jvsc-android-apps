package ca.jvsh.chem;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.text.format.Time;
import android.widget.RemoteViews;

public class ChemClockView
{

	private int	mWidgetId;

	int[]		mOldDigits		= new int[2];
	int[]		mCurrentDigits	= new int[2];

	Time		mCurrentTime	= new Time();

	boolean		mNeedRedraw		= false;

	public ChemClockView(Context context, int widgetId)
	{

		mWidgetId = widgetId;
	}

	public Context getContext()
	{
		return (ChemClockWidgetApp.getApplication());
	}

	public void Redraw(AppWidgetManager appWidgetManager)
	{

		RemoteViews rviews = new RemoteViews(getContext().getPackageName(), R.layout.chemclock_widget);

		mCurrentTime.setToNow();

		mCurrentDigits[0] = mCurrentTime.hour;
		mCurrentDigits[1] = mCurrentTime.minute;

		if (mCurrentDigits[0] != mOldDigits[0] || mCurrentDigits[1] != mOldDigits[1])
		{

			rviews.setImageViewResource(R.id.hour, R.drawable.element00 + mCurrentDigits[0]);
			rviews.setImageViewResource(R.id.minute, R.drawable.element00 + mCurrentDigits[1]);

			mOldDigits[0] = mCurrentDigits[0];
			mOldDigits[1] = mCurrentDigits[1];
		}

		appWidgetManager.updateAppWidget(mWidgetId, rviews);
	}

}
