/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.jvsh.synarprofiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.jvsh.synarprofiler.R;
import ca.jvsh.synarprofiler.utils.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

/**
 * This is an example of implementing an application service that runs locally
 * in the same process as the application.  The {@link StepServiceController}
 * and {@link StepServiceBinding} classes show how to interact with the
 * service.
 *
 * <p>Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service.  This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
public class SynarProfilerService extends Service
{
	private static final String		TAG		= "ca.jvsh.synarprofiler.SynarProfilerService";
	private SharedPreferences		mSettings;

	private int						mSensorRateMilliseconds;
	private int						mFlushDataMilliseconds;
	private int						mSleepMilliseconds;

	private long					mSensorRateStart;
	private long					mSensorRateStop;
	private long					mFlushDataStart;
	private long					mFlushDataStop;

	private PowerManager.WakeLock	wakeLock;
	private NotificationManager		mNM;

	private BufferedWriter			mOutput;

	private boolean					mActive	= false;
	private Thread					profilingThread;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//battery annd cpu power
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private File					mBatteryElectricCurrentFile;
	private File					mBatteryVoltageFile;

	private TFloatList				mBatteryElectricCurrentList;
	private TFloatList				mBatteryVoltageList;

	private boolean					mTrackBattery;

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Power Sensors (files and voltages)
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int						mPowerSensorsValues;
	private float					mListValue;
	private float					mFloatListValue;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private String					line;

	//time and counter lists
	private TLongList				mTimeList;
	private long					mFirstStart;
	private TIntList				mCountList;
	private int						mCount;

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class StepBinder extends Binder
	{
		SynarProfilerService getService()
		{
			return SynarProfilerService.this;
		}
	}

	@Override
	public void onCreate()
	{
		Log.i(TAG, "[SERVICE] onCreate");
		super.onCreate();

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();

		// Load settings
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		acquireWakeLock();

		// get the data sampling and data flushing periods
		mSensorRateMilliseconds = Integer.valueOf(mSettings.getString("sampling_period", "1000").trim());
		mFlushDataMilliseconds = Integer.valueOf(mSettings.getString("data_flushing_period", "100000").trim());

		//open output file to write header

		mOutput = null;
		Date lm = new Date();
		String fileName = "synar_profiler" + new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss").format(lm) + ".csv";
		try
		{
			File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
			FileWriter fileWriter = new FileWriter(configFile);
			mOutput = new BufferedWriter(fileWriter);
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.toString());
		}

		mTimeList = new TLongArrayList();
		mCountList = new TIntArrayList();

		try
		{

			mOutput.write("Count, Time [ms], ");

			//check profiling flags

			mTrackBattery = mSettings.getBoolean("battery_power", true);
			if (mTrackBattery)
			{
				mBatteryElectricCurrentList = new TFloatArrayList();
				mBatteryVoltageList = new TFloatArrayList();
				mOutput.write("Battery Current [uA], Battery Voltage [V], Battery Power [uW], ");
				mBatteryElectricCurrentFile = new File("/sys/class/power_supply/battery/current_now");
				mBatteryVoltageFile = new File("/sys/class/power_supply/battery/voltage_now");
			}

			mOutput.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		registerDetector();

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mReceiver, filter);

		// Tell the user we started.
		Toast.makeText(this, getText(R.string.started)
				+ "\nSampling period " + mSensorRateMilliseconds + " ms"
				+ "\nData flush period " + mFlushDataMilliseconds + " ms"
				+ "\nLog saved to " + fileName,
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		Log.i(TAG, "[SERVICE] onStart");
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy()
	{
		Log.i(TAG, "[SERVICE] onDestroy");

		// Unregister our receiver.
		unregisterReceiver(mReceiver);
		unregisterDetector();

		mNM.cancel(R.string.app_name);

		wakeLock.release();

		super.onDestroy();

		// Stop detecting
		unregisterDetector();

		// Tell the user we stopped.
		Toast.makeText(this, getText(R.string.stopped), Toast.LENGTH_LONG).show();
	}

	private void registerDetector()
	{
		mActive = true;

		profilingThread = new Thread()
		{
			public void run()
			{

				mPowerSensorsValues = 0;
				mCount = 0;

				mFirstStart = mFlushDataStart = Utils.currentTimeInMillis();
				while (mActive)
				{
					try
					{
						mSensorRateStart = Utils.currentTimeInMillis();
						mTimeList.add(mSensorRateStart - mFirstStart);

						if (mTrackBattery)
						{
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mBatteryElectricCurrentFile, "r");
								line = localRandomAccessFile.readLine();

								mBatteryElectricCurrentList.add(
										line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mBatteryVoltageFile, "r");
								line = localRandomAccessFile.readLine();
								mBatteryVoltageList.add(line != null ? Integer.parseInt(line) / 1000.0F : -1);
								localRandomAccessFile.close();
							}
						}

						mCountList.add(mCount);
						mCount++;
						mPowerSensorsValues++;

						mFlushDataStop = mSensorRateStop = Utils.currentTimeInMillis();
						mSleepMilliseconds = (int) (mSensorRateMilliseconds - (mSensorRateStop - mSensorRateStart));
						if (mSleepMilliseconds > 0)
						{
							Thread.sleep(mSensorRateMilliseconds, 0);
						}

						if ((mFlushDataStop - mFlushDataStart) > mFlushDataMilliseconds)
						{
							//put to file
							mFlushDataStart = mFlushDataStop;

							write_data();
							mOutput.flush();

							mPowerSensorsValues = 0;

						}
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				try
				{
					write_data();

					mOutput.flush();
					mOutput.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

		};
		profilingThread.start();
	}

	private void write_data()
	{
		try
		{
			for (int i = 0; i < mPowerSensorsValues; i++)
			{
				mOutput.write(mCountList.get(i) + ", " + mTimeList.get(i) + ",");

				if (mTrackBattery)
				{
					mListValue = mBatteryElectricCurrentList.get(i);
					mFloatListValue = mBatteryVoltageList.get(i);
					mOutput.write(mListValue + ", " + mFloatListValue + ", "
							+ (mListValue * mFloatListValue) + ", ");
				}

				mOutput.newLine();

			}

			mTimeList.clear();
			mCountList.clear();

			if (mTrackBattery)
			{
				mBatteryElectricCurrentList.clear();
				mBatteryVoltageList.clear();
			}

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private void unregisterDetector()
	{
		mActive = false;
		profilingThread = null;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i(TAG, "[SERVICE] onBind");
		return mBinder;
	}

	/**
	 * Receives messages from activity.
	 */
	private final IBinder	mBinder	= new StepBinder();

	public interface ICallback
	{
		public void stepsChanged(int value);
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification()
	{
		CharSequence text = getText(R.string.app_name);
		Notification notification = new Notification(R.drawable.ic_notification, null,
				System.currentTimeMillis());
		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent fallDetectorIntent = new Intent();
		fallDetectorIntent.setComponent(new ComponentName(this, SynarProfiler.class));
		fallDetectorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				fallDetectorIntent, 0);
		notification.setLatestEventInfo(this, text,
				getText(R.string.notification_subtitle), contentIntent);

		mNM.notify(R.string.app_name, notification);
	}

	// BroadcastReceiver for handling ACTION_SCREEN_OFF.
	private BroadcastReceiver	mReceiver	= new BroadcastReceiver()
											{
												@Override
												public void onReceive(Context context, Intent intent)
												{
													// Check action just to be on the safe side.
													if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
													{
														// Unregisters the listener and registers it again.
														SynarProfilerService.this.unregisterDetector();
														SynarProfilerService.this.registerDetector();
														if (mSettings.getString("operation_level", "run_in_background").equals("wake_up"))
														{
															wakeLock.release();
															acquireWakeLock();
														}
													}
												}
											};

	private void acquireWakeLock()
	{
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		int wakeFlags;
		if (mSettings.getString("operation_level", "run_in_background").equals("wake_up"))
		{
			wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
		}
		else if (mSettings.getString("operation_level", "run_in_background").equals("keep_screen_on"))
		{
			wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK;
		}
		else
		{
			wakeFlags = PowerManager.PARTIAL_WAKE_LOCK;
		}
		wakeLock = pm.newWakeLock(wakeFlags, TAG);
		wakeLock.acquire();
	}

}
