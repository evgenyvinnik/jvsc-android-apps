package ca.jvsh.flute.activity;

import java.util.ArrayList;


import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import ca.jvsh.flute.R;
import ca.jvsh.flute.designer.FluteDesigningSurface;
import android.graphics.PointF;
import android.os.Bundle;

public class FluteDesignerActivity extends SherlockFragmentActivity
{
	private static final int	MENU_TEST	= 1;
	private FluteDesigningSurface surface;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light_DarkActionBar);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.flute_designer);

		surface = (FluteDesigningSurface) findViewById(R.id.surface);

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		menu.add(0, MENU_TEST, 0, "Test").setIcon(android.R.drawable.ic_menu_manage).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == MENU_TEST)
		{
			testFlute();
		}
		return super.onOptionsItemSelected(item);
	}
	
	public final static int	CellSize			= 15;
	private final static int	GridSize			= 5 * CellSize;

	
	void testFlute()
	{
		 ArrayList<PointF> points = surface.getPoints();
		 
		 
	}
}
