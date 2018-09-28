package ca.jvsh.photosharing;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.conn.util.InetAddressUtils;

import ca.jvsh.photosharing.R;

import com.actionbarsherlock.app.SherlockFragment;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class PeeringSettingsFragment extends SherlockFragment
{
	//controls
	private EditText			mServerOpenTcpPortEdit;
	private ToggleButton		mServerOnOffToggleButton;
	private TextView			mIpTextView;

	private Context				mContext;

	//server side settings
	private int					mReadBytes;
	private int					mReadBytesTotal;
	private byte[]				mDataBuffer				= new byte[10000];
	private InputStream			mInputStream;

	private static boolean		mActive					= false;

	private Thread				mServerThread;
	private ServerSocket		mServerTcpSocket		= null;
	DatagramSocket				mServerUdpSocket		= null;
	private int					mServerTcpPort;
	private int					mServerUdpPort;

	protected static final int	MSG_SERVER_SOCKET_ERR	= 0;
	protected static final int	MSG_BYTES_RECEIVED		= 1;

	static PeeringSettingsFragment newInstance()
	{
		return new PeeringSettingsFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_peering_settings, container, false);
		mContext = view.getContext();

		//set ip
		{
			mIpTextView = (TextView) (view.findViewById(R.id.ip));

			String ip = PeeringSettingsFragment.getLocalIpAddress(mContext);
			if (ip == null)
			{
				mIpTextView.setText("No Internet connection");
			}
			else
			{
				mIpTextView.setText("My ip is " + ip);
			}
		}

		mServerOpenTcpPortEdit = (EditText) view.findViewById(R.id.editTextTcpPort);

		mServerOnOffToggleButton = (ToggleButton) view.findViewById(R.id.serverSocketButton);
		mServerOnOffToggleButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (mServerOnOffToggleButton.isChecked())
				{
					if (!startTcpServerSocket())
						mServerOnOffToggleButton.setChecked(false);
				}
				else
				{
					stopTcpServerSocket();
				}
			}
		});

		/*mZeroBytesButton = (Button) view.findViewById(R.id.serverZeroBytesButton);
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
		*/

		//restore port that server will open
		mServerOpenTcpPortEdit.setText(PreferenceManager.getDefaultSharedPreferences(mContext).getString("server_open_tcp_port", "9325"));

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
			String ip = PeeringSettingsFragment.getLocalIpAddress(mContext);
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
		editor.putString("server_open_tcp_port", mServerOpenTcpPortEdit.getText().toString());

		editor.commit();

		//stop client threads
		super.onStop();
	}

	protected boolean startTcpServerSocket()
	{
		//check if we have something in the port edit
		try
		{
			mServerTcpPort = Integer.parseInt(mServerOpenTcpPortEdit.getText().toString());
		}
		catch (NumberFormatException ex)
		{
			Log.d(PeeringSettingsFragment.class.getName(), "Can't read port number");
			ex.printStackTrace();
			Toast.makeText(mContext, "Can't read port number", Toast.LENGTH_SHORT).show();
			return false;
		}

		mReadBytesTotal = 0;

		mActive = true;
		mServerThread = new Thread()
		{
			public void run()
			{
				try
				{
					mServerTcpSocket = new ServerSocket(mServerTcpPort);
				}
				catch (IOException ex)
				{
					Log.d(PeeringSettingsFragment.class.getName(), "Can't open server socket");

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

						ObjectInputStream objectInput = new ObjectInputStream(s.getInputStream());
						try
						{
							Object object = (FileDataTransfer) objectInput.readObject();
							FileDataTransfer fileDataTransfer = (FileDataTransfer) object;
							Log.d(PeeringSettingsFragment.class.getName(), "received image " + fileDataTransfer.fileName + " of size "
									+ fileDataTransfer.fileSize);
							//tmp.Print();
							
							Runnable r = new PictureReceivingThread(fileDataTransfer.fileName,
									10000,
									fileDataTransfer.tcpOnly);
							new Thread(r).start();
						}
						catch (ClassNotFoundException e)
						{
							e.printStackTrace();
						}
						objectInput.close();

						

						ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
						output.writeObject(Integer.valueOf(10000));
						output.close();

						s.close();
						s = null;
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}

				/*int bytesCounter;
				BufferedWriter mOutput;
				
				//set up output
				{
					mOutput = null;
					String fileName = "photo.jpg";
					try
					{
						File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
						FileWriter fileWriter = new FileWriter(configFile);
						mOutput = new BufferedWriter(fileWriter);
					}
					catch (IOException ex)
					{
						Log.e(PeeringSettingsFragment.class.getName(), ex.toString());
					}
				}*/

				/*while (mActive)
				{

					try
					{
						if (s == null)
							s = mServerTcpSocket.accept();

						//bytesCounter = 0;
						mInputStream = s.getInputStream();

						while ((mReadBytes = mInputStream.read(mDataBuffer)) > 0)
						{
							//bytesCounter += mReadBytes;
							//mReadBytesTotal += mReadBytes;
						}
						//mReadBytesTotal += bytesCounter;
						try
						{
							mOutput.write(String.format("%d, %d, %d", System.nanoTime(), bytesCounter, mReadBytesTotal));
							mOutput.newLine();
						}
						catch (IOException e)
						{
							e.printStackTrace();
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

				try
				{
					mOutput.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}*/

			}
		};
		mServerThread.start();

		return true;
	}

	protected void stopTcpServerSocket()
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

	protected boolean socketUdpStart()
	{
		try
		{
			//mServerUdpPort = Integer.parseInt(mServerOpenUdpPortEdit.getText().toString());
		}
		catch (NumberFormatException ex)
		{
			Log.d(PeeringSettingsFragment.class.getName(), "Can't read port number");
			ex.printStackTrace();
			Toast.makeText(mContext, "Can't read port number", Toast.LENGTH_SHORT).show();
			return false;
		}
		mReadBytesTotal = 0;
		//mBytesReceivedTextView.setText("0");
		mActive = true;
		mServerThread = new Thread()
		{
			public void run()
			{
				try
				{
					mServerUdpSocket = new DatagramSocket(mServerUdpPort);
				}
				catch (SocketException ex)
				{

					Log.d(PeeringSettingsFragment.class.getName(), "Can't open server socket");
					ex.printStackTrace();
					Message m = new Message();
					m.what = MSG_SERVER_SOCKET_ERR;
					mToastHandler.sendMessage(m);
					mActive = false;
					mServerThread = null;
					return;
				}

				BufferedWriter mOutput;
				//set up output
				{
					mOutput = null;
					Date lm = new Date();
					String fileName = "Network_Server_Thread_UDP_" + new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss", Locale.US).format(lm) + ".csv";
					try
					{
						File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
						FileWriter fileWriter = new FileWriter(configFile);
						mOutput = new BufferedWriter(fileWriter);
					}
					catch (IOException ex)
					{
						Log.e(PeeringSettingsFragment.class.getName(), ex.toString());
					}

					try
					{
						mOutput.write("Receive Timestamp (ns), Bytes, BytesTotal");
						mOutput.newLine();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}

				while (mActive)
				{

					try
					{
						DatagramPacket receivePacket = new DatagramPacket(mDataBuffer, mDataBuffer.length);
						mServerUdpSocket.receive(receivePacket);
						mReadBytesTotal += receivePacket.getLength();

						try
						{
							mOutput.write(String.format("%d, %d, %d", System.nanoTime(), receivePacket.getLength(), mReadBytesTotal));
							mOutput.newLine();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}

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

				try
				{
					mOutput.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

			}
		};
		mServerThread.start();

		return true;
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

	MyInnerHandler	mToastHandler	= new MyInnerHandler(this);

	static class MyInnerHandler extends Handler
	{
		WeakReference<PeeringSettingsFragment>	mServerFragment;

		MyInnerHandler(PeeringSettingsFragment serverFragment)
		{
			mServerFragment = new WeakReference<PeeringSettingsFragment>(serverFragment);
		}

		@Override
		public void handleMessage(Message msg)
		{
			PeeringSettingsFragment serverFragment = mServerFragment.get();

			switch (msg.what)
			{
				case MSG_SERVER_SOCKET_ERR:

					Toast.makeText(serverFragment.mContext, "Can't open server socket", Toast.LENGTH_SHORT).show();
					serverFragment.mServerOnOffToggleButton.setChecked(false);
					break;
				case MSG_BYTES_RECEIVED:
					//serverFragment.mBytesReceivedTextView.setText(" " + msg.arg1);
				default:
					break;
			}
		}
	}

}
