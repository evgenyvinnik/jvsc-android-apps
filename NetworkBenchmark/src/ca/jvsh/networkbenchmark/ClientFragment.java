package ca.jvsh.networkbenchmark;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ca.jvsh.networkbenchmark.TestingThread;

import com.actionbarsherlock.app.SherlockFragment;
import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ClientFragment extends SherlockFragment
{
	//edits
	private EditText					mServerIpEdit;
	private EditText					mServerPortEdit;
	private EditText					mConfigurationFilePathEdit;

	private TextView					mBytesSentTextView;

	private Button						mOpenFileButton;
	private ToggleButton				mClientOnOffToggleButton;
	private RadioGroup					mSocketTypeRadioGroup;

	private Context						mContext;

	private int							mServerOpenPort;
	private InetAddress					mServerIp;

	//	private boolean					mFirstPacketFlag;
	//private long				mStartTime;

	//threads
	public SparseArray<TestingThread>	mTestingThreads;

	//	private static boolean			mNetworkThreadActive				= false;

	private Thread						mNetworkThread;
	String								mClientMessage		= "";

	private Integer						mSentBytesTotal;
	//	private Socket					mClientSocket		= null;

	//	private final int				MAGIC				= 50;
	//	private int						nWriteBytesTotal	= MAGIC;
	//	private byte[]					mDataBuffer			= new byte[MAGIC];

	// Debugging tag.
	private static final String			TAG					= "ServerFragment";
	// Handler message id
	protected static final int			MSG_INCORRECT_IP	= 0;
	protected static final int			MSG_INCORRECT_PORT	= 1;
	protected static final int			MSG_CANT_CONNECT	= 2;
	public static final int				MSG_BYTES_SENT		= 3;

	/**
	 * Create a new instance of CountingFragment, providing "num"
	 * as an argument.
	 */
	static ClientFragment newInstance(int num)
	{
		return new ClientFragment();
	}

	/**
	 * The Fragment's UI is just a simple text view showing its
	 * instance number.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_client, container, false);
		mContext = view.getContext();

		mServerIpEdit = (EditText) view.findViewById(R.id.editTextServerIp);
		mServerIpEdit.setKeyListener(IPAddressKeyListener.getInstance());

		mServerPortEdit = (EditText) view.findViewById(R.id.editTextServerPort);
		mConfigurationFilePathEdit = (EditText) view.findViewById(R.id.editTextConfigurationFilePath);

		mBytesSentTextView = (TextView) view.findViewById(R.id.textViewBytesSent);

		mClientOnOffToggleButton = (ToggleButton) view.findViewById(R.id.toggleButtonClient);
		mClientOnOffToggleButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (mClientOnOffToggleButton.isChecked())
				{
					if (!socketStart())
						mClientOnOffToggleButton.setChecked(false);
				}
				else
				{
					socketStop();
				}
			}
		});

		mOpenFileButton = (Button) view.findViewById(R.id.buttonOpenFile);
		mOpenFileButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Intent intent = new Intent(v.getContext(), FileDialog.class);
				intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());

				//can user select directories or not
				intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
				intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);

				//alternatively you can set file filter
				intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { "xml" });

				startActivityForResult(intent, SelectionMode.MODE_OPEN);
			}
		});

		//restore saved state
		//restore server IP
		mServerIpEdit.setText(PreferenceManager.getDefaultSharedPreferences(mContext).getString("server_ip", ""));

		//restore server port
		mServerPortEdit.setText(PreferenceManager.getDefaultSharedPreferences(mContext).getString("server_port", ""));

		//restore path to configuration file
		mConfigurationFilePathEdit.setText(PreferenceManager.getDefaultSharedPreferences(mContext).getString("configuration_file", ""));

		//restore client socket type
		mSocketTypeRadioGroup = (RadioGroup) view.findViewById(R.id.radioGroupClient);
		mSocketTypeRadioGroup.check(PreferenceManager.getDefaultSharedPreferences(mContext).getInt("client_socket_type", R.id.radioClientTcp));

		return view;
	}

	@Override
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
	}

	protected boolean socketStart()
	{
		if (parseInputFile(mConfigurationFilePathEdit.getText().toString()))
		{
			//	mFirstPacketFlag = true;
			//mNetworkThreadActive = true;
			mSentBytesTotal = Integer.valueOf(0);
			mBytesSentTextView.setText("0");

			mNetworkThread = new Thread()
			{
				public void run()
				{
					//check connection

					try
					{
						mServerIp = InetAddress.getByName(mServerIpEdit.getText().toString());
					}
					catch (UnknownHostException ex)
					{
						Log.d(TAG, "Incorrect server IP address.");

						Message m = new Message();
						m.what = MSG_INCORRECT_IP;
						mToastHandler.sendMessage(m);

						ex.printStackTrace();

						return;
					}

					try
					{
						mServerOpenPort = Integer.parseInt(mServerPortEdit.getText().toString());
					}
					catch (NumberFormatException ex)
					{
						Log.d(TAG, "Incorrect server port.");

						Message m = new Message();
						m.what = MSG_INCORRECT_PORT;
						mToastHandler.sendMessage(m);

						ex.printStackTrace();

						return;
					}

					//test socket
					switch (mSocketTypeRadioGroup.getCheckedRadioButtonId())
					{
						case R.id.radioClientTcp:

							Socket TcpSocket = null;
							try
							{
								TcpSocket = new Socket();
								TcpSocket.connect(new InetSocketAddress(mServerIp, mServerOpenPort), 2000);

								TcpSocket.close();

							}
							catch (SocketTimeoutException ex)
							{
								Log.d(TAG, "SocketTimeoutException: Client can't connect to server with ip " + mServerIp + " on port " + mServerOpenPort);

								Message m = new Message();
								m.what = MSG_CANT_CONNECT;
								m.obj = mContext;
								mToastHandler.sendMessage(m);

								TcpSocket = null;
								ex.printStackTrace();
								return;
							}
							catch (UnknownHostException ex)
							{
								Log.d(TAG, "UnknownHostException: Client can't connect to server with ip " + mServerIp + " on port " + mServerOpenPort);

								Message m = new Message();
								m.what = MSG_CANT_CONNECT;
								m.obj = mContext;
								mToastHandler.sendMessage(m);

								TcpSocket = null;
								ex.printStackTrace();
								return;
							}
							catch (IOException ex)
							{
								Log.d(TAG, "IOException: Client can't connect to server with ip " + mServerIp + " on port " + mServerOpenPort);

								Message m = new Message();
								m.what = MSG_CANT_CONNECT;
								m.obj = mContext;
								mToastHandler.sendMessage(m);

								TcpSocket = null;
								ex.printStackTrace();

								return;
							}
							finally
							{
								TcpSocket = null;
							}

							for (int i = 0; i < mTestingThreads.size(); i++)
							{
								mTestingThreads.get(i).setupSocket(mServerIp, mServerOpenPort, ClientFragment.this, false);
								mTestingThreads.get(i).start();
							}

							break;

						case R.id.radioClientUdp:
							
							DatagramSocket UdpSocket = null;
							try
							{
								UdpSocket = new DatagramSocket();
								UdpSocket.connect(new InetSocketAddress(mServerIp, mServerOpenPort));

								UdpSocket.close();

							}
							catch (SocketException ex)
							{
								Log.d(TAG, "SocketException: Client can't connect to server with ip " + mServerIp + " on port " + mServerOpenPort);

								Message m = new Message();
								m.what = MSG_CANT_CONNECT;
								m.obj = mContext;
								mToastHandler.sendMessage(m);

								UdpSocket = null;
								ex.printStackTrace();
								return;
							}

							finally
							{
								UdpSocket = null;
							}
							

							for (int i = 0; i < mTestingThreads.size(); i++)
							{
								mTestingThreads.get(i).setupSocket(mServerIp, mServerOpenPort, ClientFragment.this, true);
								mTestingThreads.get(i).start();
							}

							break;
					}

				}
			};
			mNetworkThread.start();

			return true;
		}
		else
		{
			return false;
		}
	}

	protected void socketStop()
	{
		for (int i = 0; i < mTestingThreads.size(); i++)
		{
			TestingThread testingThread = mTestingThreads.valueAt(i);
			testingThread.mNetworkThreadActive = false;
			testingThread = null;
		}

		mTestingThreads.clear();
		mNetworkThread = null;
	}

	public synchronized void onActivityResult(final int requestCode,
			int resultCode, final Intent data)
	{

		if (resultCode == Activity.RESULT_OK)
		{

			if (requestCode == SelectionMode.MODE_OPEN)
			{
				Log.d(TAG, "Loading...");
			}

			String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
			mConfigurationFilePathEdit.setText(filePath);

		}
		else if (resultCode == Activity.RESULT_CANCELED)
		{
			Log.d(TAG, "file not selected");
		}

	}

	boolean parseInputFile(String filePath)
	{
		if (filePath.isEmpty())
		{
			return false;
		}

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		File file = new File(filePath);

		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.parse(file);

			Element root = dom.getDocumentElement();
			root.normalize();

			NodeList threadNodes = root.getElementsByTagName("network_thread");

			if (threadNodes.getLength() == 0)
			{

				Log.d(TAG, "No network threads nodes were found in the XML file.");
				Toast.makeText(mContext, "No network threads nodes were found in the XML file.", Toast.LENGTH_SHORT).show();
				return false;
			}

			mTestingThreads = new SparseArray<TestingThread>(threadNodes.getLength());

			for (int i = 0; i < threadNodes.getLength(); i++)
			{
				Node threadNode = threadNodes.item(i);

				if (threadNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element eThread = (Element) threadNode;

					NodeList sequenceNodes = eThread.getElementsByTagName("sequence");

					if (sequenceNodes.getLength() == 0)
					{

						Log.d(TAG, "No test sequence nodes were found in the thread node.");
						Toast.makeText(mContext, "No test sequence nodes were found in the thread node.", Toast.LENGTH_SHORT).show();
						return false;
					}

					mTestingThreads.put(i, new TestingThread(sequenceNodes.getLength(), i));

					for (int j = 0; j < sequenceNodes.getLength(); j++)
					{
						TestingThread testingThread = mTestingThreads.get(i);

						testingThread.mTestingSequences.put(j, testingThread.new TestingSequence());
						Node sequence = sequenceNodes.item(j);
						if (sequence.getNodeType() == Node.ELEMENT_NODE)
						{
							TestingThread.TestingSequence testingSequence = testingThread.mTestingSequences.get(j);

							Element eElement = (Element) sequence;

							String temp = eElement.getAttribute("time_total_ms");
							testingSequence.time_total = temp.isEmpty() ? 1000 : Integer.parseInt(temp);

							temp = eElement.getAttribute("bytes");
							testingSequence.bytes_send = temp.isEmpty() ? 1000 : Integer.parseInt(temp);

							temp = eElement.getAttribute("delay_ms");
							testingSequence.delay_ms = temp.isEmpty() ? 100 : Integer.parseInt(temp);

							temp = eElement.getAttribute("repeat");
							testingSequence.repeat = temp.isEmpty() ? -1 : Integer.parseInt(temp);
						}

					}

				}
			}
		}
		catch (ParserConfigurationException ex)
		{
			Log.d(TAG, "Exception in Parser Configuration");
			Toast.makeText(mContext, "Exception in Parser Configuration", Toast.LENGTH_SHORT).show();
			ex.printStackTrace();
			return false;
		}
		catch (DOMException ex1)
		{
			Log.d(TAG, "DOM exception. Malformed or empty XML configuration file.");
			Toast.makeText(mContext, "DOM exception. Malformed configuration XML file.", Toast.LENGTH_SHORT).show();
			ex1.printStackTrace();
			return false;
		}
		catch (SAXException ex1)
		{
			Log.d(TAG, "SAX exception. Malformed or empty XML configuration file.");
			Toast.makeText(mContext, "SAX exception. Malformed configuration XML file.", Toast.LENGTH_SHORT).show();
			ex1.printStackTrace();
			return false;
		}
		catch (IOException ex2)
		{
			Log.d(TAG, "Can't open configuration XML file.");
			Toast.makeText(mContext, "Can't open configuration XML file.", Toast.LENGTH_SHORT).show();
			ex2.printStackTrace();
			return false;
		}

		return true;
	}

	public void addSendedBytes(int bytes)
	{
		mSentBytesTotal += bytes;
		Message m = new Message();
		m.what = ClientFragment.MSG_BYTES_SENT;
		m.arg1 = mSentBytesTotal;
		mToastHandler.sendMessage(m);

	}

	Handler	mToastHandler	= new Handler()
							{
								public void handleMessage(Message msg)
								{
									switch (msg.what)
									{
										case MSG_INCORRECT_IP:

											Toast.makeText(mContext, "Incorrect server IP address.", Toast.LENGTH_SHORT).show();
											mClientOnOffToggleButton.setChecked(false);

											break;
										case MSG_INCORRECT_PORT:

											Toast.makeText(mContext, "Incorrect server port.", Toast.LENGTH_SHORT).show();
											mClientOnOffToggleButton.setChecked(false);

											break;
										case MSG_CANT_CONNECT:

											Toast.makeText(mContext, "Client can't connect to server.", Toast.LENGTH_SHORT).show();
											mClientOnOffToggleButton.setChecked(false);

											break;
										case MSG_BYTES_SENT:

											mBytesSentTextView.setText(" " + msg.arg1);

										default:
											break;
									}
									super.handleMessage(msg);
								}
							};

}
