package ca.jvsh.flute.activity;

import ca.jvsh.flute.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource.
		addPreferencesFromResource(R.xml.preferences);
	}
}
