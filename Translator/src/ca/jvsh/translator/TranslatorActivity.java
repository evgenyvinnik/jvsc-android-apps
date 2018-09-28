package ca.jvsh.translator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ca.jvsh.translator.TranslatorApplication;
import ca.jvsh.translator.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class TranslatorActivity extends Activity implements OnInitListener, OnClickListener, OnGesturePerformedListener
{
	private static final String	TAG								= "TranslatorActivity";

	private GestureOverlayView	mGestureOverlayView;

	private static final int	VOICE_RECOGNITION_REQUEST_CODE	= 1234;

	private ListView			mList;

	private int					MY_DATA_CHECK_CODE				= 0;

	private SensorManager		mSensorManager;

	private ShakeEventListener	mSensorListener;

	/**
	 * Launch Home activity helper
	 * 
	 * @param c context where launch home from (used by SplashscreenActivity)
	 */
	public static void launch(Context c)
	{
		Intent intent = new Intent(c, TranslatorActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		c.startActivity(intent);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
		load();

		mGestureOverlayView = (GestureOverlayView) findViewById(R.id.gestures);
		mGestureOverlayView.addOnGesturePerformedListener(this);

		// Get display items for later interaction
		Button speakButton = (Button) findViewById(R.id.btn_speak);

		mList = (ListView) findViewById(R.id.list);

		// Check to see if a recognition activity is present
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() != 0)
		{
			speakButton.setOnClickListener(this);
		}
		else
		{
			speakButton.setEnabled(false);
			speakButton.setText("Recognizer not present");
		}

		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

		mSensorListener = new ShakeEventListener();
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

		mSensorListener.setOnShakeListener(new ShakeEventListener.OnShakeListener()
		{

			public void onShake()
			{
				Toast.makeText(TranslatorActivity.this, "Shaking!", Toast.LENGTH_SHORT).show();
				
				if (mList.getCount() > 0)
				{
					mList.setAdapter(null);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.about_menu_item:
				new AboutDialog(this).show();
				break;

			case R.id.settings_menu_item:
				SettingsActivity.launch(this);
				break;

			default:

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume()
	{
		boolean gesturesEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("gestures", true);
		mGestureOverlayView.setEnabled(gesturesEnabled);
		super.onResume();

		mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

	}

	@Override
	protected void onStop()
	{
		mSensorManager.unregisterListener(mSensorListener);
		super.onStop();
	}

	/**
	 * Handle the click on the start recognition button.
	 */
	public void onClick(View v)
	{
		if (v.getId() == R.id.btn_speak)
		{
			startVoiceRecognitionActivity();
		}
	}

	/**
	 * Fire an intent to start the speech recognition activity.
	 */
	private void startVoiceRecognitionActivity()
	{
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	/**
	 * Handle the results from the recognition activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK)
		{
			// Fill the list view with the strings the recognizer thought it could have heard
			ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, matches));
			mList.setOnItemClickListener(mHomeItemClickListener);
		}
		else if (requestCode == MY_DATA_CHECK_CODE)
		{
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				// success, create the TTS instance
				tts = new TextToSpeech(this, this);
			}
			else
			{
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onInit(int status)
	{
		if (status == TextToSpeech.SUCCESS)
		{
			Toast.makeText(TranslatorActivity.this, "Text-To-Speech engine is initialized", Toast.LENGTH_LONG).show();
		}
		else if (status == TextToSpeech.ERROR)
		{
			Toast.makeText(TranslatorActivity.this, "Error occurred while initializing Text-To-Speech engine", Toast.LENGTH_LONG).show();
		}
	}

	private TextToSpeech		tts;

	/**
	 * Launches menu actions
	 */
	private OnItemClickListener	mHomeItemClickListener	= new OnItemClickListener()
														{

															@Override
															public void onItemClick(AdapterView<?> adapterView, View view, int index, long time)
															{
																try
																{
																	String text = (String) adapterView.getAdapter().getItem(index);
																	Toast.makeText(view.getContext(), text, Toast.LENGTH_SHORT).show();
																	tts.speak(text, TextToSpeech.QUEUE_ADD, null);

																}
																catch (ClassCastException e)
																{
																	Log.w(TAG, "Unexpected position number was occurred");
																}
															}
														};

	private GestureLibrary		mLibrary;
	private boolean				mLoaded					= false;

	private boolean load()
	{
		mLoaded = mLibrary.load();
		return mLoaded;
	}

	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture)
	{
		if (!mLoaded)
		{
			if (!load())
			{
				return;
			}
		}

		ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
		if (predictions.size() > 0)
		{
			Prediction prediction = predictions.get(0);
			Log.v(TranslatorApplication.TAG, "Gesture " + prediction.name + " recognized with score " + prediction.score);
			if (prediction.score > 2.0)
			{

				String action = prediction.name;
				if ("play".equals(action))
				{
					Toast.makeText(this, "Speak", Toast.LENGTH_SHORT).show();
					startVoiceRecognitionActivity();
				}
				else if ("next".equals(action))
				{
					Toast.makeText(this, "Talk", Toast.LENGTH_SHORT).show();

					if (mList.getCount() > 0)
					{
						String text = (String) mList.getItemAtPosition(0);
						Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
						tts.speak(text, TextToSpeech.QUEUE_ADD, null);
					}
				}
			}
		}
	}

}