/*package ca.jvsh.enemy;

import ca.jvsh.enemy.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class EnemyFleetLiveWallpaperSettings  extends PreferenceActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		getPreferenceManager().setSharedPreferencesName(EnemyFleetLiveWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.enemy_fleet_settings);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onDestroy()
	{
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
	}
}*/