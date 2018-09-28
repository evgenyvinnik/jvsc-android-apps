package ca.jvsh.clockclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ClockClockPreferences extends Activity
{
	private Context	self	= this;
	private int		appWidgetId;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// get the appWidgetId of the appWidget being configured
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
		appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

		// set the result for cancel first
		// if the user cancels, then the appWidget
		// should not appear
		Intent cancelResultValue = new Intent();
		cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_CANCELED, cancelResultValue);
		// show the user interface of configuration
		setContentView(R.layout.configuration);

		final SharedPreferences prefs = self.getSharedPreferences("prefs", 0);
		final ColorPickerDialog d = new ColorPickerDialog(self, prefs.getInt("color" + appWidgetId, 0xFFFF0000));

		Button colorpicker = (Button) findViewById(R.id.colorpickerbutton);
		colorpicker.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{


				d.setAlphaSliderVisible(true);

				d.setButton("Ok", new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{

					}
				});

				d.setButton2("Cancel", new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{

					}
				});

				d.show();
			}
		});

		// the OK button
		Button okbutton = (Button) findViewById(R.id.okbutton);
		okbutton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				
				SharedPreferences.Editor edit = prefs.edit();
				edit.putInt("color" + appWidgetId, d.getColor());
				edit.commit();
				
				// change the result to OK
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				setResult(RESULT_OK, resultValue);
				// finish closes activity
				// and sends the OK result
				// the widget will be be placed on the home screen
				finish();
			}
		});

		// cancel button
		Button cancelbutton = (Button) findViewById(R.id.cancelbutton);
		cancelbutton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				// finish sends the already configured cancel result
				// and closes activity
				finish();
			}
		});
	}
}
