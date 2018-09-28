package ca.jvsh.falldetectionlogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;
import android.widget.Toast;

public class FallDetectionService extends Service implements SensorEventListener
{
	//Sensors
	private SensorManager								mSensorManager;

	private PowerManager.WakeLock						wakeLock;

	/*
	 * This is a list of callbacks that have been registered with the
	 * service.  Note that this is package scoped (instead of private) so
	 * that it can be accessed more efficiently from inner classes.
	 */
	final RemoteCallbackList<IRemoteServiceCallback>	mCallbacks	= new RemoteCallbackList<IRemoteServiceCallback>();

	NotificationManager									mNM;

	private BufferedWriter								mOutputAccelerometer;
	private BufferedWriter								mOutputGravity;
	private BufferedWriter								mOutputGyroscope;
	private BufferedWriter								mOutputMagneticField;
	private BufferedWriter								mOutputLinearAcceleration;

	Display												mDisplay;

	//public static final int REQUIRED_SENSOR_TYPE = Sensor.TYPE_ACCELEROMETER;
	//public static final int REQUIRED_SENSOR_TYPE = Sensor.TYPE_GRAVITY;
	//public static final int REQUIRED_SENSOR_TYPE = Sensor.TYPE_GYROSCOPE;
	//public static final int REQUIRED_SENSOR_TYPE = Sensor.TYPE_MAGNETIC_FIELD;
	//public static final int REQUIRED_SENSOR_TYPE = Sensor.TYPE_LINEAR_ACCELERATION;

	//public static final String SENSOR_TYPE_NAME = "accelerometer_";
	//public static final String SENSOR_TYPE_NAME = "gravity_";
	//public static final String SENSOR_TYPE_NAME = "gyroscope_";
	//public static final String SENSOR_TYPE_NAME = "magnetic_field_";
	//public static final String SENSOR_TYPE_NAME = "linear_acceleration_";

