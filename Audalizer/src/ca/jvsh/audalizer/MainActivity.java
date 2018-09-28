/**
 * org.hermit.android.core: useful Android foundation classes.
 * 
 * These classes are designed to help build various types of application.
 *
 * <br>Copyright 2009-2010 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package ca.jvsh.audalizer;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * An enhanced Activity class, for use as the main activity of an application.
 * The main thing this class provides is a nice callback-based mechanism
 * for starting sub-activities.  This makes it easier for different parts
 * of an app to kick off sub-activities and get the results.
 * 
 * <p>Note: it is best that sub-classes do not implement
 * onActivityResult(int, int, Intent).  If they do, then for safety use
 * small request codes, and call super.onActivityResult(int, int, Intent)
 * when you get an unknown code.
 *
 * @author Ian Cameron Smith
 */
public class MainActivity extends Activity
{

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

	// ******************************************************************** //
	// Activity Lifecycle.
	// ******************************************************************** //

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
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		errorReporter = Errors.getInstance(this);
	}

	// ******************************************************************** //
	// Exception Reporting.
	// ******************************************************************** //

	/**
	 * Report an unexpected exception to the user by popping up a dialog
	 * with some debug info.  Don't report the same exception more than twice,
	 * and if we get floods of exceptions, just bomb out.
	 * 
	 * <p>This method may be called from any thread.  The reporting will be
	 * deferred to the UI thread.
	 * 
	 * @param   e           The exception.
	 */
	public void reportException(final Exception e)
	{
		errorReporter.reportException(e);
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
	// Private Data.
	// ******************************************************************** //

	// Exception reporter.
	private Errors								errorReporter;

	// The next request code available to be used.  Our request codes
	// start at a large number, for no special reason.
	private int									nextRequest	= 0x60000000;

	// This map translates request codes to the listeners registered for
	// those requests.  It is used when a response is received to activate
	// the correct listener.
	private HashMap<Integer, ActivityListener>	codeMap		= new HashMap<Integer, ActivityListener>();

}
