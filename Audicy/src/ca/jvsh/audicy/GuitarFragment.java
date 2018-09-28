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

public class GuitarFragment extends Fragment
{

	static final int	GUITAR_MENU_ID	= Menu.FIRST;
	GuitarView			guitarView;
	//flag that determine guitar type
	boolean				electric		= true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		View v = inflater.inflate(R.layout.guitar_fragment, container, false);
		View tv = v.findViewById(R.id.guitarframeLayout);
		FrameLayout frameLayout = (FrameLayout) tv;

		guitarView = new GuitarView(v.getContext());
		frameLayout.addView(guitarView);
		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.add(Menu.NONE, GUITAR_MENU_ID, 0, "Switch to Acoustic Guitar").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		//final ContentResolver cr = getActivity().getContentResolver();

		switch (item.getItemId())
		{
			case GUITAR_MENU_ID:
				electric = !electric;
				setGuitarType(electric);
				if (electric)
				{
					item.setTitle("Switch to Acoustic Guitar");
				}
				else
				{
					item.setTitle("Switch to Electric Guitar");
				}

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void setGuitarType(boolean electric)
	{
		guitarView.setGuitarType(electric);
	}

}
