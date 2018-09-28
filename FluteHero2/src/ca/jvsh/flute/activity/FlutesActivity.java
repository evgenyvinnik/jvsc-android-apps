package ca.jvsh.flute.activity;

import ca.jvsh.flute.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

public class FlutesActivity extends FragmentActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.flutes);
		// find the button and add click method to it
		final Button fluteConstructorButton = (Button) findViewById(R.id.button_flute_constructor);
		fluteConstructorButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent hIntent = new Intent();
				hIntent.setClass(v.getContext(),FluteConstructorActivity.class);
				startActivity(hIntent);
			}
		});
		
		// find the button and add click method to it
		final Button fluteDesignerButton = (Button) findViewById(R.id.button_flute_designer);
		fluteDesignerButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent hIntent = new Intent();
				hIntent.setClass(v.getContext(), FluteDesignerActivity.class);
				startActivity(hIntent);
			}
		});

	}
}
