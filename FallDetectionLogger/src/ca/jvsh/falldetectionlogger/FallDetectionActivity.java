package ca.jvsh.falldetectionlogger;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

import java.lang.ref.WeakReference;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class FallDetectionActivity extends Activity
{
	private static final int	AXES			= 3;
	private static final int	COUNTS			= 777;
	protected static final int	UPDATE_COUNTER	= 10;

	/** The primary interface we will be calling on the service. */
	IRemoteService				mService		= null;

	TextView					mCallbackText;
	TextView					mAccelerometerText;

	private static final int	RESULT_SETTINGS	= 1;

	private boolean				mIsBound;

	private TLongList			mTimeStampsList;
	//this variable is to reduce frequency of the screen updates - we don't need it to update text field values so often
	private int					mUpdateCounter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fall_detection);

		mCallbackText = (TextView) findViewById(R.id.callback);
		mCallbackText.setText("Not attached.");

		mAccelerometerText = (TextView) findViewById(R.id.accelerometer);

		mSensorHandler = new ActivityDataHandler(this);
		mTimeStampsList = new TLongArrayList(COUNTS);
		mTimeStampsList.fill(0, COUNTS, 0);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.fall_detection, menu);
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_start:
				// Make sure the service is started.  It will continue running
				// until someone calls stopService().
				// We use an action code here, instead of explictly supplying
				// the component name, so that other packages can replace
				// the service.
				startService(new Intent("ca.jvsh.falldetectionlogger.REMOTE_SERVICE"));

				if (isFallDetectionServiceRunning())
				{
					// Establish a couple connections with the service, binding
					// by interface names.  This allows other applications to be
					// installed that replace the remote service by implementing
					// the same interface.
					mIsBound = bindService(new Intent(IRemoteService.class.getName()),
							mConnection, 0);

					if (mIsBound)
					{
						mCallbackText.setText(R.string.remote_service_binding);
						// As part of the sample, tell the user what happened.
						Toast.makeText(FallDetectionActivity.this, R.string.remote_service_binding,
								Toast.LENGTH_SHORT).show();
						mTimeStampsList.fill(0, COUNTS, 0);
					}
				}
				return true;

			case R.id.menu_stop:

				if (isFallDetectionServiceRunning() && mIsBound)
				{

					// If we have received the service, and hence registered with
					// it, then now is the time to unregister.
					if (mService != null)
					{
						try
						{
							mService.unregisterCallback(mCallback);
						}
						catch (RemoteException e)
						{
							// There is nothing special we need to do if the service
							// has crashed.
						}
					}

					// Detach our existing connection.
					unbindService(mConnection);
					mIsBound = false;
					mCallbackText.setText(R.string.remote_service_unbinding);

					// As part of the sample, tell the user what happened.
					Toast.makeText(FallDetectionActivity.this, R.string.remote_service_unbinding,
							Toast.LENGTH_SHORT).show();

				}
				// Cancel a previous call to startService().  Note that the
				// service will not actually stop at this point if there are
				// still bound clients.
				stopService(new Intent("ca.jvsh.falldetectionlogger.REMOTE_SERVICE"));

				return true;

			case R.id.menu_settings:
				startActivityForResult(new Intent(this, Settings.class), RESULT_SETTINGS);
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode)
		{
			case RESULT_SETTINGS:
				break;
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (isFallDetectionServiceRunning())
		{
			// Establish a couple connections with the service, binding
			// by interface names.  This allows other applications to be
			// installed that replace the remote service by implementing
			// the same interface.
			mIsBound = bindService(new Intent(IRemoteService.class.getName()),
					mConnection, 0);

			if (mIsBound)
			{
				mCallbackText.setText(R.string.remote_service_binding);
				// As part of the sample, tell the user what happened.
				Toast.makeText(FallDetectionActivity.this, R.string.remote_service_binding,
						Toast.LENGTH_SHORT).show();
				mTimeStampsList.fill(0, COUNTS, 0);
			}
		}
	}

	@Override
	protected void onPause()
	{
		if (isFallDetectionServiceRunning() && mIsBound)
		{

			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (mService != null)
			{
				try
				{
					mService.unregisterCallback(mCallback);
				}
				catch (RemoteException e)
				{
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}

			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			mCallbackText.setText(R.string.remote_service_unbinding);

			// As part of the sample, tell the user what happened.
			Toast.makeText(FallDetectionActivity.this, R.string.remote_service_unbinding,
					Toast.LENGTH_SHORT).show();

		}
		super.onPause();

	}

	/*
	* Class for interacting with the main interface of the service.
	*/
	private ServiceConnection		mConnection	= new ServiceConnection()
												{
													public void onServiceConnected(ComponentName className,
															IBinder service)
													{
														// This is called when the connection with the service has been
														// established, giving us the service object we can use to
														// interact with the service.  We are communicating with our
														// service through an IDL interface, so get a client-side
														// representation of that from the raw service object.
														mService = IRemoteService.Stub.asInterface(service);

														mCallbackText.setText("Attached.");

														// We want to monitor the service for as long as we are
														// connected to it.
														try
														{
															mService.registerCallback(mCallback);
														}
														catch (RemoteException e)
														{
															// In this case the service has crashed before we could even
															// do anything with it; we can count on soon being
															// disconnected (and then reconnected if it can be restarted)
															// so there is no need to do anything here.
														}

														// As part of the sample, tell the user what happened.
														Toast.makeText(FallDetectionActivity.this, R.string.remote_service_connected,
																Toast.LENGTH_SHORT).show();
													}

													public void onServiceDisconnected(ComponentName className)
													{
														// This is called when the connection with the service has been
														// unexpectedly disconnected -- that is, its process crashed.
														mService = null;

														mCallbackText.setText("Disconnected.");

														// As part of the sample, tell the user what happened.
														Toast.makeText(FallDetectionActivity.this, R.string.remote_service_disconnected,
																Toast.LENGTH_SHORT).show();
													}
												};

	// ----------------------------------------------------------------------
	// Code showing how to deal with callbacks.
	// ----------------------------------------------------------------------
	private ActivityDataHandler		mSensorHandler;
	/**
	 * This implementation is used to receive callbacks from the remote
	 * service.
	 */
	private IRemoteServiceCallback	mCallback	= new IRemoteServiceCallback.Stub()
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

														Message mSensorMessage = new Message();
														Bundle mMessageBundle = new Bundle();

														mMessageBundle.putFloat("X", X);
														mMessageBundle.putFloat("Y", Y);
														mMessageBundle.putFloat("Z", Z);
														mMessageBundle.putLong("Timestamp", timestamp);

														mSensorMessage.setData(mMessageBundle);

														mSensorHandler.sendMessage(mSensorMessage);
													}
												};

	static class ActivityDataHandler extends Handler
	{
		WeakReference<FallDetectionActivity>	mFallDetectionActivity;

		ActivityDataHandler(FallDetectionActivity fallDetectionActivity)
		{
			mFallDetectionActivity = new WeakReference<FallDetectionActivity>(fallDetectionActivity);
		}

		@Override
		public void handleMessage(Message msg)
		{
			synchronized (this)
			{
				FallDetectionActivity fallDetectionActivity = mFallDetectionActivity.get();

				Bundle bundle = msg.getData();

				float values[] = new float[AXES];
				values[0] = bundle.getFloat("X");
				values[1] = bundle.getFloat("Y");
				values[2] = bundle.getFloat("Z");

				fallDetectionActivity.mTimeStampsList.removeAt(0);
				fallDetectionActivity.mTimeStampsList.add(bundle.getLong("Timestamp"));

				if (fallDetectionActivity.mUpdateCounter++ % UPDATE_COUNTER == 0)
				{
					fallDetectionActivity.mAccelerometerText.setText(String.format("Acceleration: X %6.3f Y %6.3f Z %6.3f", values[0], values[1], values[2]));

					fallDetectionActivity.mCallbackText .setText(String.format(
									"Frequency: %7.3f Hz", COUNTS / ((double) (fallDetectionActivity.mTimeStampsList.get(COUNTS - 1) - fallDetectionActivity.mTimeStampsList.get(0)) / 1000000000.0)));
				}

			}

		}

	}

	private boolean isFallDetectionServiceRunning()
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
		{
			Log.i(FallDetectionActivity.class.getName(), " " + service.service.getClassName());
			if (FallDetectionService.class.getName().equals(service.service.getClassName()))
			{

				return true;
			}
		}
		return false;
	}
}
