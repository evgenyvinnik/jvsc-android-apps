package ca.jvsh.photosharing;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.util.Log;

public class PhotoSendingThread implements Runnable
{
	private String		filepath;
	private InetAddress	ip;
	private Integer		port;
	private boolean		tcpOnly;

	public PhotoSendingThread(String filepath, InetAddress ip, Integer port, boolean tcpOnly)
	{
		this.filepath = filepath;
		this.ip = ip;
		this.port = port;
		this.tcpOnly = tcpOnly;
	}

	public void run()
	{
		Socket TcpSocket = null;
		try
		{
			TcpSocket = new Socket();
			TcpSocket.connect(new InetSocketAddress(ip, port), 2000);

			File imageFile = new File(filepath);
			FileDataTransfer fileDataTransfer = new FileDataTransfer(tcpOnly, imageFile.length(), imageFile.getName());

			ObjectOutputStream objectOutput = new ObjectOutputStream(TcpSocket.getOutputStream());
			objectOutput.writeObject(fileDataTransfer);
			Log.d(PeeringSettingsFragment.class.getName(), "send image " + fileDataTransfer.fileName + " of size" + fileDataTransfer.fileSize);
			objectOutput.close();

			ObjectInputStream objectInput = new ObjectInputStream(TcpSocket.getInputStream());
			try
			{
				Object object = (Integer) objectInput.readObject();
				Integer port = (Integer) object;
				Log.d(PeeringSettingsFragment.class.getName(), "send image via port " + port);
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
			objectInput.close();

			TcpSocket.close();

			Runnable r = new PictureSendingThread(filepath,
					ip, port, tcpOnly);
			new Thread(r).start();
		}
		catch (SocketTimeoutException ex)
		{
			Log.d(PhotoSendingThread.class.getName(), "SocketTimeoutException: Client can't connect to server with ip " + ip + " on port " + port);

			/*Message m = new Message();
			m.what = MSG_CANT_CONNECT;
			m.obj = mContext;
			mToastHandler.sendMessage(m);*/

			TcpSocket = null;
			ex.printStackTrace();
			return;
		}
		catch (UnknownHostException ex)
		{
			Log.d(PhotoSendingThread.class.getName(), "UnknownHostException: Client can't connect to server with ip " + ip + " on port " + port);

			/*Message m = new Message();
			m.what = MSG_CANT_CONNECT;
			m.obj = mContext;
			mToastHandler.sendMessage(m);*/

			TcpSocket = null;
			ex.printStackTrace();
			return;
		}
		catch (IOException ex)
		{
			Log.d(PhotoSendingThread.class.getName(), "IOException: Client can't connect to server with ip " + ip + " on port " + port);

			/*Message m = new Message();
			m.what = MSG_CANT_CONNECT;
			m.obj = mContext;
			mToastHandler.sendMessage(m);*/

			TcpSocket = null;
			ex.printStackTrace();

			return;
		}
		finally
		{
			TcpSocket = null;
		}

	}
}
