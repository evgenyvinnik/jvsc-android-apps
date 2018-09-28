package ca.jvsh.fall;

import gnu.trove.function.TIntFunction;
import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Locale;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener
{
	//consts and magic numbers
	private static final int	AXES						= 3;
	private static final String	AXES_NAMES[]				= { "X: ", "Y: ", "Z: " };
	private static final int	COUNTS						= 234;
	private static final int	MIN							= 0;
	private static final int	MAX							= 1;
	private static final int	RANGE						= 2;

	private static final int	SVM_INPUTS					= 12;
	private static final int	SVM_INPUT_MEAN				= 0;
	private static final int	SVM_INPUT_MIN				= 3;
	private static final int	SVM_INPUT_MAX				= 6;
	private static final int	SVM_INPUT_RANGE				= 9;

	protected static final int	MSG_SENSOR					= 1;

	protected static final int	UPDATE_COUNTER				= 10;

	private static final String	TAG							= "FallDetectionUI";

	private static final String	ACTIVITIES[]				= { "ADL ", "Fall " };
	private static final String	ACTIVITIES_ADL_CLASSES[]	= { "Normal walk", "Standing quietly", "Standing to sitting", "Standing to lying",
															"Sit to stand", "Reach and pick", "Ascend stairs", "Descend stairs" };
	private static final String	ACTIVITIES_FALL_CLASSES[]	= { "Bump", "Misstep", "Incorrect stand to sit", "Incorrect sit to stand", "Collapse", "Slip",
															"Trip" };

	//Text views
	private TextView			mStatusTextView;
	private TextView			mMaxTextView;
	private TextView			mMinTextView;
	private TextView			mRangeTextView;
	private TextView			mAverageTextView;
	private TextView			mFrequencyTextView;

	//Radio buttons
	private RadioGroup			mSensorTypeRadioGroup;

	//TalkBack
	private CheckBox			mTalkBackCheckBox;
	private TextToSpeech		mTts;

	//Beep
	private CheckBox			mBeepCheckBox;
	private ToneGenerator		mToneGenerator;

	//Logger
	private TextView			mCurrentState;
	private EditText			mLogEditText;

	//Sensors
	private SensorManager		mSensorManager;
	private Sensor				mAccelerometer;
	private Sensor				mGyroscope;
	private SensorDataHandler	mSensorHandler;

	//flags
	private boolean				mTalkBack;
	private boolean				mBeep;

	//lists
	private TFloatList			mElementsList[]				= new TFloatList[AXES];
	private TIntList			mMinMaxIndicesList[][]		= new TIntList[AXES][2];
	private double				mRanges[]					= new double[AXES];
	private float				mTotal[]					= new float[AXES];
	private TLongList			mTimeStampsList;

	//this variable is to reduce frequency of the screen updates - we don't need it to update text field values so often
	private int					mUpdateCounter;

	//svm
	private svm_model			mCombinedFallDetectionModel;
	private svm_model			mClassifiedFallDetectionModel;
	private double				SvmPredictionCombined, SvmPredictionClassified;
	private double				SvmPredictionCombinedPrev, SvmPredictionClassifiedPrev;

	private boolean				mNotFirst;

	private double				mGlobalMinMaxRange[][]		= new double[SVM_INPUTS][3];

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mStatusTextView = ((TextView) findViewById(R.id.textView_status));
		mMaxTextView = ((TextView) findViewById(R.id.textView_max));
		mMinTextView = ((TextView) findViewById(R.id.textView_min));
		mRangeTextView = ((TextView) findViewById(R.id.textView_range));
		mAverageTextView = ((TextView) findViewById(R.id.textView_average));
		mFrequencyTextView = ((TextView) findViewById(R.id.textView_frequency));

		mSensorTypeRadioGroup = ((RadioGroup) findViewById(R.id.radioGroupSensor));
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mSensorHandler = new SensorDataHandler(this);

		mTalkBackCheckBox = ((CheckBox) findViewById(R.id.checkBoxTalkback));
		mTalkBackCheckBox.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//is chkIos checked?
				mTalkBack = ((CheckBox) v).isChecked();

				if (mTalkBack)
				{
					mTts.speak("Talkback is on", TextToSpeech.QUEUE_FLUSH, null);
				}
				else
				{
					Toast.makeText(v.getContext(), "Talkback is off", Toast.LENGTH_SHORT).show();
				}
			}
		});
		mTts = new TextToSpeech(this, this);

		mBeepCheckBox = ((CheckBox) findViewById(R.id.checkBoxBeep));
		mBeepCheckBox.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//is chkIos checked?
				mBeep = ((CheckBox) v).isChecked();

				if (mBeep)
					mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
				else
					Toast.makeText(v.getContext(), "No beeping on fall", Toast.LENGTH_SHORT).show();
			}
		});
		mToneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

		mCurrentState = ((TextView) findViewById(R.id.textView_state));

		mLogEditText = ((EditText) findViewById(R.id.editTextLog));

		for (int i = 0; i < AXES; i++)
		{
			mElementsList[i] = new TFloatArrayList(COUNTS);
			for (int j = 0; j < 2; j++)
				mMinMaxIndicesList[i][j] = new TIntArrayList();
		}

		mTimeStampsList = new TLongArrayList(COUNTS);

		try
		{

			BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("model_combined_4.txt")));

			mCombinedFallDetectionModel = svm.svm_load_model(br);
			mCombinedFallDetectionModel.param.degree = 3;
			mCombinedFallDetectionModel.param.C = 4096;
			mCombinedFallDetectionModel.param.nu = 0.5;
			mCombinedFallDetectionModel.param.cache_size = 100;
			mCombinedFallDetectionModel.param.eps = 1e-3;
			mCombinedFallDetectionModel.param.p = 0.1;
			mCombinedFallDetectionModel.param.shrinking = 1;

			br = new BufferedReader(new InputStreamReader(getAssets().open("model_classified_4.txt")));

			mClassifiedFallDetectionModel = svm.svm_load_model(br);
			mClassifiedFallDetectionModel.param.degree = 3;
			mClassifiedFallDetectionModel.param.C = 4096;
			mClassifiedFallDetectionModel.param.nu = 0.5;
			mClassifiedFallDetectionModel.param.cache_size = 100;
			mClassifiedFallDetectionModel.param.eps = 1e-3;
			mClassifiedFallDetectionModel.param.p = 0.1;
			mClassifiedFallDetectionModel.param.shrinking = 1;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < 9; i++)
		{
			mGlobalMinMaxRange[i][MIN] = - 39.2266;
			mGlobalMinMaxRange[i][MAX] =  39.2266;
		}
							
		for (int i = 9; i < SVM_INPUTS; i++)
		{
			mGlobalMinMaxRange[i][MIN] = 0;
			mGlobalMinMaxRange[i][MAX] = 78.4532;
		}

		for (int i = 0; i < SVM_INPUTS; i++)
			mGlobalMinMaxRange[i][RANGE] = mGlobalMinMaxRange[i][MAX] - mGlobalMinMaxRange[i][MIN];

		//svm tests
		/*svm_node[] svmInputs = new svm_node[12];
		for (int i = 0; i < 12; i++)
		{
			svmInputs[i] = new svm_node();
			svmInputs[i].index = i+1;
		}
		
		double d;

		//normal walk
		svmInputs[0].value = -0.997289195920020;
		svmInputs[1].value = 0.592711939806421;
		svmInputs[2].value = -0.0544100172755184;
		svmInputs[3].value = 0.0563041759037544;
		svmInputs[4].value = -0.225156471326762;
		svmInputs[5].value = 0.413740892105432;
		svmInputs[6].value = -0.740449811785684;
		svmInputs[7].value = -0.114613407615276;
		svmInputs[8].value = -0.446313500452597;
		svmInputs[9].value = 0.650151478407600;
		svmInputs[10].value = 0.0706165018436840;
		svmInputs[11].value = 0.519740778429027;
		
		d = svm.svm_predict(mClassifiedFallDetectionModel, svmInputs);
		Log.d(TAG, " predicted " + d);

		
		//lost of consciousness
		svmInputs[0].value = 0.0786503873390698;
		svmInputs[1].value = -0.344754568626876;
		svmInputs[2].value = -0.0334360500517317;
		svmInputs[3].value = -1;
		svmInputs[4].value = -1;
		svmInputs[5].value = 0.141265097111009;
		svmInputs[6].value = 0.586172219069278;
		svmInputs[7].value = -0.190798060675274;
		svmInputs[8].value = 1;
		svmInputs[9].value = -0.728432190831160;
		svmInputs[10].value = -0.258636345253017;
		svmInputs[11].value = -0.364843380280982;

		d = svm.svm_predict(mClassifiedFallDetectionModel, svmInputs);
		Log.d(TAG, " predicted " + d);
		
		//standing quietly
		svmInputs[0].value =-0.958774966936017;
		svmInputs[1].value = 0.974041492539280;
		svmInputs[2].value = 0.000455869210387094;
		
		svmInputs[3].value = 0.409149865244530;
		svmInputs[4].value = 0.993512420723146;
		svmInputs[5].value = 0.943683720742329;
		
		svmInputs[6].value = -0.997572900087643;
		svmInputs[7].value = -0.616555370136567;
		svmInputs[8].value = -0.829635411385251;
		
		svmInputs[9].value = 0.999417583535953;
		svmInputs[10].value = 0.999404341356533;
		svmInputs[11].value = 1;

		d = svm.svm_predict(mClassifiedFallDetectionModel, svmInputs);
		Log.d(TAG, " predicted " + d);*/
		//test
		/*{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 2 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}

		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 3 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}
		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 4 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}
		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 2 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}
		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 6 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}
		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 2 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}
		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 5 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}
		
		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();
		
			mMessageBundle.putInt("SensorType", 1);
			mMessageBundle.putFloatArray("SensorValues", new float[] { 1 });
			mMessageBundle.putLong("Timestamp", System.nanoTime());
		
			mSensorMessage.setData(mMessageBundle);
		
			mSensorHandler.sendMessage(mSensorMessage);
		}*/
	}

	@Override
	public void onDestroy()
	{
		mSensorManager.unregisterListener(this);
		// Don't forget to shutdown!
		if (mTts != null)
		{
			mTts.stop();
			mTts.shutdown();
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.menu_start:

				onMenuItemStart();

				return true;

			case R.id.menu_stop:

				onMenuItemStop();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void onMenuItemStart()
	{
		Toast.makeText(this, "Starting receiving data", Toast.LENGTH_SHORT).show();
		mLogEditText.setText("");

		for (int i = 0; i < AXES; i++)
		{
			mElementsList[i].fill(0, COUNTS, 0);
			for (int j = 0; j < 2; j++)
			{
				mMinMaxIndicesList[i][j].clear();
				mMinMaxIndicesList[i][j].add(COUNTS - 1);
			}
			mTotal[i] = 0;
		}
			/*mGlobalMinMaxRange[0][MIN] = -9.85152435064935;
		mGlobalMinMaxRange[1][MIN] = -8.11194208017336;
		mGlobalMinMaxRange[2][MIN] = -6.01557056843157;
		mGlobalMinMaxRange[3][MIN] = -39.2266000000000;
		mGlobalMinMaxRange[4][MIN] = -39.2266000000000;
		mGlobalMinMaxRange[5][MIN] = -39.2266000000000;
		mGlobalMinMaxRange[6][MIN] = -9.60204500000000;
		mGlobalMinMaxRange[7][MIN] = -8.03692800000000;
		mGlobalMinMaxRange[8][MIN] = -4.89430600000000;
		mGlobalMinMaxRange[9][MIN] = -78.4532000000000;
		mGlobalMinMaxRange[10][MIN] = -78.4532000000000;
		mGlobalMinMaxRange[11][MIN] = -78.4532000000000;
		
													

		
		mGlobalMinMaxRange[0][MAX] = 2.88546640736728;
		mGlobalMinMaxRange[1][MAX] = 1.06157259940060;
		mGlobalMinMaxRange[2][MAX] = 3.65364376423576;
		mGlobalMinMaxRange[3][MAX] = 2.79692600000000;
		mGlobalMinMaxRange[4][MAX] = 0.991791000000000;
		mGlobalMinMaxRange[5][MAX] = -0.116985000000000;
		mGlobalMinMaxRange[6][MAX] = 39.2266000000000;
		mGlobalMinMaxRange[7][MAX] = 39.2266000000000;
		mGlobalMinMaxRange[8][MAX] = 39.2266000000000;
		mGlobalMinMaxRange[9][MAX] = -0.0522570000000009;
		mGlobalMinMaxRange[10][MAX] = -0.139890000000000;
		mGlobalMinMaxRange[11][MAX] = -0.0822530000000001;
		
													

		mGlobalMinMaxRange[0][RANGE] = 12.7369907580166;
		mGlobalMinMaxRange[1][RANGE] = 9.17351467957395;
		mGlobalMinMaxRange[2][RANGE] = 9.66921433266733;
		mGlobalMinMaxRange[3][RANGE] = 42.0235260000000;
		mGlobalMinMaxRange[4][RANGE] = 40.2183910000000;
		mGlobalMinMaxRange[5][RANGE] = 39.1096150000000;
		mGlobalMinMaxRange[6][RANGE] = 48.8286450000000;
		mGlobalMinMaxRange[7][RANGE] = 47.2635280000000;
		mGlobalMinMaxRange[8][RANGE] = 44.1209060000000;
		mGlobalMinMaxRange[9][RANGE] = 78.4009430000000;
		mGlobalMinMaxRange[10][RANGE] = 78.3133100000000;
		mGlobalMinMaxRange[11][RANGE] = 78.3709470000000;*/

		/*mGlobalMinMaxRange[SVM_INPUT_MIN + i][MIN] = Double.MAX_VALUE;
		mGlobalMinMaxRange[SVM_INPUT_MAX + i][MIN] = Double.MAX_VALUE;
		mGlobalMinMaxRange[SVM_INPUT_RANGE + i][MIN] = Double.MAX_VALUE;

		mGlobalMinMaxRange[SVM_INPUT_MEAN + i][MAX] = Double.MIN_VALUE;
		mGlobalMinMaxRange[SVM_INPUT_MIN + i][MAX] = Double.MIN_VALUE;
		mGlobalMinMaxRange[SVM_INPUT_MAX + i][MAX] = Double.MIN_VALUE;
		mGlobalMinMaxRange[SVM_INPUT_RANGE + i][MAX] = Double.MIN_VALUE;

		mGlobalMinMaxRange[SVM_INPUT_MEAN + i][RANGE] = 0;
		mGlobalMinMaxRange[SVM_INPUT_MIN + i][RANGE] = 0;
		mGlobalMinMaxRange[SVM_INPUT_MAX + i][RANGE] = 0;
		mGlobalMinMaxRange[SVM_INPUT_RANGE + i][RANGE] = 0;*/
		
		mTimeStampsList.fill(0, COUNTS, 0);

		mNotFirst = false;

		switch (mSensorTypeRadioGroup.getCheckedRadioButtonId())
		{
			case R.id.radioAccelerometer:
				mSensorManager.registerListener(this,
						mAccelerometer,
						SensorManager.SENSOR_DELAY_FASTEST);
				mStatusTextView.setText("receiving data from accelerometer");
				break;

			case R.id.radioGyroscope:
				mSensorManager.registerListener(this,
						mGyroscope,
						SensorManager.SENSOR_DELAY_FASTEST);
				mStatusTextView.setText("receiving data from gyroscope");
				break;
		}

		for (int i = 0; i < mSensorTypeRadioGroup.getChildCount(); i++)
		{
			mSensorTypeRadioGroup.getChildAt(i).setEnabled(false);
		}
	}

	private void onMenuItemStop()
	{
		mSensorManager.unregisterListener(this);

		for (int i = 0; i < mSensorTypeRadioGroup.getChildCount(); i++)
		{
			mSensorTypeRadioGroup.getChildAt(i).setEnabled(true);
		}

		Toast.makeText(this, "Stopping receiving data", Toast.LENGTH_SHORT).show();
		mStatusTextView.setText("no activity");

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

	float	c;

	public void onSensorChanged(SensorEvent event)
	{
		synchronized (this)
		{
			Message mSensorMessage = new Message();
			Bundle mMessageBundle = new Bundle();

			mMessageBundle.putInt("SensorType", event.sensor.getType());

			/*c = -event.values[1];
			event.values[1] = -event.values[0];
			event.values[0] = c;
			
			event.values[2] = - event.values[2];*/

			/*c = -event.values[2];
			event.values[2] = -event.values[0];
			event.values[0] = c;

			event.values[1] = -event.values[1];*/

			//Log.d(TAG, String.format("%6.3f ", event.values[0]));

			//mMessageBundle.putFloatArray("SensorValues", event.values);
			mMessageBundle.putFloat("X", event.values[0]);
			mMessageBundle.putFloat("Y", event.values[1]);
			mMessageBundle.putFloat("Z", event.values[2]);
			mMessageBundle.putLong("Timestamp", event.timestamp);

			mSensorMessage.setData(mMessageBundle);

			mSensorHandler.sendMessage(mSensorMessage);
		}
	}

	static class SensorDataHandler extends Handler
	{
		WeakReference<MainActivity>	mSensorActivity;

		Decreaser					decreaser	= new Decreaser();

		SensorDataHandler(MainActivity sensorActivity)
		{
			mSensorActivity = new WeakReference<MainActivity>(sensorActivity);
		}

		@Override
		public void handleMessage(Message msg)
		{
			synchronized (this)
			{
				MainActivity sensorActivity = mSensorActivity.get();

				Bundle bundle = msg.getData();

				//float values[] = bundle.getFloatArray("SensorValues");
				float values[] = new float[AXES];
				values[0]= bundle.getFloat("X");
				values[1]= bundle.getFloat("Y");
				values[2]= bundle.getFloat("Z");
				
				//Log.w(TAG, String.format("Appear %6.3f ", values[0]));

				String maxVal = "";
				String minVal = "";
				String averageVal = "";
				String rangeVal = "";
				String stateVal = "";

				boolean changed = false;

				svm_node[] svmInputs = new svm_node[SVM_INPUTS];
				for (int i = 0; i < SVM_INPUTS; i++)
				{
					svmInputs[i] = new svm_node();
					svmInputs[i].index = i + 1;
				}

				double averageValue;

				for (int i = 0; i < AXES; i++)
				{
					//average calculation
					{
						sensorActivity.mTotal[i] -= sensorActivity.mElementsList[i].removeAt(0);
						sensorActivity.mElementsList[i].add(values[i]);
						sensorActivity.mTotal[i] += values[i];

						averageValue = sensorActivity.mTotal[i] / (double) COUNTS;
						svmInputs[SVM_INPUT_MEAN + i].value = averageValue;//sensorActivity.mTotal[i] / COUNTS;

						averageVal += String.format(AXES_NAMES[i] + " %6.3f ", svmInputs[SVM_INPUT_MEAN + i].value);
					}
					//min calculation
					{
						sensorActivity.mMinMaxIndicesList[i][MIN].transformValues(decreaser);

						while (!sensorActivity.mMinMaxIndicesList[i][MIN].isEmpty()
								&& values[i] <= sensorActivity.mElementsList[i].get(sensorActivity.mMinMaxIndicesList[i][MIN]
										.get(sensorActivity.mMinMaxIndicesList[i][MIN].size() - 1)))
							sensorActivity.mMinMaxIndicesList[i][MIN].remove(sensorActivity.mMinMaxIndicesList[i][MIN].size() - 1, 1);

						sensorActivity.mMinMaxIndicesList[i][MIN].add(COUNTS - 1);

						svmInputs[SVM_INPUT_MIN + i].value = sensorActivity.mElementsList[i].get(sensorActivity.mMinMaxIndicesList[i][MIN].get(0));
						
						minVal += String.format(AXES_NAMES[i] + " %6.3f ", svmInputs[SVM_INPUT_MIN + i].value);

						if (!sensorActivity.mMinMaxIndicesList[i][MIN].isEmpty() && sensorActivity.mMinMaxIndicesList[i][MIN].get(0) == 0)
							sensorActivity.mMinMaxIndicesList[i][MIN].remove(0, 1);
					}

					//max calculation
					{
						sensorActivity.mMinMaxIndicesList[i][MAX].transformValues(decreaser);

						//if (i == 0 && values[i] > -1)
						//	Log.e(TAG, String.format("WTF %6.3f ", values[i]));

						while (!sensorActivity.mMinMaxIndicesList[i][MAX].isEmpty()
								&& values[i] >= sensorActivity.mElementsList[i].get(sensorActivity.mMinMaxIndicesList[i][MAX]
										.get(sensorActivity.mMinMaxIndicesList[i][MAX].size() - 1)))
							sensorActivity.mMinMaxIndicesList[i][MAX].remove(sensorActivity.mMinMaxIndicesList[i][MAX].size() - 1, 1);

						sensorActivity.mMinMaxIndicesList[i][MAX].add(COUNTS - 1);

						svmInputs[SVM_INPUT_MAX + i].value = sensorActivity.mElementsList[i].get(sensorActivity.mMinMaxIndicesList[i][MAX].get(0));


						sensorActivity.mRanges[i] = svmInputs[SVM_INPUT_MAX + i].value - svmInputs[SVM_INPUT_MIN + i].value;
						
						svmInputs[SVM_INPUT_RANGE + i].value = sensorActivity.mRanges[i];


						maxVal += String.format(AXES_NAMES[i] + " %6.3f ", svmInputs[SVM_INPUT_MAX + i].value);

						if (!sensorActivity.mMinMaxIndicesList[i][MAX].isEmpty() && sensorActivity.mMinMaxIndicesList[i][MAX].get(0) == 0)
							sensorActivity.mMinMaxIndicesList[i][MAX].remove(0, 1);
					}

					rangeVal += String.format(AXES_NAMES[i] + " %6.3f ", sensorActivity.mRanges[i]);

				}

				/*
				int type = bundle.getInt("SensorType"); 
				if (type == Sensor.TYPE_ACCELEROMETER)
				{

				}
				else if (type == Sensor.TYPE_GYROSCOPE)
				{

				}*/

				//if (sensorActivity.mNotFirst)
				{
					for (int i = 0; i < SVM_INPUTS; i++)
						svmInputs[i].value =
								((svmInputs[i].value - sensorActivity.mGlobalMinMaxRange[i][MIN]) / sensorActivity.mGlobalMinMaxRange[i][RANGE]) * 2.0 - 1.0;

					//svm fall or adl
					sensorActivity.SvmPredictionCombined = svm.svm_predict(sensorActivity.mCombinedFallDetectionModel, svmInputs);
					switch ((int) sensorActivity.SvmPredictionCombined)
					{
						case 1:
							stateVal = ACTIVITIES[0];
							break;
						case -1:
							stateVal = ACTIVITIES[1];
							break;
					}

					if (sensorActivity.SvmPredictionCombined != sensorActivity.SvmPredictionCombinedPrev)
					{
						changed = true;

						switch ((int) sensorActivity.SvmPredictionCombined)
						{
							case -1:
								if (sensorActivity.mBeep)
									sensorActivity.mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
								break;
						}

					}
					sensorActivity.SvmPredictionCombinedPrev = sensorActivity.SvmPredictionCombined;

					sensorActivity.SvmPredictionClassified = svm.svm_predict(sensorActivity.mClassifiedFallDetectionModel, svmInputs);
					if (sensorActivity.SvmPredictionClassified > 0)
					{
						switch ((int) sensorActivity.SvmPredictionClassified)
						{
							case 1:
								stateVal += ACTIVITIES_ADL_CLASSES[0];
								break;
							case 2:
								stateVal += ACTIVITIES_ADL_CLASSES[1];
								break;
							case 3:
								stateVal += ACTIVITIES_ADL_CLASSES[2];
								break;
							case 4:
								stateVal += ACTIVITIES_ADL_CLASSES[3];
								break;
							case 5:
								stateVal += ACTIVITIES_ADL_CLASSES[4];
								break;
							case 6:
								stateVal += ACTIVITIES_ADL_CLASSES[5];
								break;
							case 7:
								stateVal += ACTIVITIES_ADL_CLASSES[6];
								break;
							case 8:
								stateVal += ACTIVITIES_ADL_CLASSES[7];
								break;
						}
					}
					else
					{

						switch ((int) sensorActivity.SvmPredictionClassified)
						{
							case -1:
								stateVal += ACTIVITIES_FALL_CLASSES[0];
								break;
							case -2:
								stateVal += ACTIVITIES_FALL_CLASSES[1];
								break;
							case -3:
								stateVal += ACTIVITIES_FALL_CLASSES[2];
								break;
							case -4:
								stateVal += ACTIVITIES_FALL_CLASSES[3];
								break;
							case -5:
								stateVal += ACTIVITIES_FALL_CLASSES[4];
								break;
							case -6:
								stateVal += ACTIVITIES_FALL_CLASSES[5];
								break;
							case -7:
								stateVal += ACTIVITIES_FALL_CLASSES[6];
								break;
						}
					}

					if (sensorActivity.SvmPredictionClassified != sensorActivity.SvmPredictionClassifiedPrev)
					{
						changed = true;

						if (sensorActivity.SvmPredictionClassified < 0)
						{

							if (sensorActivity.mBeep)
								sensorActivity.mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);

						}

					}
					sensorActivity.SvmPredictionClassifiedPrev = sensorActivity.SvmPredictionClassified;

					if (changed)
					{
						sensorActivity.mCurrentState.setText(stateVal);
						sensorActivity.mLogEditText.append(stateVal + "\n");
						if (sensorActivity.mTalkBack)
						{
							sensorActivity.mTts.speak(stateVal, TextToSpeech.QUEUE_FLUSH, null);
						}
					}
				}
				/*else
				{
					boolean changeNotFirstFlag = true;
					//we need to make sure that all ranges are not null
					for (int i = 0; i < SVM_INPUTS; i++)
						if (sensorActivity.mGlobalMinMaxRange[i][RANGE] == 0)
						{
							changeNotFirstFlag = false;
							break;
						}

					if (changeNotFirstFlag)
						sensorActivity.mNotFirst = true;
				}*/

				sensorActivity.mTimeStampsList.removeAt(0);
				//sensorActivity.mTimeStampsList.add(bundle.getLong("Timestamp"));
				sensorActivity.mTimeStampsList.add(System.nanoTime());

				if (sensorActivity.mUpdateCounter++ % UPDATE_COUNTER == 0)
				{
					sensorActivity.mMaxTextView.setText(maxVal);
					sensorActivity.mMinTextView.setText(minVal);
					sensorActivity.mRangeTextView.setText(rangeVal);

					sensorActivity.mAverageTextView.setText(averageVal);
					sensorActivity.mFrequencyTextView.setText(String.format("%7.3f Hz", COUNTS
							/ ((double) (sensorActivity.mTimeStampsList.get(COUNTS - 1) - sensorActivity.mTimeStampsList.get(0)) / 1000000000.0)));
				}
				
			
			}

		}

		private static class Decreaser implements TIntFunction
		{
			@Override
			public int execute(int v)
			{
				return v - 1;
			}
		}

	}

	// Implements TextToSpeech.OnInitListener.
	public void onInit(int status)
	{
		// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
		if (status == TextToSpeech.SUCCESS)
		{
			// Set preferred language to US english.
			// Note that a language may not be available, and the result will indicate this.
			int result = mTts.setLanguage(Locale.US);
			// Try this someday for some interesting results.
			// int result mTts.setLanguage(Locale.FRANCE);
			if (result == TextToSpeech.LANG_MISSING_DATA ||
					result == TextToSpeech.LANG_NOT_SUPPORTED)
			{
				// Lanuage data is missing or the language is not supported.
				Toast.makeText(this, "Language is not available.", Toast.LENGTH_SHORT).show();
			}
			else
			{
				// Check the documentation for other possible result codes.
				// For example, the language may be available for the locale,
				// but not for the specified country and variant.

				// The TTS engine has been successfully initialized.
				// Allow the user to press the button for the app to speak again.

			}
		}
		else
		{
			// Initialization failed.
			Toast.makeText(this, "Could not initialize TextToSpeech.", Toast.LENGTH_SHORT).show();
		}
	}
}
