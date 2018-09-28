package ca.jvsh.flute.activity;

import ca.jvsh.flute.R;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class FluteHeroActivity extends FragmentActivity
{

	/**
	 * Launch Home activity helper
	 * 
	 * @param c context where launch home from (used by SplashscreenActivity)
	 */
	public static void launch(Context c)
	{
		Intent intent = new Intent(c, FluteHeroActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		c.startActivity(intent);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// find the button and add click method to it
		final Button gameLevelsButton = (Button) findViewById(R.id.button_game_levels);
		gameLevelsButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent hIntent = new Intent();
				hIntent.setClass(v.getContext(), GameLevelsActivity.class);
				startActivity(hIntent);
			}
		});
		
		// find the button and add click method to it
		final Button flutesButton = (Button) findViewById(R.id.button_flutes);
		flutesButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent hIntent = new Intent();
				hIntent.setClass(v.getContext(), FlutesActivity.class);
				startActivity(hIntent);
			}
		});
		
		// find the button and add click method to it
		final Button effectsButton = (Button) findViewById(R.id.button_effects);
		effectsButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent hIntent = new Intent();
				hIntent.setClass(v.getContext(), EffectsActivity.class);
				startActivity(hIntent);
			}
		});
		
		// find the button and add click method to it
		final Button scoreloopButton = (Button) findViewById(R.id.button_scoreloop);
		scoreloopButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent hIntent = new Intent();
				hIntent.setClass(v.getContext(), ca.jvsh.flute.scoreloop.ScoreloopActivity.class);
				startActivity(hIntent);
			}
		});
	}

	private static final int	MENU_HELP			= 1;
	private static final int	MENU_PREFERENCES	= 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_HELP, 0, "Help").setIcon(R.drawable.ic_menu_help).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		menu.add(0, MENU_PREFERENCES, 0, "Preferences").setIcon(R.drawable.ic_menu_preferences).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

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
			Intent pIntent = new Intent();
			pIntent.setClass(this, PreferencesActivity.class);
			startActivity(pIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}