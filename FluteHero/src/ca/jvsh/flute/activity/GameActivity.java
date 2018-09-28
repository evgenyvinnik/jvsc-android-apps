package ca.jvsh.flute.activity;

import java.util.HashMap;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

import ca.jvsh.flute.activity.PreferencesActivity;

import ca.jvsh.flute.R;
import ca.jvsh.flute.util.ButtonLayerView;
import ca.jvsh.flute.util.GestureInterpreter;
import ca.jvsh.flute.util.WidgetFader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public class GameActivity extends SherlockFragmentActivity
{
	private static final int					MENU_HELP			= 1;
	private static final int					MENU_PREFERENCES	= 2;
	private static final int					MENU_INSTRUMENT = 3;
	private static final int					MENU_VIEW = 4;

	private GestureDetector						gestureDetector;

	private static final String					TAG					= "Audalyzer";
	private FluteView							mFluteView;

	// The next request code available to be used.  Our request codes
	// start at a large number, for no special reason.
	private int									nextRequest			= 0x6000;

	// This map translates request codes to the listeners registered for
	// those requests.  It is used when a response is received to activate
	// the correct listener.
	private HashMap<Integer, ActivityListener>	codeMap				= new HashMap<Integer, ActivityListener>();

	//external thread that updates game view
	//private CoinThread		mCoinThread;

	/**
	 * Called when the activity is starting.  This is where most
	 * initialisation should go: calling setContentView(int) to inflate
	 * the activity's UI, etc.
	 * 
	 * You can call finish() from within this function, in which case
	 * onDestroy() will be immediately called without any of the rest of
	 * the activity lifecycle executing.
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
	 * 
	 * @param   icicle          If the activity is being re-initialised
	 *                          after previously being shut down then this
	 *                          Bundle contains the data it most recently
	 *                          supplied in onSaveInstanceState(Bundle).
	 *                          Note: Otherwise it is null.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light_DarkActionBar);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game);

		// Restore our preferences.
		updatePreferences();

		// We want the audio controls to control our sound volume.
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		final ButtonLayerView providerButtons = (ButtonLayerView) findViewById(R.id.layer_buttons_control);
		final WidgetFader layerControlFader = new WidgetFader(providerButtons, 2500);
		providerButtons.hide();
		final int numChildren = providerButtons.getChildCount();
		for (int i = 0; i < numChildren; ++i)
		{
			final ImageButton button = (ImageButton) providerButtons.getChildAt(i);
			button.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					layerControlFader.keepActive();
				}
			});
		}

		//  MapMover mapMover = new MapMover(model, controller, this, sharedPreferences);
		gestureDetector = new GestureDetector(this, new GestureInterpreter(new WidgetFader[] { layerControlFader }));

		// Look up the AdView as a resource and load a request.
		AdView adView = (AdView) this.findViewById(R.id.adView);
		adView.loadAd(new AdRequest());

		//create flute view
		mFluteView = new FluteView(this);
		final FrameLayout fluteLayer = (FrameLayout) findViewById(R.id.flute_view);
		fluteLayer.addView(mFluteView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		menu.add(0, MENU_PREFERENCES, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		menu.add(0, MENU_INSTRUMENT, 0, "Instrument").setIcon(android.R.drawable.ic_menu_edit).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		menu.add(0, MENU_VIEW, 0, "View").setIcon(android.R.drawable.ic_menu_slideshow).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == MENU_HELP)
		{
			// Launch the help activity as a subactivity.
			Intent hIntent = new Intent();
			hIntent.setClass(this, HelpActivity.class);
			startActivity(hIntent);
			return true;
		}
		else if (item.getItemId() == MENU_PREFERENCES)
		{

			// Launch the preferences activity as a subactivity, so we
			// know when it returns.
			Intent pIntent = new Intent();
			pIntent.setClass(this, PreferencesActivity.class);
			startActivityForResult(pIntent, new GameActivity.ActivityListener()
			{
				@Override
				public void onActivityFinished(int resultCode, Intent data)
				{
					updatePreferences();
				}
			});
			return true;
		}
		else if (item.getItemId() == MENU_INSTRUMENT)
		{
			mFluteView.switchInstrument();
		}
		else if (item.getItemId() == MENU_VIEW)
		{
			mFluteView.switchView();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Log.d(TAG, "Touch event " + event);
		// Either of the following detectors can absorb the event, but one
		// must not hide it from the other
		boolean eventAbsorbed = false;
		if (gestureDetector.onTouchEvent(event))
		{
			eventAbsorbed = true;
		}

		return eventAbsorbed;
	}

	/**
	 * Called after {@link #onCreate} or {@link #onStop} when the current
	 * activity is now being displayed to the user.  It will
	 * be followed by {@link #onRestart}.
	 */
	@Override
	protected void onStart()
	{
		Log.i(TAG, "onStart()");

		super.onStart();
		mFluteView.onStart();
	}

	/**
	 * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
	 * for your activity to start interacting with the user.  This is a good
	 * place to begin animations, open exclusive-access devices (such as the
	 * camera), etc.
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
	 */
	@Override
	protected void onResume()
	{
		Log.i(TAG, "onResume()");

		super.onResume();
		mFluteView.onResume();
		// Just start straight away.
		mFluteView.surfaceStart();

	}

	/**
	 * Called to retrieve per-instance state from an activity before being
	 * killed so that the state can be restored in onCreate(Bundle) or
	 * onRestoreInstanceState(Bundle) (the Bundle populated by this method
	 * will be passed to both).
	 * 
	 * @param   outState        A Bundle in which to place any state
	 *                          information you wish to save.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		Log.i(TAG, "onSaveInstanceState()");
		super.onSaveInstanceState(outState);
	}

	/**
	 * Called as part of the activity lifecycle when an activity is going
	 * into the background, but has not (yet) been killed.  The counterpart
	 * to onResume(). 
	 */
	@Override
	protected void onPause()
	{
		Log.i(TAG, "onPause()");

		super.onPause();
		mFluteView.onPause();
	}

	/**
	 * Called when you are no longer visible to the user.  You will next
	 * receive either {@link #onStart}, {@link #onDestroy}, or nothing,
	 * depending on later user activity.
	 */
	@Override
	protected void onStop()
	{
		Log.i(TAG, "onStop()");

		super.onStop();
		mFluteView.onStop();
	}

	// ******************************************************************** //
	// Preferences Handling.
	// ******************************************************************** //

	/**
	 * Read our application preferences and configure ourself appropriately.
	 */
	private void updatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Get the desired sample rate.
		int sampleRate = 8000;
		try
		{
			String srate = prefs.getString("sampleRate", null);
			sampleRate = Integer.valueOf(srate);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Pref: bad sampleRate");
		}
		if (sampleRate < 8000)
			sampleRate = 8000;
		Log.i(TAG, "Prefs: sampleRate " + sampleRate);
		//mFluteView.setSampleRate(sampleRate);

	}

	// ******************************************************************** //
	// Sub-Activities.
	// ******************************************************************** //

	/**
	 * Launch an activity for which you would like a result when it
	 * finished.  When this activity exits, the given ActivityListener
	 * will be invoked.
	 * 
	 * <p>Note that this method should only be used with Intent protocols
	 * that are defined to return a result.  In other protocols (such
	 * as ACTION_MAIN or ACTION_VIEW), you may not get the result when
	 * you expect.
	 * 
	 * As a special case, if you call startActivityForResult() during
	 * the initial onCreate() / onResume() of your activity, then your
	 * window will not be displayed until a result is returned back
	 * from the started activity.
	 * 
	 * This method throws ActivityNotFoundException if there was no
	 * Activity found to run the given Intent.
	 * 
	 * @param   intent          The intent to start.
	 * @param   listener        Listener to invoke when the activity returns.
	 */
	public void startActivityForResult(Intent intent, ActivityListener listener)
	{
		// If this listener doesn't yet have a request code, give it one,
		// and add it to the map so we can find it again.  On subsequent calls
		// we re-use the same code.
		if (listener.requestCode == 0)
		{
			listener.requestCode = nextRequest++;
			codeMap.put(listener.requestCode, listener);
		}

		// Start the sub-activity.
		startActivityForResult(intent, listener.requestCode);
	}

	// ******************************************************************** //
	// Activity Management.
	// ******************************************************************** //

	/**
	 * Called when an activity you launched exits, giving you the requestCode
	 * you started it with, the resultCode it returned, and any additional
	 * data from it.  The resultCode will be RESULT_CANCELED if the activity
	 * explicitly returned that, didn't return any result, or crashed during
	 * its operation.
	 * 
	 * @param	requestCode		The integer request code originally supplied
	 * 							to startActivityForResult(), allowing you to
	 * 							identify who this result came from.
	 * @param	resultCode		The integer result code returned by the child
	 * 							activity through its setResult().
	 * @param	data			Additional data to return to the caller.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		ActivityListener listener = codeMap.get(requestCode);
		if (listener == null)
			Log.e("MainActivity", "Unknown request code: " + requestCode);
		else
			listener.onActivityFinished(resultCode, data);
	}

	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //

	/**
	 * This interface defines a listener for sub-activity results.
	 */
	public static abstract class ActivityListener
	{

		/**
		 * Called when an activity you launched exits.
		 * 
		 * <p>Applications can override this to be informed when an activity
		 * finishes, either by an error, the user pressing "back", or
		 * normally, or whatever.  The default implementation calls either
		 * onActivityCanceled(), if resultCode == RESULT_CANCELED, or
		 * else onActivityResult().
		 * 
		 * @param	resultCode		The integer result code returned by the
		 * 							child activity through its setResult().
		 * @param	data			Additional data returned by the activity.
		 */
		public void onActivityFinished(int resultCode, Intent data)
		{
			if (resultCode == RESULT_CANCELED)
				onActivityCanceled(data);
			else
				onActivityResult(resultCode, data);
		}

		/**
		 * Called when an activity you launched exits with a result code
		 * of RESULT_CANCELED.  This will happen if the user presses "back",
		 * or if the activity returned that code explicitly, didn't return
		 * any result, or crashed during its operation.
		 * 
		 * <p>Applications can override this if they want to be separately
		 * notified of a RESULT_CANCELED.  It doesn't make sense to override
		 * both onActivityFinished() and this method.
		 * 
		 * @param	data			Additional data returned by the activity.
		 */
		public void onActivityCanceled(Intent data)
		{
		}

		/**
		 * Called when an activity you launched exits with a result code
		 * other than RESULT_CANCELED, giving you the resultCode it
		 * returned, and any additional data from it.
		 * 
		 * <p>Applications can override this if they want to be separately
		 * notified of a normal exit.  It doesn't make sense to override
		 * both onActivityFinished() and this method.
		 * 
		 * @param	resultCode		The integer result code returned by the
		 * 							child activity through its setResult().
		 * @param	data			Additional data returned by the activity.
		 */
		public void onActivityResult(int resultCode, Intent data)
		{
		}

		// This listener's request code.  This code is auto-assigned
		// the first time the listener is used, and is used to find it
		// from the response.
		private int	requestCode	= 0;

	}
}
