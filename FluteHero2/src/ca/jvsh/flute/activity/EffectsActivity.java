package ca.jvsh.flute.activity;

import ca.jvsh.flute.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

public class EffectsActivity extends FragmentActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.effects);
		// find the button and add click method to it
		final Button effectsEditorButton = (Button) findViewById(R.id.button_effects_editor);
		effectsEditorButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent hIntent = new Intent();
				hIntent.setClass(v.getContext(), EffectsEditorActivity.class);
				startActivity(hIntent);
			}
		});

	}
}
