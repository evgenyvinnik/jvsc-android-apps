package ca.jvsh.networkbenchmark.lite;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.http.conn.util.InetAddressUtils;

import ca.jvsh.networkbenchmark.lite.R;

import com.actionbarsherlock.app.SherlockFragment;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ServerFragment extends SherlockFragment
{
	private EditText			mServerOpenPortEdit;
	private ToggleButton		mServerOnOffToggleButton;
	private Button				mZeroBytesButton;
	private TextView			mBytesReceivedTextView;
	private TextView			mIpTextView;
	private RadioGroup			mSocketTypeRadioGroup;

	private Context				mContext;

	private int					mReadBytes;
	private int					mReadBytesTotal;
	private byte[]				mDataBuffer				= new byte[30000];
	private InputStream			mInputStream;

	private static boolean		mActive					= false;

	private Thread				mServerThread;
	private ServerSocket		mServerTcpSocket		= null;
	DatagramSocket				mServerUdpSocket		= null;
	private int					mServerPort;

	// Debugging tag.
	private static final String	TAG						= "ServerFragment";
	protected static final int	MSG_SERVER_SOCKET_ERR	= 0;
	protected static final int	MSG_BYTES_RECEIVED		= 1;

	/**
	 * Create a new instance of CountingFragment, providing "num"
	 * as an argument.
	 */
	static ServerFragment newInstance(int num)
	{
		return new ServerFragment();
	}

	/**
	 * The Fragment's UI is just a simple text view showing its
	 * instance number.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_server, container, false);
		mContext = view.getContext();

		//set ip
		{
			mIpTextView = (TextView) (view.findViewById(R.id.ip));

			String ip = ServerFragment.getLocalIpAddress(mContext);
			if (ip == null)
			{
				mIpTextView.setText("No Internet connection");
			}
			else
			{
				mIpTextView.setText("My ip is " + ip);
			}
		}

		mServerOpenPortEdit = (EditText) view.findViewById(R.id.editTextPort);
		mBytesReceivedTextView = (TextView) view.findViewById(R.id.textViewBytesReceived);

		mServerOnOffToggleButton = (ToggleButton) view.findViewById(R.id.serverSocketButton);
		mServerOnOffToggleButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (mServerOnOffToggleButton.isChecked())
				{
					if (!socketStart())
						mServerOnOffToggleButton.setChecked(false);
				}
				else
				{
					socketStop();
				}
			}
		});

		mZeroBytesButton = (Button) view.findViewById(R.id.serverZeroBytesButton);
		mZeroBytesButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Message m = new Message();
				m.what = MSG_BYTES_RECEIVED;
				m.arg1 = mReadBytesTotal = 0;
				mToastHandler.sendMessage(m);
			}
		});

		//restore port that server will open
		mServerOpenPortEdit.setText(PreferenceManager.getDefaultSharedPreferences(mContext).getString("server_open_port", "6000"));

		//restore server socket type
		mSocketTypeRadioGroup = (RadioGroup) view.findViewById(R.id.radioGroupServer);
		mSocketTypeRadioGroup.check(PreferenceManager.getDefaultSharedPreferences(mContext).getInt("server_socket_type", R.id.radioServerTcp));
		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if (mContext == null)
			mIpTextView.setText("No Internet connection");
		else
		{
			String ip = ServerFragment.getLocalIpAddress(mContext);
			if (ip == null)
			{
				mIpTextView.setText("No Internet connection");
			}
			else
			{
				mIpTextView.setText("My ip is " + ip);
			}
		}
	}

	@Override
	public void onStop()
	{
		//////////////////////////////////
		//saving parameters
		//////////////////////////////////
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

		//save port that server would open
		editor.putString("server_open_port", mServerOpenPortEdit.getText().toString());

		editor.putInt("server_socket_type", mSocketTypeRadioGroup.getCheckedRadioButtonId());

		editor.commit();

		//stop client threads
		super.onStop();
	}

	protected boolean socketStart()
	{
		//test socket
		switch (mSocketTypeRadioGroup.getCheckedRadioButtonId())
		{
			case R.id.radioServerTcp:
				return socketTcpStart();
			case R.id.radioServerUdp:
				return socketUdpStart();
		}

		return false;
	}

	protected boolean socketTcpStart()
	{
		//check if we have something in the port edit
		try
		{
			mServerPort = Integer.parseInt(mServerOpenPortEdit.getText().toString());
		}
		catch (NumberFormatException ex)
		{
			Log.d(TAG, "Can't read port number");
			ex.printStackTrace();
			Toast.makeText(mContext, "Can't read port number", Toast.LENGTH_SHORT).show();
			return false;
		}
		mReadBytesTotal = 0;
		mBytesReceivedTextView.setText("0");
		mActive = true;
		mServerThread = new Thread()
		{
			public void run()
			{
				try
				{
					mServerTcpSocket = new ServerSocket(mServerPort);
				}
				catch (IOException ex)
				{

					Log.d(TAG, "Can't open server socket");
					ex.printStackTrace();
					Message m = new Message();
					m.what = MSG_SERVER_SOCKET_ERR;
					mToastHandler.sendMessage(m);
					mActive = false;
					mServerThread = null;
					return;
				}

				Socket s = null;

				while (mActive)
				{

					try
					{
						if (s == null)
							s = mServerTcpSocket.accept();

						mInputStream = s.getInputStream();

						while ((mReadBytes = mInputStream.read(mDataBuffer)) > 0)
						{

							mReadBytesTotal += mReadBytes;
						}

						Message m = new Message();
						m.what = MSG_BYTES_RECEIVED;
						m.arg1 = mReadBytesTotal;
						mToastHandler.sendMessage(m);
						s.close();
						s = null;
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}

			}
		};
		mServerThread.start();

		return true;
	}

	protected boolean socketUdpStart()
	{
		try
		{
			mServerPort = Integer.parseInt(mServerOpenPortEdit.getText().toString());
		}
		catch (NumberFormatException ex)
		{
			Log.d(TAG, "Can't read port number");
			ex.printStackTrace();
			Toast.makeText(mContext, "Can't read port number", Toast.LENGTH_SHORT).show();
			return false;
		}
		mReadBytesTotal = 0;
		mBytesReceivedTextView.setText("0");
		mActive = true;
		mServerThread = new Thread()
		{
			public void run()
			{
				try
				{
					mServerUdpSocket = new DatagramSocket(mServerPort);
				}
				catch (SocketException ex)
				{

					Log.d(TAG, "Can't open server socket");
					ex.printStackTrace();
					Message m = new Message();
					m.what = MSG_SERVER_SOCKET_ERR;
					mToastHandler.sendMessage(m);
					mActive = false;
					mServerThread = null;
					return;
				}

				while (mActive)
				{

					try
					{
						DatagramPacket receivePacket = new DatagramPacket(mDataBuffer, mDataBuffer.length);
						mServerUdpSocket.receive(receivePacket);
						mReadBytesTotal += receivePacket.getLength();
						
						Message m = new Message();
						m.what = MSG_BYTES_RECEIVED;
						m.arg1 = mReadBytesTotal;
						mToastHandler.sendMessage(m);

					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}

			}
		};
		mServerThread.start();

		return true;
	}

	protected void socketStop()
	{

		//test socket
		switch (mSocketTypeRadioGroup.getCheckedRadioButtonId())
		{
			case R.id.radioServerTcp:
				socketTcpStop();
				break;
			case R.id.radioServerUdp:
				socketUdpStop();
				break;
		}
	}

	protected void socketTcpStop()
	{
		mActive = false;
		mServerThread = null;
		if (mServerTcpSocket != null)
			if (!mServerTcpSocket.isClosed())
				try
				{
					mServerTcpSocket.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
	}

	protected void socketUdpStop()
	{
		mActive = false;
		mServerThread = null;
		if (mServerUdpSocket != null)
			if (!mServerUdpSocket.isClosed())
				mServerUdpSocket.close();

	}

	public static String getLocalIpAddress(Context context)
	{
		try
		{
			ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

			final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			//try get wifi address first
			if (wifi.isAvailable())
			{
				WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
				int ipAddress = myWifiInfo.getIpAddress();
				return intToIp(ipAddress);
			}
			//if no success use that standard way

			String ipv4;
			List<NetworkInterface> nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface ni : nilist)
			{
				//display only connections that are up
				if (ni.isUp())
				{
					List<InetAddress> ialist = Collections.list(ni.getInetAddresses());
					for (InetAddress address : ialist)
					{

						if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4 = address.getHostAddress()))
						{
							return ipv4;
						}
					}
				}

			}

		}
		catch (SocketException ex)
		{
			Log.e("ClientSocketActivity", ex.toString());
		}
		return null;
	}

	public static String intToIp(int ip)
	{

		return String.format(Locale.getDefault(),
				"%d.%d.%d.%d",
				(ip & 0xff),
				(ip >> 8 & 0xff),
				(ip >> 16 & 0xff),
				(ip >> 24 & 0xff));
	}

	 MyInnerHandler mToastHandler = new MyInnerHandler(this);
	 static class MyInnerHandler extends Handler
	    {
	        WeakReference<ServerFragment> mServerFragment;

	        MyInnerHandler(ServerFragment serverFragment)
	        {
	        	mServerFragment = new WeakReference<ServerFragment>(serverFragment);
	        }

	        @Override
	        public void handleMessage(Message msg)
	        {
	        	ServerFragment serverFragment = mServerFragment.get();
	             
	        	switch (msg.what)
				{
					case MSG_SERVER_SOCKET_ERR:

						Toast.makeText(serverFragment.mContext, "Can't open server socket", Toast.LENGTH_SHORT).show();
						serverFragment.mServerOnOffToggleButton.setChecked(false);
						break;
					case MSG_BYTES_RECEIVED:
						serverFragment.mBytesReceivedTextView.setText(" " + msg.arg1);
					default:
						break;
				}
	        }
	    }
	
}
