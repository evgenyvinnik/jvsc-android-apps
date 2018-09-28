package ca.jvsh.grahambrown;

import ca.jvsh.grahambrown.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class TiledPatternLiveWallpaperSettings  extends PreferenceActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		getPreferenceManager().setSharedPreferencesName(TiledPatternLiveWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.tiled_pattern_settings);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	protected void onResume()
	{
		super.onResume();
	}

	protected void onDestroy()
	{
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
	}
}
