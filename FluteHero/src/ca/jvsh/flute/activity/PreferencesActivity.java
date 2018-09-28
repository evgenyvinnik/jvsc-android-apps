package ca.jvsh.flute.activity;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import ca.jvsh.flute.R;
import android.os.Bundle;

public class PreferencesActivity extends SherlockPreferenceActivity
{

	private static final int	MENU_SAVE	= 1;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource.
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		menu.add(0, MENU_SAVE, 0, "Save").setIcon(android.R.drawable.ic_menu_save).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == MENU_SAVE)
		{
			// Launch the help activity as a subactivity.
			this.setResult(RESULT_OK);
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
