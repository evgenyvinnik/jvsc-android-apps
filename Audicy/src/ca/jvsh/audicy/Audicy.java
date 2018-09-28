package ca.jvsh.audicy;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.app.Activity;

import android.os.Bundle;
import android.widget.Toast;

public class Audicy extends Activity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		
		ActionBar.Tab drumTab = bar.newTab().setText("Drum Machine");
		ActionBar.Tab wobbleTab = bar.newTab().setText("Wobble Bass");
		ActionBar.Tab guitarTab = bar.newTab().setText("Guitar");

		drumTab.setTabListener(new TabListener<DrumFragment>(this, "drum", DrumFragment.class));
		wobbleTab.setTabListener(new TabListener<WobbleFragment>(this, "wobble", WobbleFragment.class));
		guitarTab.setTabListener(new TabListener<GuitarFragment>(this, "guitar", GuitarFragment.class));

		bar.addTab(drumTab);
		bar.addTab(guitarTab);
		bar.addTab(wobbleTab);
	}


	public static class TabListener<T extends Fragment> implements
			ActionBar.TabListener
	{
		private final Activity	mActivity;
		private final String	mTag;
		private final Class<T>	mClass;
		private final Bundle	mArgs;
		private Fragment		mFragment;

		public TabListener(Activity activity, String tag, Class<T> clz)
		{
			this(activity, tag, clz, null);
		}

		public TabListener(Activity activity, String tag, Class<T> clz,
				Bundle args)
		{
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			mArgs = args;

			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state. If so, deactivate it, because our
			// initial state is that a tab isn't shown.
			mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
			if (mFragment != null && /*!mFragment.isDetached()*/ !mFragment.isHidden())
			{
				FragmentTransaction ft = mActivity.getFragmentManager()
						.beginTransaction();
				//ft.detach(mFragment);
				ft.hide(mFragment);
				ft.commit();
			}
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft)
		{
			if (mFragment == null)
			{
				System.out.println("\n onTabSelected mFragment == null\n");

				mFragment = Fragment.instantiate(mActivity, mClass.getName(),
						mArgs);
				ft.add(android.R.id.content, mFragment, mTag);
			}
			else
			{
				System.out.println("\n onTabSelected mFragment not null\n");
				//ft.attach(mFragment);
				ft.show(mFragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft)
		{
			if (mFragment != null)
			{
				System.out.println("\n onTabUnselected detach mFragment\n");
				//ft.detach(mFragment);
				ft.hide(mFragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft)
		{
			Toast.makeText(mActivity, "Reselected!", Toast.LENGTH_SHORT).show();
		}
	}
}