package ca.jvsh.photosharing;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class PeerListAdapter extends ArrayAdapter<Peer>
{

	private List<Peer>				planetList;
	private PhotoSharingFragment	editorFragment;

	public PeerListAdapter(List<Peer> planetList, PhotoSharingFragment editorFragment)
	{
		super(editorFragment.mContext, R.layout.computer_list_item, planetList);
		this.planetList = planetList;
		this.editorFragment = editorFragment;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{

		View v = convertView;

		PlanetHolder holder = new PlanetHolder();

		// First let's verify the convertView is not null
		if (convertView == null)
		{
			// This a new view we inflate the new layout
			LayoutInflater inflater = (LayoutInflater) editorFragment.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.computer_list_item, null);
			// Now we can fill the layout with the right values
			CheckBox chk = (CheckBox) v.findViewById(R.id.chk);
			TextView ipText = (TextView) v.findViewById(R.id.ip);
			TextView portText = (TextView) v.findViewById(R.id.port);

			holder.ipText = ipText;
			holder.portText = portText;
			holder.chk = chk;

			v.setTag(holder);
		}
		else
			holder = (PlanetHolder) v.getTag();

		Peer p = planetList.get(position);
		holder.ipText.setText(p.getIp().getHostAddress());
		holder.portText.setText("" + p.getPort());
		holder.chk.setOnCheckedChangeListener(null);
		holder.chk.setChecked(p.isChecked());
		holder.chk.setOnCheckedChangeListener(editorFragment);
		return v;
	}

	private static class PlanetHolder
	{
		public TextView	ipText;
		public TextView	portText;
		public CheckBox	chk;
	}

}
