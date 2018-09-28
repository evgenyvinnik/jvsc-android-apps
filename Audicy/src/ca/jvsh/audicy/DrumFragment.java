package ca.jvsh.audicy;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class DrumFragment extends Fragment
{
	static final int		CLEAR_ALL_MENU_ID	= Menu.FIRST;
	static final int		SMOKE_MENU_ID		= Menu.FIRST + 1;
	static final int		PALLETE_MENU_ID		= Menu.FIRST + 2;
	static final int		GRAYSCALE_MENU_ID	= Menu.FIRST + 3;
	static final int		ROUNDED_MENU_ID		= Menu.FIRST + 4;

	TenoriView				tenoriView;
	//external thread that updates game view
	private TenoriThread	mTenoriThread;
	private ChimeThread		mChimeThread;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		View v = inflater.inflate(R.layout.drum_fragment, container, false);
		View tv = v.findViewById(R.id.drumframeLayout);
		FrameLayout frameLayout = (FrameLayout) tv;

		tenoriView = new TenoriView(v.getContext());
		frameLayout.addView(tenoriView);
		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.add(Menu.NONE, CLEAR_ALL_MENU_ID, 0, "Clear all").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, SMOKE_MENU_ID, 0, "No waves").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, PALLETE_MENU_ID, 0, "Use pallete").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, GRAYSCALE_MENU_ID, 0, "Grayscale").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, ROUNDED_MENU_ID, 0, "Square edges").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(tenoriView == null)
			return super.onOptionsItemSelected(item);
		
		switch (item.getItemId())
		{
			case CLEAR_ALL_MENU_ID:
				tenoriView.clearSwitches();
				return true;
			case SMOKE_MENU_ID:
				tenoriView.smoke = !tenoriView.smoke;
				if(tenoriView.smoke)
				{
					item.setTitle("No waves");
				}
				else
				{
					item.setTitle("Sound waves");
				}
				return true;
			case PALLETE_MENU_ID:
				tenoriView.palette = !tenoriView.palette;
				if(tenoriView.palette)
				{
					item.setTitle("Use random");
				}
				else
				{
					item.setTitle("Use pallete");
				}
				return true;
			case GRAYSCALE_MENU_ID:
				tenoriView.color = !tenoriView.color;
				if(tenoriView.color)
				{
					item.setTitle("Grayscale");
				}
				else
				{
					item.setTitle("Color");
				}
				return true;
			case ROUNDED_MENU_ID:
				tenoriView.rounded = !tenoriView.rounded;
				if(tenoriView.rounded)
				{
					item.setTitle("Square edges");
				}
				else
				{
					item.setTitle("Rounded edges");
				}
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		if (mChimeThread == null)
		{
			mChimeThread = new ChimeThread();
			mChimeThread.start();
			tenoriView.setChimeThread(mChimeThread);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// start the background thread
		if (mTenoriThread == null)
		{
			mTenoriThread = new TenoriThread(tenoriView);
			mTenoriThread.start();
		}
	}

	@Override
	public void onPause()
	{
		//stop background thread
		if (mChimeThread != null)
		{
			tenoriView.setChimeThread(null);
			mChimeThread.requestStop();
			mChimeThread = null;
		}
		super.onPause();

	}

	@Override
	public void onStop()
	{
		if (mTenoriThread != null)
		{
			mTenoriThread.requestStop();
			mTenoriThread = null;
		}
		super.onStop();
	}
}
