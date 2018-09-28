package ca.jvsh.flute.help.fragments;

import com.actionbarsherlock.app.SherlockFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ca.jvsh.flute.R;


public class EffectsFragment extends SherlockFragment
{
	int	mNum;

	/**
	 * Create a new instance of CountingFragment, providing "num"
	 * as an argument.
	 */
	static EffectsFragment newInstance(int num)
	{
		EffectsFragment f = new EffectsFragment();

		// Supply num input as an argument.
		Bundle args = new Bundle();
		args.putInt("num", num);
		f.setArguments(args);

		return f;
	}

	/**
	 * When creating, retrieve this instance's number from its arguments.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mNum = getArguments() != null ? getArguments().getInt("num") : 1;
	}

	/**
	 * The Fragment's UI is just a simple text view showing its
	 * instance number.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.help_effects, container, false);
		//View tv = v.findViewById(R.id.text);
		//((TextView) tv).setText("Chorus Fragment #" + mNum);
		//tv.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.gallery_thumb));
		return v;
	}
}
