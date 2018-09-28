package ca.jvsh.flute.activity;

import java.util.Random;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FluteView extends SurfaceView implements MultiTouchObjectCanvas<Object>, SurfaceHolder.Callback
{
	private MultiTouchController<Object>	multiTouchController;
	private PointInfo						mCurrTouchPoint;

	private static final int[]				mTouchPointColors	= { 0xFFF2E3B6, 0xFFBCD5B0, 0xFF76A68B, 0xFF898C70, 0xFFBF5F56, 0xFF66202C, 0xFF8F4A3C, 0xFFAB7245, 0xFFB59E65, 0xFFD4CE76 };

	private final Paint						mPaint				= new Paint();

	//private int[]							mTouchPointColors	= new int[MultiTouchController.MAX_TOUCH_POINTS];

	// Width, height and pixel format of the surface.
	private int								canvasWidth			= 0;
	private int								canvasHeight		= 0;

	// Application handle.
	private Context							appContext;

	// The surface manager for the view.
	private SurfaceHolder					surfaceHolder		= null;

	// Debugging tag.
	private static final String				TAG					= "FluteView";

	// Enable flags.  In order to run, we need onSurfaceCreated() and
	// onResume(), which can come in either order.  So we track which ones
	// we have by these flags.  When all are set, we're good to go.  Note
	// that this is distinct from the game state machine, and its pause
	// and resume actions -- the whole game is enabled by the combination
	// of these flags set in enableFlags.
	private static final int				ENABLE_SURFACE		= 0x01;
	private static final int				ENABLE_SIZE			= 0x02;
	private static final int				ENABLE_RESUMED		= 0x04;
	private static final int				ENABLE_STARTED		= 0x08;
	private static final int				ENABLE_FOCUSED		= 0x10;
	private static final int				ENABLE_ALL			= ENABLE_SURFACE | ENABLE_SIZE | ENABLE_RESUMED | ENABLE_STARTED | ENABLE_FOCUSED;

	// The time in ms to sleep each time round the main animation loop.
	// If zero, we will not sleep, but will run continuously.
	private long							animationDelay		= 0;

	// Enablement flags; see comment above.
	private int								enableFlags			= 0;

	// The ticker thread which runs the animation.  null if not active.
	private Ticker							animTicker			= null;

	//////////////////////////////////////////////////////////////////////
	//clarinet variables
	//////////////////////////////////////////////////////////////////////

	private AudioTrack						mAudioOutput;
	private int								mOutBufferSize;

	private int								mInBufferSize;
	private AudioRecord						mAudioInput;

	private int								mBufferSize;

	int										fs					= 44100;


	short[]									buffer;
	short[]									inputBuffer;

	private static boolean					mActive				= false;

	Thread									soundThread;
	
	

	private static final float				MAX_16_BIT			= 32768;

	float									power;

	//////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////
	//power gauge
	///////////////////////////////////////////////////////////////////////////

	// Number of peaks we will track in the VU meter.
	private static final int				METER_PEAKS			= 4;

	// Time in ms over which peaks in the VU meter fade out.
	private static final int				METER_PEAK_TIME		= 800;

	// Number of updates over which we average the VU meter to get
	// a rolling average.  32 is about 2 seconds.
	private static final int				METER_AVERAGE_COUNT	= 4;

	// Colours for the meter power bar and average bar and peak marks.
	// In METER_PEAK_COL, alpha is set dynamically in the code.
	private static final int				METER_POWER_COL		= 0xff796B7D;
	private static final int				METER_AVERAGE_COL	= 0xa045334A;
	private static final int				METER_PEAK_COL		= 0x00FFA3A3;
	private static final int				METER_SOUND_BARRIER	= 0xa011A883;
	
	int NOTES = 15;
	private static final int[]				mNotesColors	= { 0xFF7B5A9F, 0xFFFF86A4, 0xFFFFEB75, 0xFF51E8DB,
		
																0xFF6AA690, 0xFFF2BC1B, 0xFFF2DC99, 0xFFF29057,
																0xFFBF1F1F, 0xFFE87C71, 0xFF53C2A8, 0xFFFFEBA3,
																0xFFBFFFAF, 0xFF30F0FF, 0xFFFF9D27, 0xFFA62A16};


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Configured meter bar thickness.
	//private int								barHeight			= 32;

	// Display position and size within the parent view.
	//private int								dispX				= 0;
	//private int								dispY				= 0;
	//private int					dispWidth			= 0;
	//private int								dispHeight			= 0;

	// Label text size for the gauge.  Zero if not set yet.
	//private float							labelSize			= 0f;

	// Layout parameters for the VU meter.  Position and size for the
	// bar itself; position and size for the bar labels; position
	// and size for the main readout text.
	//private float							meterBarTop			= 0;
	//private float							meterBarGap			= 0;
	//private float							meterLabX			= 0;
	//private float							meterBarMargin		= 0;

	// Current and previous power levels.
	private float							currentPower		= 0f;
	private float							prevPower			= 0f;

	// Buffered old meter levels, used to calculate the rolling average.
	// Index of the most recent value.
	private float[]							powerHistory		= null;
	private int								historyIndex		= 0;

	// Rolling average power value,  calculated from the history buffer.
	private float							averagePower		= 1.0f;

	// Peak markers in the VU meter, and the times for each one.  A zero
	// time indicates a peak not set.
	private float[]							meterPeaks			= null;
	private long[]							meterPeakTimes		= null;
	private float							meterPeakMax		= 0f;

	// The paint we use for drawing.
	private Paint							powerMeterPaint		= null;

	//////////////////////////////////////////////////////////////////////////


	//////////////////////////////////////////////////////////////////////////
	//
	int										toneHoleRadius		= 80;
	int										dotRadius			= 50;

	private final static int				TONE_HOLES			= 4;
	int[]									ToneHolesColors		= new int[] { 0xFF4D9453, 0xFFA62A16, 0xFFADDE4E, 0xFFFF9D27 };
	float[]									ToneHolesX			= new float[TONE_HOLES];
	float[]									ToneHolesY			= new float[TONE_HOLES];

	int										ToneHoleCovered;


	float[]									ClarinetNoteFrequencies		= new float[] { 440.0f, 987.77f, 932.33f, 880.0f, 830.61f, 783.99f, 739.99f, 698.46f, 659.26f, 622.25f, 587.33f, 554.37f, 523.25f, 493.88f, 466.16f, 440.0f };
	//int[] NoteFrequencies = new int[]{66, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66};
	String[]								ClarinetNoteStrings			= new String[] { "", "B5", "A#5", "A5", "G#5", "G5", "F#5", "F5", "E5", "D#5", "D5", "C#5", "C5", "B4", "A#4", "A4" };

	float[]									FluteNoteFrequencies		= new float[] {  220.0f,  	554.36f, 	523.25f, 	493.88f, 	466.16f,   440.0f, 415.3f, 391.99f, 369.99f, 349.22f, 329.62f, 311.12f, 293.66f, 277.18f, 261.62f, 246.94f,  };
	String[]								FluteNoteStrings			= new String[] { "", 	"C#5",  	"C5", 		"B4", 		"A#4",    "A4", "G#4", "G4", "F#4", "F4", "E4", "D#4", "D4", "C#4", "C4", "B3", };

	
	String									NoteString			= "";
	//float									noteFrequency		= 440.0f;
	float									noteFrequency		= 440.0f;
	boolean									pressed				= false;

	//
	/////////////////////////////////////////////////////////////////////////
	
	
	/////////////////////////////////////////////////////////////////////////
	//Flute
	////////////////////////////////////////////////////////////////////////

	//private final static Random	rand	= new Random();

		
	Clarinet clarinet;
	Flute flute;
	
	boolean instrument = true;
	boolean view = false;

	
	/////////////////////////////////////////////////////////////////////////

	public FluteView(Context context)
	{
		this(context, null);
	}

	public FluteView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);

	}

	public FluteView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		appContext = context;

		multiTouchController = new MultiTouchController<Object>(this);
		mCurrTouchPoint = new PointInfo();

		/*for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
		{
			mTouchPointColors[i] = i < TOUCH_COLORS.length ? TOUCH_COLORS[i] : (int) (Math.random() * 0xffffff) + 0xff000000;
		}*/

		// Register for events on the surface.
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		setFocusable(true);
		setFocusableInTouchMode(true);

		mOutBufferSize = AudioTrack.getMinBufferSize(fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		mInBufferSize = AudioRecord.getMinBufferSize(fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

		mAudioOutput = new AudioTrack(AudioManager.STREAM_MUSIC, fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * mOutBufferSize, AudioTrack.MODE_STREAM);
		mAudioInput = new AudioRecord(MediaRecorder.AudioSource.MIC, fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * mInBufferSize);

		mBufferSize = (int) Math.min(mOutBufferSize, mInBufferSize);

		inputBuffer = new short[mBufferSize];
		buffer = new short[mBufferSize];
		
		clarinet = new Clarinet(mBufferSize);
		flute = new Flute(mBufferSize);

		meterPeaks = new float[METER_PEAKS];
		meterPeakTimes = new long[METER_PEAKS];

		// Create and initialize the history buffer.
		powerHistory = new float[METER_AVERAGE_COUNT];
		for (int i = 0; i < METER_AVERAGE_COUNT; ++i)
			powerHistory[i] = 0.0f;
		averagePower = 0.0f;

		// Set up our paint.
		mPaint.setAntiAlias(true);
		powerMeterPaint = new Paint();
		powerMeterPaint.setAntiAlias(true);
		powerMeterPaint.setStrokeWidth(4.0f);

	}
	
	public void switchInstrument()
	{
		instrument = !instrument;
	}
	
	public void switchView()
	{
		view = !view;
		
		if(view == true)
		{
	
			try
			{
				mAudioInput.stop();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Can't stop recording");
				return;
			}
		}
		else
		{

			try
			{
				mAudioInput.startRecording();
			}
			catch (Exception e)
			{
				Log.e(TAG, "Failed to start recording");
				
				return;
			}
		}
	}

	protected void animStart()
	{
		mActive = true;
		soundThread = new Thread()
		{
			public void run()
			{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				try
				{
					mAudioOutput.play();
				}
				catch (Exception e)
				{
					Log.e(TAG, "Failed to start playback");
					return;
				}

				try
				{
					mAudioInput.startRecording();
				}
				catch (Exception e)
				{
					Log.e(TAG, "Failed to start recording");
					mAudioOutput.stop();
					return;
				}
				//flute();
				
				instrumentSound();
				//mAudioOutput.stop();
			}
		};
		soundThread.start();
	}

	protected void animStop()
	{
		mActive = false;
		soundThread = null;
	}

	void instrumentSound()
	{
		
		//float pm_prev = multiplier;
		//float nu = .05f;
		
		try
		{

			while (mActive)
			{
				if(view)
				{
					if(instrument)
					{
						flute.flute(buffer, mBufferSize, pressed ? 1.0f : 0, noteFrequency, fs);
					}
					else
					{
						clarinet.clarinet( buffer, mBufferSize, pressed ?1.0f : 0, noteFrequency, fs);
					}
				}
				else
				{
					mAudioInput.read(inputBuffer, 0, mBufferSize);
	
					// We need longs to avoid running out of bits.
					float sum = 0;
					float sqsum = 0;
					for (int i = 0; i < mBufferSize; i++)
					{
						final long v = inputBuffer[i];
						sum += v;
						sqsum += v * v;
					}
	
					power = (sqsum - sum * sum / mBufferSize) / mBufferSize;
					power /= MAX_16_BIT * MAX_16_BIT;
					updatePower(power);
					
					//int current_N = (int) Math.floor(fs / noteFrequency + 0.5f);
	
					if(instrument)
					{
						flute.flute(buffer, mBufferSize, pressed ? averagePower : 0, noteFrequency, fs);
					}
					else
					{
						clarinet.clarinet( buffer, mBufferSize, pressed ?averagePower : 0, noteFrequency, fs);
					}
				}
				//clarinet.clarinet(inputBuffer, buffer, mBufferSize, power, noteFrequency);
				
				/*int written=*/mAudioOutput.write(buffer, 0, mBufferSize);
				//Log.d(TAG, "Written " + written);
			}

		}
		catch (Exception e)
		{
			Log.d(TAG, "Error while recording, aborting.");
		}

		try
		{
			mAudioOutput.stop();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Can't stop playback");
			try
			{
				mAudioInput.stop();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Can't stop recording");
				return;
			}
			return;
		}

		try
		{
			mAudioInput.stop();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Can't stop recording");
			return;
		}

	}

/*	void flute()
	{
	

	

		

		
		
		//float[] lowOut = new float[mBufferSize];
		//float[] jetOut = new float[mBufferSize];
		//float[] sigOut = new float[mBufferSize];
		//float[][] venOut = new float[NRofVents][mBufferSize];
		//float[] staOut = new float[mBufferSize];

		//float[] pressure = new float[samples];
		//Input of the flute model
		//for (int i = 0; i < samples; i++)
		//	pressure[i] = noiseGain * 2 * (rand.nextFloat() - 0.5f);
		
		

		//int nAttack = 2000;
		//int nDecay = 5000;
		float[] inputAmpl = new float[samples];
		

		int m;
		for (m = 0; m < nAttack; m++)
			inputAmpl[m] = inputGain * m / nAttack;

		for (; m < samples - nAttack - nDecay; m++)
			inputAmpl[m] = inputGain;

		for (int j = 0; m < samples; m++, j++)
			inputAmpl[m] = inputGain * j / nDecay;

		// State variables


		try
		{
			while (mActive)
			{
				mAudioInput.read(inputBuffer, 0, mBufferSize);
				
				//y0_prev = y0[mBufferSize - 1];
				
				mAudioOutput.write(buffer, 0, mBufferSize);
			}
		}
		catch (Exception e)
		{
			Log.d(TAG, "Error while recording, aborting.");
		}

		try
		{
			mAudioOutput.stop();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Can't stop playback");
			try
			{
				mAudioInput.stop();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Can't stop recording");
				return;
			}
			return;
		}
		
		try
		{
			mAudioInput.stop();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Can't stop recording");
			return;
		}
		
		
		//for (int i = 0; i < samples; i++)
		//{
			// Open and close the vent. Note that we also adjust the jet line length
			//% by moving the place from which samples are taken from the jetline.
			//%
			//% VENTSTATE:  Current vent openess value
			//% VENTTARGET: Desired vent openess value
			//
			if (i == openVentHere)
			{
				jetLength = jetLength2;
				ventTarget = 1;
			}
			else if (i == closeVentHere)
			{
				jetLength = jetLength1;
				ventTarget = 0;
			}

			//open vent
			//if (ventTarget > ventState)
			//	ventState = Math.min(ventTarget, ventState + ventStep);
			//close vent
			//else if (ventTarget < ventState)
			//	ventState = Math.max(ventTarget, ventState - ventStep);

			//			
			//% Below is an exponential vent regulator, but it does not function well
			//% when the vent is closing.
			//%
			//% VENTSTATE = VENTSTATE + (VENTTARGET - VENTSTATE) / 10.0;
			//
			

		//}

	}*/

	

	
	// ******************************************************************** //
	// State Handling.
	// ******************************************************************** //

	/**
	 * Set the delay in ms in each iteration of the main loop.
	 * 
	 * @param   delay       The time in ms to sleep each time round the main
	 *                      animation loop.  If zero, we will not sleep,
	 *                      but will run continuously.
	 *                      
	 *                      <p>If you want to do all your animation under
	 *                      direct app control using {@link #postUpdate()},
	 *                      just set a large delay.  You may want to consider
	 *                      using 1000 -- i.e. one second -- to make sure
	 *                      you get a refresh at a decent interval.
	 */
	public void setDelay(long delay)
	{
		Log.i(TAG, "setDelay " + delay);
		animationDelay = delay;
	}

	/**
	 * This is called immediately after the surface is first created.
	 * Implementations of this should start up whatever rendering code
	 * they desire.
	 * 
	 * Note that only one thread can ever draw into a Surface, so you
	 * should not draw into the Surface here if your normal rendering
	 * will be in another thread.
	 * 
	 * @param	holder		The SurfaceHolder whose surface is being created.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		setEnable(ENABLE_SURFACE, "surfaceCreated");
	}

	/**
	 * This is called immediately after any structural changes (format or
	 * size) have been made to the surface.  This method is always
	 * called at least once, after surfaceCreated(SurfaceHolder).
	 * 
	 * @param	holder		The SurfaceHolder whose surface has changed.
	 * @param	format		The new PixelFormat of the surface.
	 * @param	width		The new width of the surface.
	 * @param	height		The new height of the surface.
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{

		setSize(format, width, height);
		setEnable(ENABLE_SIZE, "set size " + width + "x" + height);
	}

	/**
	 * This is called immediately before a surface is destroyed.
	 * After returning from this call, you should no longer try to
	 * access this surface.  If you have a rendering thread that directly
	 * accesses the surface, you must ensure that thread is no longer
	 * touching the Surface before returning from this function.
	 * 
	 * @param	holder		The SurfaceHolder whose surface is being destroyed.
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		clearEnable(ENABLE_SURFACE, "surfaceDestroyed");

	}

	/**
	 * The application is starting.  Applications must call this from their
	 * Activity.onStart() method.
	 */
	public void onStart()
	{
		Log.i(TAG, "onStart");

	}

	/**
	 * We're resuming the app.  Applications must call this from their
	 * Activity.onResume() method.
	 */
	public void onResume()
	{
		setEnable(ENABLE_RESUMED, "onResume");
	}

	/**
	 * Start the surface running.  Applications must call this to set
	 * the surface going.  They may use this to implement their own level
	 * of start/stop control, for example to implement a "pause" button.
	 */
	public void surfaceStart()
	{
		setEnable(ENABLE_STARTED, "surfaceStart");
	}

	/**
	 * Stop the surface running.  Applications may call this to stop
	 * the surface running.  They may use this to implement their own level
	 * of start/stop control, for example to implement a "pause" button.
	 */
	public void surfaceStop()
	{
		clearEnable(ENABLE_STARTED, "surfaceStop");
	}

	/**
	 * Pause the app.  Applications must call this from their
	 * Activity.onPause() method.
	 */
	public void onPause()
	{
		clearEnable(ENABLE_RESUMED, "onPause");
	}

	/**
	 * The application is closing down.  Applications must call
	 * this from their Activity.onStop() method.
	 */
	public void onStop()
	{
		Log.i(TAG, "onStop()");

		// Make sure we're paused.
		onPause();
	}

	/**
	 * Handle changes in focus.  When we lose focus, pause the game
	 * so a popup (like the menu) doesn't cause havoc.
	 * 
	 * @param	hasWindowFocus		True iff we have focus.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus)
	{
		if (!hasWindowFocus)
			clearEnable(ENABLE_FOCUSED, "onWindowFocusChanged");
		else
			setEnable(ENABLE_FOCUSED, "onWindowFocusChanged");
	}

	/**
	 * Set the given enable flag, and see if we're good to go.
	 * 
	 * @param   flag        The flag to set.
	 * @param   why         Short tag explaining why, for debugging.
	 */
	private void setEnable(int flag, String why)
	{
		boolean enabled1 = false;
		boolean enabled2 = false;
		synchronized (surfaceHolder)
		{
			enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
			enableFlags |= flag;
			enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;

			Log.i(TAG, "EN + " + why + " -> " + enableString());
		}

		// Are we all set?
		if (!enabled1 && enabled2)
			startRun();
	}

	/**
	 * Clear the given enable flag, and see if we need to shut down.
	 * 
	 * @param   flag        The flag to clear.
	 * @param   why         Short tag explaining why, for debugging.
	 */
	private void clearEnable(int flag, String why)
	{
		boolean enabled1 = false;
		boolean enabled2 = false;
		synchronized (surfaceHolder)
		{
			enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
			enableFlags &= ~flag;
			enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;

			Log.i(TAG, "EN - " + why + " -> " + enableString());
		}

		// Do we need to stop?
		if (enabled1 && !enabled2)
			stopRun();
	}

	/**
	 * Get the current enable state as a string for debugging.
	 * 
	 * @return              The current enable state as a string.
	 */
	private String enableString()
	{
		char[] buf = new char[5];
		buf[0] = (enableFlags & ENABLE_SURFACE) != 0 ? 'S' : '-';
		buf[1] = (enableFlags & ENABLE_SIZE) != 0 ? 'Z' : '-';
		buf[2] = (enableFlags & ENABLE_RESUMED) != 0 ? 'R' : '-';
		buf[3] = (enableFlags & ENABLE_STARTED) != 0 ? 'A' : '-';
		buf[4] = (enableFlags & ENABLE_FOCUSED) != 0 ? 'F' : '-';

		return String.valueOf(buf);
	}

	/**
	 * Start the animation running.  All the conditions we need to
	 * run are present (surface, size, resumed).
	 */
	private void startRun()
	{
		synchronized (surfaceHolder)
		{

			// Tell the subclass we're running.
			try
			{
				animStart();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (animTicker != null && animTicker.isAlive())
				animTicker.kill();
			Log.i(TAG, "set running: start ticker");
			//animTicker = !optionSet(LOOPED_TICKER) ? new ThreadTicker() : new LoopTicker();
			animTicker = new LoopTicker();
		}
	}

	/**
	 * Stop the animation running.  Our surface may have been destroyed, so
	 * stop all accesses to it.  If the caller is not the ticker thread,
	 * this method will only return when the ticker thread has died.
	 */
	private void stopRun()
	{
		// Kill the thread if it's running, and wait for it to die.
		// This is important when the surface is destroyed, as we can't
		// touch the surface after we return.  But if I am the ticker
		// thread, don't wait for myself to die.
		Ticker ticker = null;
		synchronized (surfaceHolder)
		{
			ticker = animTicker;
		}
		if (ticker != null && ticker.isAlive())
		{
			if (onSurfaceThread())
				ticker.kill();
			else
				ticker.killAndWait();
		}
		synchronized (surfaceHolder)
		{
			animTicker = null;
		}

		// Tell the subclass we've stopped.
		try
		{
			animStop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	/**
	 * Asynchronously schedule an update; i.e. a frame of animation.
	 * This can only be called if the SurfaceRunner was created with
	 * the option LOOPED_TICKER.
	 */
	public void postUpdate()
	{
		synchronized (surfaceHolder)
		{
			if (!(animTicker instanceof LoopTicker))
				throw new IllegalArgumentException("Can't post updates" + " without LOOPED_TICKER set");
			LoopTicker ticker = (LoopTicker) animTicker;
			ticker.post();
		}
	}

	private void tick()
	{
		try
		{
			// And update the screen.
			refreshScreen();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Draw the game board to the screen in its current state, as a one-off.
	 * This can be used to refresh the screen.
	 */
	private void refreshScreen()
	{
		Canvas canvas = null;
		try
		{
			canvas = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder)
			{
				canvas.drawColor(Color.WHITE);

				if(view)
				{
					//draw slides
					{
						float part =   (float)canvasHeight / (float)NOTES;
						
						final float densityMultiplier = getContext().getResources().getDisplayMetrics().density;
						final float scaledPx = 35 * densityMultiplier;
						mPaint.setTextSize(scaledPx);
						
						for (int i = 0; i < NOTES; i++)
						{
							mPaint.setColor(mNotesColors[i]);
							canvas.drawRect(0,  i * part, canvasWidth, (i+1) * part , mPaint);
							
							
							mPaint.setColor(0xFFFFFFFF);
							if(instrument)
							{
								float length = mPaint.measureText(FluteNoteStrings[i+1]);
								canvas.drawText(FluteNoteStrings[i+1], (canvasWidth - length) / 2, (i + 1)  * part, mPaint);
							}
							else
							{
								float length = mPaint.measureText(ClarinetNoteStrings[i+1]);
								canvas.drawText(ClarinetNoteStrings[i+1], (canvasWidth - length) / 2, (i + 1)  * part, mPaint);
								
							}
						}
						ToneHoleCovered = 0;
						if (mCurrTouchPoint.isDown())
						{
							float x = mCurrTouchPoint.getX();
							float y= mCurrTouchPoint.getY();
							
							// Show touch circle
							mPaint.setColor(mTouchPointColors[0]);
							canvas.drawCircle(x, y, dotRadius/2, mPaint);
							
							for (int i = 0; i < NOTES; i++)
							{
								if( y > (i * part) &&  y < ((i+1) * part)  )
								{
									ToneHoleCovered = i+1;
									break;
								}
								
							}
		
						}
						else
						{
							ToneHoleCovered = 0;
						}
						
						
						pressed = ToneHoleCovered == 0 ? false : true;
						if(instrument)
						{
							NoteString = FluteNoteStrings[ToneHoleCovered];
							noteFrequency = FluteNoteFrequencies[ToneHoleCovered];
						}
						else
						{
							NoteString = ClarinetNoteStrings[ToneHoleCovered];
							noteFrequency = ClarinetNoteFrequencies[ToneHoleCovered];
						}
					}
				}
				else
				{
					//draw tone holes
					{
						for (int i = 0; i < TONE_HOLES; i++)
						{
							mPaint.setColor(ToneHolesColors[i]);
							canvas.drawCircle(ToneHolesX[i], ToneHolesY[i], toneHoleRadius, mPaint);
						
						}

					}
	
					int numPoints = mCurrTouchPoint.getNumTouchPoints();
	
					ToneHoleCovered = 0;
	
					if (mCurrTouchPoint.isDown())
					{
						float[] xs = mCurrTouchPoint.getXs();
						float[] ys = mCurrTouchPoint.getYs();
	
						for (int idx = 0; idx < numPoints; idx++)
						{
							// Show touch circles
							mPaint.setColor(mTouchPointColors[idx]);
							canvas.drawCircle(xs[idx], ys[idx], dotRadius, mPaint);
	
							for (int i = 0; i < TONE_HOLES; i++)
								if (((ToneHolesX[i] - xs[idx]) * (ToneHolesX[i] - xs[idx]) + (ToneHolesY[i] - ys[idx]) * (ToneHolesY[i] - ys[idx])) < toneHoleRadius * toneHoleRadius)
									ToneHoleCovered = ToneHoleCovered | (1 << i);
						}
					}
					else
					{
						ToneHoleCovered = 0;
					}
	
					pressed = ToneHoleCovered == 0 ? false : true;
					if(instrument)
					{
						NoteString = FluteNoteStrings[ToneHoleCovered];
						noteFrequency = FluteNoteFrequencies[ToneHoleCovered];
					}
					else
					{
						NoteString = ClarinetNoteStrings[ToneHoleCovered];
						noteFrequency = ClarinetNoteFrequencies[ToneHoleCovered];
					}
	
					final float densityMultiplier = getContext().getResources().getDisplayMetrics().density;
					final float scaledPx = 60 * densityMultiplier;
					mPaint.setTextSize(scaledPx);
					
					mPaint.setColor(0xFF94A200);
					float length = mPaint.measureText(NoteString);
					canvas.drawText(NoteString, (canvasWidth - length) / 2, canvasHeight / 2, mPaint);
				

					long now = System.currentTimeMillis();
	
					{
						// Re-calculate the peak markers.
						calculatePeaks(now, currentPower, prevPower);
	
						// Position parameters.
	
						/*final float my = dispY + meterBarMargin;
						final float mh = dispHeight - meterBarMargin * 2;
						final float bx = dispX + meterBarTop;
						final float bw = barHeight;
						final float gap = meterBarGap;
						final float bh = mh - 2f;*/
						powerMeterPaint.setStyle(Style.STROKE);
						powerMeterPaint.setColor(METER_SOUND_BARRIER);
						//canvas.drawRect(mx + 1, by + gap, mx + p + 1, by + bh - gap, paint);
						//canvas.drawRect(bx + gap, my + bh, bx + bw - gap, my + bh - p, drawPaint);
						canvas.drawCircle(canvasWidth / 2, canvasHeight, 0.15f * canvasWidth / 4, powerMeterPaint);
	
						//final float p = (currentPower) * bh;
						powerMeterPaint.setStyle(Style.FILL);
						powerMeterPaint.setColor(METER_POWER_COL);
						//canvas.drawRect(mx + 1, by + gap, mx + p + 1, by + bh - gap, paint);
						//canvas.drawRect(bx + gap, my + bh, bx + bw - gap, my + bh - p, drawPaint);
						canvas.drawCircle(canvasWidth / 2, canvasHeight, currentPower * canvasWidth / 4, powerMeterPaint);
	
						// Draw the average bar.
						//final float pa = (averagePower) * bh;
						powerMeterPaint.setStyle(Style.FILL);
						powerMeterPaint.setColor(METER_AVERAGE_COL);
						//canvas.drawRect(mx + 1, by + 1, mx + pa + 1, by + bh - 1, paint);
						//canvas.drawRect(bx + 1, my + bh, bx + bw - 1, my + bh - pa, drawPaint);
						canvas.drawCircle(canvasWidth / 2, canvasHeight, averagePower * canvasWidth / 4, powerMeterPaint);
						// Draw the power bar.
	
						// Now, draw in the peaks.
						powerMeterPaint.setStyle(Style.STROKE);
						for (int i = 0; i < METER_PEAKS; ++i)
						{
							if (meterPeakTimes[i] != 0)
							{
								// Fade the peak according to its age.
								long age = now - meterPeakTimes[i];
								float fac = 1f - ((float) age / (float) METER_PEAK_TIME);
								int alpha = (int) (fac * 255f);
								powerMeterPaint.setColor(METER_PEAK_COL | (alpha << 24));
								// Draw it in.
								canvas.drawCircle(canvasWidth / 2, canvasHeight, meterPeaks[i] * canvasWidth / 4, powerMeterPaint);
								//final float pp = (meterPeaks[i]) * bh;
								//canvas.drawRect(mx + pp - 1, by + gap, mx + pp + 3, by + bh - gap, paint);
								//canvas.drawRect(bx + gap, my + bh - pp + 1, bx + bw - gap, my + bh - pp - 3, drawPaint);
							}
						}
					}
				}
			}
		}
		finally
		{
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (canvas != null)
				surfaceHolder.unlockCanvasAndPost(canvas);
		}
	}

	/**
	 * Re-calculate the positions of the peak markers in the VU meter.
	 */
	private final void calculatePeaks(long now, float power, float prev)
	{
		// First, delete any that have been passed or have timed out.
		for (int i = 0; i < METER_PEAKS; ++i)
		{
			if (meterPeakTimes[i] != 0 && (meterPeaks[i] < power || now - meterPeakTimes[i] > METER_PEAK_TIME))
				meterPeakTimes[i] = 0;
		}

		// If the meter has gone up, set a new peak, if there's an empty
		// slot.  If there isn't, don't bother, because we would be kicking
		// out a higher peak, which we don't want.
		if (power > prev)
		{
			boolean done = false;

			// First, check for a slightly-higher existing peak.  If there
			// is one, just bump its time.
			for (int i = 0; i < METER_PEAKS; ++i)
			{
				if (meterPeakTimes[i] != 0 && meterPeaks[i] - power < 2.5)
				{
					meterPeakTimes[i] = now;
					done = true;
					break;
				}
			}

			if (!done)
			{
				// Now scan for an empty slot.
				for (int i = 0; i < METER_PEAKS; ++i)
				{
					if (meterPeakTimes[i] == 0)
					{
						meterPeaks[i] = power;
						meterPeakTimes[i] = now;
						break;
					}
				}
			}
		}

		// Find the highest peak value.
		meterPeakMax = 0f;
		for (int i = 0; i < METER_PEAKS; ++i)
			if (meterPeakTimes[i] != 0 && meterPeaks[i] > meterPeakMax)
				meterPeakMax = meterPeaks[i];
	}

	/**
	 * Determine whether the caller is on the surface's animation thread.
	 * 
	 * @return             The resource value.
	 */
	public boolean onSurfaceThread()
	{
		return Thread.currentThread() == animTicker;
	}

	/**
	 * Set the size of the table.
	 * 
	 * @param   format      The new PixelFormat of the surface.
	 * @param   width       The new width of the surface.
	 * @param   height      The new height of the surface.
	 */
	private void setSize(int format, int width, int height)
	{
		synchronized (surfaceHolder)
		{
			canvasWidth = width;
			canvasHeight = height;

			if (canvasWidth > 720)
			{
				toneHoleRadius = 50;
				dotRadius = 40;
			}



			ToneHolesX[0] = ToneHolesX[2] = toneHoleRadius;
			ToneHolesX[1] = ToneHolesX[3] = canvasWidth - toneHoleRadius;

			ToneHolesY[3] = canvasHeight / 2 - 2f * toneHoleRadius;
			ToneHolesY[2] = canvasHeight / 2 - 1.5f * toneHoleRadius;
			ToneHolesY[1] = canvasHeight / 2 + 1.5f * toneHoleRadius;
			ToneHolesY[0] = canvasHeight / 2 + 2 * toneHoleRadius;
		}
	}

	void updatePower(double power)
	{
		//synchronized (this)
		{
			//Log.d("PowerGauge", "power" + power);
			// Save the current level.  Clip it to a reasonable range.
			if (power > 1.0)
				power = 1.0;
			else if (power < 0.0)
				power = 0.0;
			currentPower = (float) power;

			// Get the previous power value, and add the new value into the
			// history buffer.  Re-calculate the rolling average power value.
			if (++historyIndex >= powerHistory.length)
				historyIndex = 0;
			prevPower = powerHistory[historyIndex];
			powerHistory[historyIndex] = (float) power;
			averagePower -= prevPower / METER_AVERAGE_COUNT;
			averagePower += (float) power / METER_AVERAGE_COUNT;

		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		((Activity) appContext).onTouchEvent(event);
		// Pass the event on to the controller
		return multiTouchController.onTouchEvent(event);
	}

	public Object getDraggableObjectAtPoint(PointInfo pt)
	{
		// IMPORTANT: to start a multitouch drag operation, this routine must
		// return non-null
		return this;
	}

	public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut)
	{
		// We aren't dragging any objects, so this doesn't do anything in this
		// app
	}

	public void selectObject(Object obj, PointInfo touchPoint)
	{
		// We aren't dragging any objects in this particular app, but this is
		// called when the point goes up (obj == null) or down (obj != null),
		// save the touch point info
		touchPointChanged(touchPoint);
	}

	public boolean setPositionAndScale(Object obj, PositionAndScale newObjPosAndScale, PointInfo touchPoint)
	{
		// Called during a drag or stretch operation, update the touch point
		// info
		touchPointChanged(touchPoint);
		return true;
	}

	/**
	 * Called when the touch point info changes, causes a redraw.
	 * 
	 * @param touchPoint
	 */
	private void touchPointChanged(PointInfo touchPoint)
	{
		// Take a snapshot of touch point info, the touch point is volatile
		mCurrTouchPoint.set(touchPoint);
		//invalidate();
	}

	// ******************************************************************** //
	// Private Classes.
	// ******************************************************************** //

	/**
	 * Base interface for the ticker we use to control the animation.
	 */
	private interface Ticker
	{
		// Stop this thread.  There will be no new calls to tick() after this.
		public void kill();

		// Stop this thread and wait for it to die.  When we return, it is
		// guaranteed that tick() will never be called again.
		// 
		// Caution: if this is called from within tick(), deadlock is
		// guaranteed.
		public void killAndWait();

		// Run method for this thread -- simply call tick() a lot until
		// enable is false.
		public void run();

		// Determine whether this ticker is still going.
		public boolean isAlive();
	}

	/**
	 * Looper-based ticker class.  This has the advantage that asynchronous
	 * updates can be scheduled by passing it a message.
	 */
	private class LoopTicker extends Thread implements Ticker
	{
		// Constructor -- start at once.
		private LoopTicker()
		{
			super("Surface Runner");
			Log.v(TAG, "Ticker: start");
			start();
		}

		// Post a tick.  An update will be done near-immediately on the
		// appropriate thread.
		public void post()
		{
			synchronized (this)
			{
				if (msgHandler == null)
					return;

				// Remove any delayed ticks.
				msgHandler.removeMessages(MSG_TICK);

				// Do a tick right now.
				msgHandler.sendEmptyMessage(MSG_TICK);
			}
		}

		// Stop this thread.  There will be no new calls to tick() after this.
		@Override
		public void kill()
		{
			Log.v(TAG, "LoopTicker: kill");

			synchronized (this)
			{
				if (msgHandler == null)
					return;

				// Remove any delayed ticks.
				msgHandler.removeMessages(MSG_TICK);

				// Do an abort right now.
				msgHandler.sendEmptyMessage(MSG_ABORT);
			}
		}

		// Stop this thread and wait for it to die.  When we return, it is
		// guaranteed that tick() will never be called again.
		// 
		// Caution: if this is called from within tick(), deadlock is
		// guaranteed.
		@Override
		public void killAndWait()
		{
			Log.v(TAG, "LoopTicker: killAndWait");

			if (Thread.currentThread() == this)
				throw new IllegalStateException("LoopTicker.killAndWait()" + " called from ticker thread");

			synchronized (this)
			{
				if (msgHandler == null)
					return;

				// Remove any delayed ticks.
				msgHandler.removeMessages(MSG_TICK);

				// Do an abort right now.
				msgHandler.sendEmptyMessage(MSG_ABORT);
			}

			// Wait for the thread to finish.  Ignore interrupts.
			if (isAlive())
			{
				boolean retry = true;
				while (retry)
				{
					try
					{
						join();
						retry = false;
					}
					catch (InterruptedException e)
					{
					}
				}
				Log.v(TAG, "LoopTicker: killed");
			}
			else
			{
				Log.v(TAG, "LoopTicker: was dead");
			}
		}

		@Override
		public void run()
		{
			Looper.prepare();

			msgHandler = new Handler()
			{
				@Override
				public void handleMessage(Message msg)
				{
					switch (msg.what)
					{
						case MSG_TICK:
							tick();
							if (!msgHandler.hasMessages(MSG_TICK))
								msgHandler.sendEmptyMessageDelayed(MSG_TICK, animationDelay);
							break;
						case MSG_ABORT:
							Looper.myLooper().quit();
							break;
					}
				}
			};

			// Schedule the first tick.
			msgHandler.sendEmptyMessageDelayed(MSG_TICK, animationDelay);

			// Go into the processing loop.
			Looper.loop();
		}

		// Message codes.
		private static final int	MSG_TICK	= 6;
		private static final int	MSG_ABORT	= 9;

		// Our message handler.
		private Handler				msgHandler	= null;
	}
}
