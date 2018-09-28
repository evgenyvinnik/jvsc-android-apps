package ca.jvsh.reflectius;


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
import android.widget.RadioButton;

public class ReflectiusPreferences extends Activity
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

		Button colorpicker = (Button) findViewById(R.id.colorpickerbutton);
		colorpicker.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				final SharedPreferences prefs = self.getSharedPreferences("prefs", 0);

				final ColorPickerDialog d = new ColorPickerDialog(self, prefs.getInt("color"+ appWidgetId, 0xffff0000) );
				d.setAlphaSliderVisible(true);

				d.setButton("Ok", new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{

						SharedPreferences.Editor editor = prefs.edit();
						editor.putInt("color"+ appWidgetId, d.getColor());
						editor.commit();

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
				// get the date from DatePicker
				RadioButton radioMonthDay = (RadioButton) findViewById(R.id.radioMonthDay);
				RadioButton radioHourMinute = (RadioButton) findViewById(R.id.radioHourMinute);
				RadioButton radioMinuteSecond = (RadioButton) findViewById(R.id.radioMinuteSecond);

				int timeformat = 2;
				if (radioMonthDay.isChecked())
					timeformat = 0;
				if (radioHourMinute.isChecked())
					timeformat = 1;
				if (radioMinuteSecond.isChecked())
					timeformat = 2;

				// save the time format in SharedPreferences
				// we can only store simple types only like long
				// if multiple widget instances are placed
				// each can have own goal date
				// so store it under a name that contains appWidgetId
				SharedPreferences prefs = self.getSharedPreferences("prefs", 0);
				SharedPreferences.Editor edit = prefs.edit();
				edit.putInt("timeformat" + appWidgetId, timeformat);
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
