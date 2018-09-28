package ca.jvsh.networkbenchmark.lite;

import java.util.HashMap;

import ca.jvsh.networkbenchmark.lite.R;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TabHost;

public class BenchmarkActivity extends SherlockFragmentActivity
{

	TabHost						mTabHost;
	TabManager					mTabManager;
	//private static final String	TAG	= "BenchmarkActivity";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_benchmark);
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabManager = new TabManager(this, mTabHost, R.id.realtabcontent);

		//create and add tabs
		mTabManager.addTab(mTabHost.newTabSpec("Server").setIndicator("Server"), ServerFragment.class, null);
		mTabManager.addTab(mTabHost.newTabSpec("Client").setIndicator("Client"), ClientFragment.class, null);
		mTabManager.addTab(mTabHost.newTabSpec("Editor").setIndicator("Editor"), EditorFragment.class, null);

		//switch to the previously saved tab
		switch (PreferenceManager.getDefaultSharedPreferences(this).getInt("tab_selected", 0))
		{
			case 2:
				mTabHost.setCurrentTab(2);
				break;
			case 1:
				mTabHost.setCurrentTab(1);
				break;
			case 0:
			default:
				mTabHost.setCurrentTab(0);
				break;
		}
	}

	//on activity stop we are saving the tab that was selected last
	public void onStop()
	{
		if (mTabHost != null)
		{
			// save result in the memory
			{
				Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
				editor.putInt("tab_selected", mTabHost.getCurrentTab());
				editor.commit();
			}
		}
		super.onStop();
	}

	/* This is a helper class that implements a generic mechanism for
	* associating fragments with the tabs in a tab host.  It relies on a
	* trick.  Normally a tab host has a simple API for supplying a View or
	* Intent that each tab will show.  This is not sufficient for switching
	* between fragments.  So instead we make the content part of the tab host
	* 0dp high (it is not shown) and the TabManager supplies its own dummy
	* view to show as the tab content.  It listens to changes in tabs, and takes
	* care of switch to the correct fragment shown in a separate content area
	* whenever the selected tab changes.
	*/
	public static class TabManager implements TabHost.OnTabChangeListener
	{
		private final FragmentActivity			mActivity;
		private final TabHost					mTabHost;
		private final int						mContainerId;
		private final HashMap<String, TabInfo>	mTabs	= new HashMap<String, TabInfo>();
		TabInfo									mLastTab;

		static final class TabInfo
		{
			private final String	tag;
			private final Class<?>	clss;
			private final Bundle	args;
			private Fragment		fragment;

			TabInfo(String _tag, Class<?> _class, Bundle _args)
			{
				tag = _tag;
				clss = _class;
				args = _args;
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory
		{
			private final Context	mContext;

			public DummyTabFactory(Context context)
			{
				mContext = context;
			}

			@Override
			public View createTabContent(String tag)
			{
				View v = new View(mContext);
				v.setMinimumWidth(0);
				v.setMinimumHeight(0);
				return v;
			}
		}

		public TabManager(FragmentActivity activity, TabHost tabHost, int containerId)
		{
			mActivity = activity;
			mTabHost = tabHost;
			mContainerId = containerId;
			mTabHost.setOnTabChangedListener(this);
		}

		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args)
		{
			tabSpec.setContent(new DummyTabFactory(mActivity));
			String tag = tabSpec.getTag();

			TabInfo info = new TabInfo(tag, clss, args);

			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state.  If so, deactivate it, because our
			// initial state is that a tab isn't shown.
			info.fragment = mActivity.getSupportFragmentManager().findFragmentByTag(tag);
			if (info.fragment != null && !info.fragment.isDetached())
			{
				FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
				ft.detach(info.fragment);
				ft.commit();
			}

			mTabs.put(tag, info);
			mTabHost.addTab(tabSpec);
		}

		@Override
		public void onTabChanged(String tabId)
		{
			TabInfo newTab = mTabs.get(tabId);
			if (mLastTab != newTab)
			{
				FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
				if (mLastTab != null)
				{
					if (mLastTab.fragment != null)
					{
						ft.detach(mLastTab.fragment);
					}
				}
				if (newTab != null)
				{
					if (newTab.fragment == null)
					{
						newTab.fragment = Fragment.instantiate(mActivity,
								newTab.clss.getName(), newTab.args);
						ft.add(mContainerId, newTab.fragment, newTab.tag);
					}
					else
					{
						ft.attach(newTab.fragment);
					}
				}

				mLastTab = newTab;
				ft.commit();
				mActivity.getSupportFragmentManager().executePendingTransactions();
			}
		}
	}

}
