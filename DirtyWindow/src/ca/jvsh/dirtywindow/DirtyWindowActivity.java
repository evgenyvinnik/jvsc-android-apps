package ca.jvsh.dirtywindow;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class DirtyWindowActivity extends Activity
{
	private DirtyWindowView	mDirtyWindowView	= null;

	// advertisement
	private AdView			mAdView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// create main layout
		FrameLayout game = new FrameLayout(this);

		// create secondary layout pane for the toggle button
		LinearLayout toggleButtonLayout = new LinearLayout(this);
		toggleButtonLayout.setOrientation(LinearLayout.VERTICAL);
		toggleButtonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		
	
		// Create the adView
		mAdView = new AdView(this, AdSize.BANNER, "a14f0faf02554a1");

		toggleButtonLayout.addView(mAdView);// add to layout

		// Initiate a generic request to load it with an ad
		mAdView.loadAd(new AdRequest());

		// create main game view
		mDirtyWindowView = new DirtyWindowView(this);

		// finalize application layout
		game.addView(mDirtyWindowView);
		game.addView(toggleButtonLayout);
		setContentView(game);
	}

	@Override
	public void onDestroy()
	{
		mAdView.destroy();

		super.onDestroy();
	}
}