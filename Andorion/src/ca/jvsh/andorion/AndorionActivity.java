package ca.jvsh.andorion;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

public class AndorionActivity extends SherlockFragmentActivity
{
	public static final int GRID = 16;
	
	private static final String	TAG	= "AndorionActivity";
	private AndorionView		mAndorionView;

	/**
	 * Launch Home activity helper
	 * 
	 * @param c context where launch home from (used by SplashscreenActivity)
	 */
	public static void launch(Context c)
	{
		Intent intent = new Intent(c, AndorionActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		c.startActivity(intent);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final FrameLayout andorionViewLayer =
				(FrameLayout) findViewById(R.id.andorion_view);

		// Look up the AdView as a resource and load a request.
		AdView adView = (AdView) this.findViewById(R.id.adView);
		adView.loadAd(new AdRequest());

		//create andorion view
		mAndorionView = new AndorionView(this);

		andorionViewLayer.addView(mAndorionView);
	}

	private static final int	MENU_HELP			= 1;
	private static final int	MENU_PREFERENCES	= 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_HELP, 0, "Help")
				.setIcon(android.R.drawable.ic_menu_help)
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_IF_ROOM
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		menu.add(0, MENU_PREFERENCES, 0, "Preferences")
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_IF_ROOM
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == MENU_HELP)
		{
			// Launch the help activity as a subactivity.

			return true;
		}
		else if (item.getItemId() == MENU_PREFERENCES)
		{

			return true;
		}
		return super.onOptionsItemSelected(item);
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
		mAndorionView.onResume();
		// Just start straight away.
		mAndorionView.surfaceStart();

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
		mAndorionView.onPause();
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
		mAndorionView.onStop();
	}
}