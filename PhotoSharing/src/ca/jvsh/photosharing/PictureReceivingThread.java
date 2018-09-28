package ca.jvsh.photosharing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;
import android.os.Message;
import android.util.Log;

public class PictureReceivingThread implements Runnable
{
	private String			filename;
	private Integer			port;
	private boolean			tcpOnly;

	private ServerSocket	mServerTcpSocket	= null;
	DatagramSocket			mServerUdpSocket	= null;

	private int				mReadBytes;
	private byte[]			mDataBuffer			= new byte[2500000];
	private InputStream		mInputStream;

	public PictureReceivingThread(String filename, Integer port, boolean tcpOnly)
	{
		this.filename = filename;
		this.port = port;
		this.tcpOnly = tcpOnly;
	}

	public void run()
	{
		if (tcpOnly)
		{
			try
			{
				mServerTcpSocket = new ServerSocket(port);

			}
			catch (IOException ex)
			{
				Log.d(PeeringSettingsFragment.class.getName(), "Can't open server socket");

				ex.printStackTrace();
				return;
			}

			Socket s = null;

			try
			{
				if (s == null)
					s = mServerTcpSocket.accept();

				File sdCard = Environment.getExternalStorageDirectory();
				File file = new File(sdCard, filename);
				FileOutputStream out = new FileOutputStream(file);

				mInputStream = s.getInputStream();
				while ((mReadBytes = mInputStream.read(mDataBuffer)) > 0)
				{
					out.write(mDataBuffer, 0, mReadBytes);
				}

				out.close();
				mInputStream.close();
				s.close();
				s = null;

				mServerTcpSocket.close();

			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
		else
		{
			try
			{
				mServerUdpSocket = new DatagramSocket(port);
			}
			catch (SocketException ex)
			{

				Log.d(PeeringSettingsFragment.class.getName(), "Can't open server socket");
				ex.printStackTrace();
				return;
			}

			try
			{

				File sdCard = Environment.getExternalStorageDirectory();
				File file = new File(sdCard, filename);
				FileOutputStream out = new FileOutputStream(file);

				DatagramPacket receivePacket = new DatagramPacket(mDataBuffer, mDataBuffer.length);
				mServerUdpSocket.receive(receivePacket);

				out.write(receivePacket.getData(), 0, receivePacket.getLength());

				out.close();

				mServerUdpSocket.close();

			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
	}
}
