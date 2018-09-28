package ca.jvsh.photosharing;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import ca.jvsh.photosharing.IPAddressKeyListener;
import ca.jvsh.photosharing.R;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.view.ViewCompat;

public class PhotoSharingFragment extends SherlockFragment implements android.widget.CompoundButton.OnCheckedChangeListener
{

	//controls
	private EditText			mConfigurationFilePathEdit;
	private Button				mOpenFileButton;
	private RadioGroup			mSocketTypeRadioGroup;
	ListView					mComputersList;
	private Button				mSendFileButton;

	View						mView;
	Context						mContext;

	//
	ArrayList<Peer>				mPeersList				= new ArrayList<Peer>();
	PeerListAdapter				mPeerListAdapter;
	boolean						mRemovePeerAllowed		= false;

	//between threads communication
	protected static final int	MSG_SERVER_SOCKET_ERR	= 0;
	protected static final int	MSG_BYTES_RECEIVED		= 1;

	protected static final int	MSG_INCORRECT_IP		= 0;
	protected static final int	MSG_INCORRECT_PORT		= 1;
	protected static final int	MSG_CANT_CONNECT		= 2;
	public static final int		MSG_BYTES_SENT			= 3;

	private static final int	SELECT_PICTURE			= 1;

	static PhotoSharingFragment newInstance()
	{
		return new PhotoSharingFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	private void initPeersList(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		try
		{
			mPeersList = (ArrayList<Peer>) ObjectSerializer.deserialize(prefs.getString("peers", ObjectSerializer.serialize(new ArrayList<Peer>())));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		mView = inflater.inflate(R.layout.fragment_photo_sharing, container, false);
		mContext = mView.getContext();
		initPeersList(mContext);

		mConfigurationFilePathEdit = (EditText) mView.findViewById(R.id.editTextEditorConfigurationFilePath);

		mOpenFileButton = (Button) mView.findViewById(R.id.buttonOpenFile);
		mOpenFileButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				mRemovePeerAllowed = false;

				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);

				/*Intent intent = new Intent(v.getContext(), FileDialog.class);
				intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/");

				//can user select directories or not
				intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
				intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);

				//alternatively you can set file filter
				intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { "jpg" });

				startActivityForResult(intent, SelectionMode.MODE_OPEN);*/
			}
		});

		// Load animation for deleted list items
		final Animation anim = AnimationUtils.loadAnimation(this.mContext, R.anim.fade_anim);

		mComputersList = (ListView) mView.findViewById(R.id.list);
		mPeerListAdapter = new PeerListAdapter(mPeersList, this);
		mComputersList.setAdapter(mPeerListAdapter);
		mComputersList.setLongClickable(true);

		// React to user clicks on item
		mComputersList.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parentAdapter, final View view, final int position,
					long id)
			{
				TextView clickedView = (TextView) view.findViewById(R.id.ip);

				Toast.makeText(mContext, "Selected Peer IP [" + clickedView.getText() + "]",
						Toast.LENGTH_SHORT).show();

				CheckBox rb = (CheckBox) view.findViewById(R.id.chk);
				rb.setChecked(!rb.isChecked());
				mRemovePeerAllowed = false;
			}
		});

		mComputersList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			public boolean onItemLongClick(AdapterView<?> parentAdapter, final View view, final int position,
					long id)
			{
				if (mRemovePeerAllowed)
				{
					anim.setAnimationListener(new Animation.AnimationListener()
					{
						@Override
						public void onAnimationStart(Animation animation)
						{
							ViewCompat.setHasTransientState(view, true);
						}

						@Override
						public void onAnimationRepeat(Animation animation)
						{
						}

						@Override
						public void onAnimationEnd(Animation animation)
						{
							Peer item = mPeerListAdapter.getItem(position);
							mPeerListAdapter.remove(item);
							mRemovePeerAllowed = false;
							ViewCompat.setHasTransientState(view, false);
						}
					});
					view.startAnimation(anim);
				}
				return true;
			}
		});

		mSocketTypeRadioGroup = (RadioGroup) mView.findViewById(R.id.radioGroupClient);
		mSocketTypeRadioGroup.check(PreferenceManager.getDefaultSharedPreferences(mContext).getInt("file_sharing_type", R.id.radioClientTcpOnly));

		mSendFileButton = (Button) mView.findViewById(R.id.buttonSendFile);
		mSendFileButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				for (Peer peer : mPeersList)
				{
					if (peer.isChecked())
					{
						Runnable r = new PhotoSendingThread(mConfigurationFilePathEdit.getText().toString(),
								peer.getIp(), peer.getPort(),
								mSocketTypeRadioGroup.getCheckedRadioButtonId() == R.id.radioClientTcpOnly);
						new Thread(r).start();
					}
				}

			}
		});

		return mView;
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onStop()
	{
		//////////////////////////////////
		//saving parameters
		//////////////////////////////////
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

		editor.putInt("file_sharing_type", mSocketTypeRadioGroup.getCheckedRadioButtonId());

		if (mPeersList.isEmpty())
		{
			editor.remove("peers");

		}
		else
		{
			try
			{
				editor.putString("peers", ObjectSerializer.serialize(mPeersList));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		editor.commit();

		//stop client threads
		super.onStop();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		int pos = mComputersList.getPositionForView(buttonView);
		System.out.println("Pos [" + pos + "]");
		if (pos != ListView.INVALID_POSITION)
		{
			Peer p = mPeersList.get(pos);
			p.setChecked(isChecked);
		}
	}

	public void addPeer()
	{
		final Dialog d = new Dialog(mContext);
		d.setContentView(R.layout.add_peer);
		d.setTitle("Add Peer");
		d.setCancelable(true);

		final EditText editIp = (EditText) d.findViewById(R.id.editTextIp);
		editIp.setKeyListener(IPAddressKeyListener.getInstance());

		final EditText editPort = (EditText) d.findViewById(R.id.editTextPort);

		Button b = (Button) d.findViewById(R.id.addPeerButton);
		b.setOnClickListener(new View.OnClickListener()
		{

			public void onClick(View v)
			{
				String peerIp = editIp.getText().toString();
				int peerPort = Integer.parseInt(editPort.getText().toString());
				try
				{
					PhotoSharingFragment.this.mPeersList.add(new Peer(InetAddress.getByName(peerIp), peerPort));
				}
				catch (UnknownHostException e)
				{
					e.printStackTrace();
				}
				PhotoSharingFragment.this.mPeerListAdapter.notifyDataSetChanged(); // We notify the data model is changed
				d.dismiss();
			}
		});

		d.show();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.fragment_photosharing_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.peer_add_menu:

				mRemovePeerAllowed = false;
				addPeer();

				return true;

			case R.id.peer_remove_menu:
				Toast.makeText(mContext, "Long click peer to remove.", Toast.LENGTH_SHORT).show();
				mRemovePeerAllowed = true;

				return true;
		}

		return false;
	}

	@Override
	public synchronized void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == SELECT_PICTURE && data != null && data.getData() != null)
		{
			Uri _uri = data.getData();

			//User had pick an image.
			Cursor cursor = mContext.getContentResolver().query(_uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
			cursor.moveToFirst();

			//Link to the image
			final String filePath = cursor.getString(0);
			mConfigurationFilePathEdit.setText(filePath);
			mSendFileButton.setEnabled(true);
			cursor.close();
		}
		else
		{
			mConfigurationFilePathEdit.setText("");
			mSendFileButton.setEnabled(false);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/*public synchronized void onActivityResult(final int requestCode,
			int resultCode, final Intent data)
	{

		if (resultCode == Activity.RESULT_OK)
		{
			String filePath = data.getStringExtra(FileDialog.RESULT_PATH);

			mConfigurationFilePathEdit.setText(filePath);
			mSendFileButton.setEnabled(true);
			//mAddNewThreadButton.setEnabled(true);

			if (requestCode == SelectionMode.MODE_OPEN)
			{
				Log.d(PhotoSharingFragment.class.getName(), "Opening old configuration...");

				//loadXmlConfiguration(filePath);
				//mConfigurationChanged = false;
				Log.d(PhotoSharingFragment.class.getName(), "onActivityResult mSaveMenuItem.setVisible(false);");
				//mSaveMenuItem.setVisible(false);
			}

		}
		else if (resultCode == Activity.RESULT_CANCELED)
		{
			mConfigurationFilePathEdit.setText("");
			mSendFileButton.setEnabled(false);
			//mSaveMenuItem.setVisible(false);
			//mAddNewThreadButton.setEnabled(false);
		}

	}*/

	public boolean checkFileExt(String filepath, String checkExt)
	{
		String ext = filepath.substring((filepath.lastIndexOf(".") + 1), filepath.length());
		if (ext.compareToIgnoreCase(checkExt) != 0)
			return false;
		return true;
	}

	/*@Override
	public void onStop()
	{
		//////////////////////////////////
		//saving parameters
		//////////////////////////////////
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

		//save server IP
		editor.putString("server_ip", mServerIpEdit.getText().toString());

		//save server port
		editor.putString("server_port", mServerPortEdit.getText().toString());

		//save path to the configuration file
		editor.putString("configuration_file", mConfigurationFilePathEdit.getText().toString());

		editor.putInt("client_socket_type", mSocketTypeRadioGroup.getCheckedRadioButtonId());

		editor.commit();

		//stop client threads
		super.onStop();
	}*/

	MyInnerHandler	mToastHandler	= new MyInnerHandler(this);

	static class MyInnerHandler extends Handler
	{
		WeakReference<PhotoSharingFragment>	mClientFragment;

		MyInnerHandler(PhotoSharingFragment clientFragment)
		{
			mClientFragment = new WeakReference<PhotoSharingFragment>(clientFragment);
		}

		@Override
		public void handleMessage(Message msg)
		{
			PhotoSharingFragment clientFragment = mClientFragment.get();

			switch (msg.what)
			{
				case MSG_INCORRECT_IP:

					Toast.makeText(clientFragment.mContext, "Incorrect server IP address.", Toast.LENGTH_SHORT).show();
					//TODO: take a look what is going on here
					//clientFragment.mClientOnOffToggleButton.setChecked(false);

					break;
				case MSG_INCORRECT_PORT:

					Toast.makeText(clientFragment.mContext, "Incorrect server port.", Toast.LENGTH_SHORT).show();
					//TODO: take a look what is going on here
					//clientFragment.mClientOnOffToggleButton.setChecked(false);

					break;
				case MSG_CANT_CONNECT:

					Toast.makeText(clientFragment.mContext, "Client can't connect to server.", Toast.LENGTH_SHORT).show();
					//TODO: take a look what is going on here
					//clientFragment.mClientOnOffToggleButton.setChecked(false);

					break;
				case MSG_BYTES_SENT:

					//TODO: take a look what is going on here
					//clientFragment.mBytesSentTextView.setText(" " + msg.arg1);

				default:
					break;
			}
		}
	}
}