	@Override
	public void onCreate()
	{
		super.onCreate();

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		mDisplay = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		// Load settings
		acquireWakeLock();

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// Display a notification about us starting.
		showNotification();
	}

	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i("LocalService", "Received start id " + startId + ": " + intent);

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				SensorManager.SENSOR_DELAY_FASTEST);

		Date lm = new Date();
		mOutputAccelerometer = null;
		String fileName = "FallDetectionLogger_accelerometer" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(lm) + ".csv";
		try
		{
			File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
			FileWriter fileWriter = new FileWriter(configFile);
			mOutputAccelerometer = new BufferedWriter(fileWriter);
		}
		catch (IOException ex)
		{
			Log.e(FallDetectionService.class.getName(), ex.toString());
		}

		try
		{
			mOutputAccelerometer.write("X, Y, Z, Timestamp, ");
			mOutputAccelerometer.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		mOutputGravity = null;
		fileName = "FallDetectionLogger_gravity" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(lm) + ".csv";
		try
		{
			File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
			FileWriter fileWriter = new FileWriter(configFile);
			mOutputGravity = new BufferedWriter(fileWriter);
		}
		catch (IOException ex)
		{
			Log.e(FallDetectionService.class.getName(), ex.toString());
		}

		try
		{
			mOutputGravity.write("X, Y, Z, Timestamp, ");
			mOutputGravity.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		mOutputGyroscope = null;
		fileName = "FallDetectionLogger_gyroscope" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(lm) + ".csv";
		try
		{
			File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
			FileWriter fileWriter = new FileWriter(configFile);
			mOutputGyroscope = new BufferedWriter(fileWriter);
		}
		catch (IOException ex)
		{
			Log.e(FallDetectionService.class.getName(), ex.toString());
		}

		try
		{
			mOutputGyroscope.write("X, Y, Z, Timestamp, ");
			mOutputGyroscope.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		mOutputMagneticField = null;
		fileName = "FallDetectionLogger_magnetic_field" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(lm) + ".csv";
		try
		{
			File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
			FileWriter fileWriter = new FileWriter(configFile);
			mOutputMagneticField = new BufferedWriter(fileWriter);
		}
		catch (IOException ex)
		{
			Log.e(FallDetectionService.class.getName(), ex.toString());
		}

		try
		{
			mOutputMagneticField.write("X, Y, Z, Timestamp, ");
			mOutputMagneticField.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		mOutputLinearAcceleration = null;
		fileName = "FallDetectionLogger_linear_acceleration" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(lm) + ".csv";
		try
		{
			File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
			FileWriter fileWriter = new FileWriter(configFile);
			mOutputLinearAcceleration = new BufferedWriter(fileWriter);
		}
		catch (IOException ex)
		{
			Log.e(FallDetectionService.class.getName(), ex.toString());
		}

		try
		{
			mOutputLinearAcceleration.write("X, Y, Z, Timestamp, ");
			mOutputLinearAcceleration.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			mBinder.registerCallback(mSelfCallback);
		}
		catch (RemoteException e)
		{
			// In this case the service has crashed before we could even
			// do anything with it; we can count on soon being
			// disconnected (and then reconnected if it can be restarted)
			// so there is no need to do anything here.
		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		try
		{
			mOutputAccelerometer.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			mOutputGravity.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			mOutputGyroscope.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			mOutputMagneticField.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			mOutputLinearAcceleration.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		mSensorManager.unregisterListener(this);

		// Cancel the persistent notification.
		mNM.cancel(R.string.remote_service_started);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();

		wakeLock.release();

		try
		{
			mBinder.unregisterCallback(mSelfCallback);
		}
		catch (RemoteException e)
		{
			// There is nothing special we need to do if the service
			// has crashed.
		}

		// Unregister all callbacks.
		mCallbacks.kill();

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// Select the interface to return.  If your service only implements
		// a single interface, you can just return it here without checking
		// the Intent.
		if (IRemoteService.class.getName().equals(intent.getAction()))
		{
			return mBinder;
		}
		return null;
	}

	/**
	 * The IRemoteInterface is defined through IDL
	 */
	private final IRemoteService.Stub	mBinder	= new IRemoteService.Stub()
												{
													public void registerCallback(IRemoteServiceCallback cb)
													{
														if (cb != null)
															mCallbacks.register(cb);
													}

													public void unregisterCallback(IRemoteServiceCallback cb)
													{
														if (cb != null)
															mCallbacks.unregister(cb);
													}
												};

	@Override
	public void onTaskRemoved(Intent rootIntent)
	{
		Toast.makeText(this, "Task removed: " + rootIntent, Toast.LENGTH_LONG).show();
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		switch (accuracy)
		{
			case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
				Toast.makeText(this, "maximum accuracy", Toast.LENGTH_LONG).show();
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
				Toast.makeText(this, "average level of accuracy", Toast.LENGTH_LONG).show();
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
				Toast.makeText(this, "low accuracy", Toast.LENGTH_LONG).show();
				break;
			case SensorManager.SENSOR_STATUS_UNRELIABLE:
				Toast.makeText(this, "sensor cannot be trusted", Toast.LENGTH_LONG).show();
				break;

		}
	}

	public void onSensorChanged(SensorEvent event)
	{

		synchronized (this)
		{

			switch (event.sensor.getType())
			{
				case Sensor.TYPE_ACCELEROMETER:
					try
					{
						mOutputAccelerometer.write(String.format("%f, %f, %f, %d", event.values[0], event.values[1], event.values[2], event.timestamp));
						mOutputAccelerometer.newLine();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;
				case Sensor.TYPE_GRAVITY:
					try
					{
						mOutputGravity.write(String.format("%f, %f, %f, %d", event.values[0], event.values[1], event.values[2], event.timestamp));
						mOutputGravity.newLine();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;
				case Sensor.TYPE_GYROSCOPE:
					try
					{
						mOutputGyroscope.write(String.format("%f, %f, %f, %d", event.values[0], event.values[1], event.values[2], event.timestamp));
						mOutputGyroscope.newLine();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					try
					{
						mOutputMagneticField.write(String.format("%f, %f, %f, %d", event.values[0], event.values[1], event.values[2], event.timestamp));
						mOutputMagneticField.newLine();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;
				case Sensor.TYPE_LINEAR_ACCELERATION:
					try
					{
						mOutputLinearAcceleration.write(String.format("%f, %f, %f, %d", event.values[0], event.values[1], event.values[2], event.timestamp));
						mOutputLinearAcceleration.newLine();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					break;

			}
		}
	}

	private IRemoteServiceCallback	mSelfCallback	= new IRemoteServiceCallback.Stub()
													{
														/**
														 * This is called by the remote service regularly to tell us about
														 * new values.  Note that IPC calls are dispatched through a thread
														 * pool running in each process, so the code executing here will
														 * NOT be running in our main thread like most other things -- so,
														 * to update the UI, we need to use a Handler to hop over there.
														 */
														public void accelerometerChanged(float X, float Y, float Z, long timestamp)
														{
															synchronized (this)
															{

															}
														}
													};

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification()
	{
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.remote_service_started);

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, FallDetectionActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		Notification notification = new NotificationCompat.Builder((Context) this)
				.setContentText(getText(R.string.remote_service_label))
				.setSubText(text)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(contentIntent)
				.build();

		// Send the notification.
		// We use a string id because it is a unique number.  We use it later to cancel.
		mNM.notify(R.string.remote_service_started, notification);
	}

	// ----------------------------------------------------------------------

	private void acquireWakeLock()
	{
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, FallDetectionService.class.getName());
		wakeLock.acquire();
	}
}
