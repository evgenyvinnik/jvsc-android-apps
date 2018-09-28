package su.vinnik.layout;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Layout extends Activity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	public void someMethod(View theButton)
	{
		((Button)theButton).setText("Hello");
	}
}