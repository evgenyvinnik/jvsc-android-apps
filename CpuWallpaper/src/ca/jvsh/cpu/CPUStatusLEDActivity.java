/*
 * Copyright (C) 2008 Google Inc. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ca.jvsh.cpu;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;



/**
 *
 * Main controller activity for CPUStatusLED application.
 *
 * Creates the display (table plus graph view) and connects to the
 * CPUStatusLEDService, starting it if necessary. Since the service will
 * directly update the display when it generates new data, references of the
 * display elements are passed to the service after binding.
 */
public class CPUStatusLEDActivity extends Activity implements OnSeekBarChangeListener /* seekbar */, OnClickListener/* buttons */
{
	private String TAG;
	View chart = null;
	public static boolean disabledLEDs = false;
	CPUStatusChart cpuStatusChart;

	/**
	 * Service connection callback object used to establish communication with
	 * the service after binding to it.
	 */
	private myServiceConnection mConnection;
	 
	/**
	 * Framework method called when the activity is first created.
	 * */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		TAG = this.getResources().getText(R.string.app_name).toString();
		setContentView(R.layout.layout);
		startService(new Intent(this, CPUStatusLEDService.class));
		mConnection = new myServiceConnection(this);
		if (cpuStatusChart == null) cpuStatusChart = new CPUStatusChart();
	}

	/**
	 * Framework method to create menu structure.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Framework method called when activity menu option is selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.disableleds:
				break;
			case R.id.help:
				break;
			case R.id.stop:
				stopService(new Intent(this, CPUStatusLEDService.class));
				mConnection.mService.stopSelf();
				mConnection.mService.stopService(getIntent());
				finish();
				if (mConnection.mService != null) System.exit(RESULT_OK);//service just wont die otherwise!!!
			case R.id.menu_settings:
				LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(R.layout.settings, (ViewGroup) findViewById(R.id.layout_root));
				AlertDialog.Builder builder;
				builder = new AlertDialog.Builder(this);
				builder.setView(layout);
				alertDialog = builder.create();
				alertDialog.show();
				dialogView = layout;
				setListenersAndValues(alertDialog);
				break;
		}
		return true;
	}

	private View dialogView;
	private AlertDialog alertDialog;

	private void setListenersAndValues(AlertDialog alertDialog)
	{
		Button ok = (Button) (dialogView.findViewById(R.id.buttonok));
		ok.setOnClickListener(this);

		Button cancel = (Button) (dialogView.findViewById(R.id.buttoncancel));
		cancel.setOnClickListener(this);

		//row1
		SeekBar t1 = (SeekBar) dialogView.findViewById(R.id.seekbar1);
		t1.setOnSeekBarChangeListener(this);
		t1.setMax(100);
		t1.setKeyProgressIncrement(1);


		TextView tv1 = (TextView) dialogView.findViewById(R.id.threshold1);
		tv1.setText("" + t1.getProgress());

		//row2
		SeekBar t2 = (SeekBar) dialogView.findViewById(R.id.seekbar2);
		t2.setOnSeekBarChangeListener(this);
		t2.setMax(100);
		t2.setKeyProgressIncrement(1);
	
		TextView tv2 = (TextView) dialogView.findViewById(R.id.threshold2);
		tv2.setText("" + t2.getProgress());

		//row3
		SeekBar t3 = (SeekBar) dialogView.findViewById(R.id.seekbar3);
		t3.setOnSeekBarChangeListener(this);
		t3.setMax(100);
		t3.setKeyProgressIncrement(1);


		TextView tv3 = (TextView) dialogView.findViewById(R.id.threshold3);
		tv3.setText("" + t3.getProgress());

		//row4
		SeekBar t4 = (SeekBar) dialogView.findViewById(R.id.seekbar4);
		t4.setOnSeekBarChangeListener(this);
		t4.setMax(100);
		t4.setKeyProgressIncrement(1);


		TextView tv4 = (TextView) dialogView.findViewById(R.id.threshold4);
		tv4.setText("" + t4.getProgress());
	}

	/**
	 * Framework method called when activity becomes the foreground activity.
	 *
	 * onResume/onPause implement the most narrow window of activity life-cycle
	 * during which the activity is in focus and foreground.
	 */
	@Override
	public void onResume()
	{
		super.onResume();
		bindService(new Intent(this, CPUStatusLEDService.class), mConnection, Context.BIND_AUTO_CREATE);

	}

	/**
	 * Framework method called when activity looses foreground position
	 */
	@Override
	public void onPause()
	{
		super.onPause();
		unbindService(mConnection);
	}

	public void setCPUValues(String value)
	{
		TextView tv = (TextView) this.findViewById(R.id.widget31);
		tv.setText(value);
	}

	public void updateGraph(ArrayList<Integer> userHistory, ArrayList<Integer> systemHistory, ArrayList<Integer> signalHistory, ArrayList<String> topProcesses)
	{
		if (topProcesses != null && topProcesses.size() >= 3)
		{
			((TextView) this.findViewById(R.id.top_process)).setText(topProcesses.get(0) + "  " + topProcesses.get(1) + "  " + topProcesses.get(2));
		}
		else
		{
			((TextView) this.findViewById(R.id.top_process)).setText("");
		}

		chart = cpuStatusChart.createView(this, userHistory, systemHistory, signalHistory);
		chart.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		ScrollView sv = (ScrollView) this.findViewById(R.id.scrollview);
		sv.removeAllViews();
		sv.addView(chart);
	}

	public void showExitContinueAlert(String msg)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg).setCancelable(false).setPositiveButton("Exit",
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						//mConnection.mService.stopSelf();
						mConnection.mService.stopService(getIntent());
						CPUStatusLEDActivity.this.finish();
						System.exit(0);
					}
				}).setNegativeButton("Continue",
						new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog, int id)
								{
									dialog.cancel();
								}
							});
		builder.create().show();
	}

	public void onProgressChanged(SeekBar view, int progress, boolean fromuser)
	{
		if (!fromuser) return;
		Log.i(TAG, "onProgressChanged");
		if (view.getId() == R.id.seekbar1)
		{
			((TextView) dialogView.findViewById(R.id.threshold1)).setText("" + progress);
			adjustSeekBars();
		}
		if (view.getId() == R.id.seekbar2)
		{
			((TextView) dialogView.findViewById(R.id.threshold2)).setText("" + progress);
			adjustSeekBars();
		}
		if (view.getId() == R.id.seekbar3)
		{
			((TextView) dialogView.findViewById(R.id.threshold3)).setText("" + progress);
			adjustSeekBars();
		}
		if (view.getId() == R.id.seekbar4)
		{
			((TextView) dialogView.findViewById(R.id.threshold4)).setText("" + progress);
			adjustSeekBars();
		}

	}

	private void adjustSeekBars()
	{
		//1->4
		if (((SeekBar) dialogView.findViewById(R.id.seekbar1)).getProgress() > ((SeekBar) dialogView.findViewById(R.id.seekbar2)).getProgress())
		{
			((SeekBar) dialogView.findViewById(R.id.seekbar2)).setProgress((((SeekBar) dialogView.findViewById(R.id.seekbar1)).getProgress() + 1 % 101));
			((TextView) dialogView.findViewById(R.id.threshold2)).setText("" + ((SeekBar) dialogView.findViewById(R.id.seekbar2)).getProgress());
		}
		if (((SeekBar) dialogView.findViewById(R.id.seekbar2)).getProgress() > ((SeekBar) dialogView.findViewById(R.id.seekbar3)).getProgress())
		{
			((SeekBar) dialogView.findViewById(R.id.seekbar3)).setProgress((((SeekBar) dialogView.findViewById(R.id.seekbar2)).getProgress() + 1 % 101));
			((TextView) dialogView.findViewById(R.id.threshold3)).setText("" + ((SeekBar) dialogView.findViewById(R.id.seekbar3)).getProgress());
		}
		if (((SeekBar) dialogView.findViewById(R.id.seekbar3)).getProgress() > ((SeekBar) dialogView.findViewById(R.id.seekbar4)).getProgress())
		{
			((SeekBar) dialogView.findViewById(R.id.seekbar4)).setProgress((((SeekBar) dialogView.findViewById(R.id.seekbar3)).getProgress() + 1 % 101));
			((TextView) dialogView.findViewById(R.id.threshold4)).setText("" + ((SeekBar) dialogView.findViewById(R.id.seekbar4)).getProgress());
		}
	}

	public void onStartTrackingTouch(SeekBar arg0)
	{
	}

	public void onStopTrackingTouch(SeekBar arg0)
	{
	}

	public void onClick(View view)
	{
		//Log.i(TAG,"onClick");
		if (view.getId() == R.id.buttonok)
		{
			savePreferences();
			alertDialog.dismiss();
		}
		if (view.getId() == R.id.buttoncancel)
		{
			alertDialog.dismiss();
		}

	}

	private void savePreferences()
	{
		Toast.makeText(this, "Preferences saved.", Toast.LENGTH_SHORT).show();
		Log.i(TAG, "Saved prefs");
	}

	//needed otherwise the activity/dialog is destroyed on rotate.
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

}
