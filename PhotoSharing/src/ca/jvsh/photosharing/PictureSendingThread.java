package ca.jvsh.photosharing;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import android.util.Log;

public class PictureSendingThread implements Runnable
{
	private String		filepath;
	private InetAddress	ip;
	private Integer		port;
	private boolean		tcpOnly;

	public PictureSendingThread(String filepath, InetAddress ip, Integer port, boolean tcpOnly)
	{
		this.filepath = filepath;
		this.ip = ip;
		this.port = port;
		this.tcpOnly = tcpOnly;
	}

	public void run()
	{
		if (tcpOnly)
		{
			runTcp();
		}
		else
		{
			runUdp();
		}
	}

	private void runTcp()
	{
		try
		{
			Socket socket = new Socket(ip, port);

			FileInputStream fileInputStream = new FileInputStream(filepath);
			OutputStream os = socket.getOutputStream();
			int nRead;
			byte[] data = new byte[2500000];

			try
			{
				while ((nRead = fileInputStream.read(data, 0, data.length)) != -1)
				{
					os.write(data, 0, nRead);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			fileInputStream.close();
			socket.close();
		}
		catch (IOException e)
		{
			Log.d(PictureSendingThread.class.getName(), "Can't open TCP socket on thread with on ip " + ip + " and port " + port);
			e.printStackTrace();
			return;
		}
	}

	private void runUdp()
	{

		try
		{
			DatagramSocket socket = null;
			socket = new DatagramSocket();

			RandomAccessFile f = new RandomAccessFile(filepath, "r");
			byte[] b = new byte[(int) f.length()];
			f.read(b);
			DatagramPacket sendPacket = new DatagramPacket(b, b.length, ip, port);
			socket.send(sendPacket);
			socket.close();
			f.close();
		}
		catch (IOException e)
		{
			Log.d(PictureSendingThread.class.getName(), "Can't open UDP socket on thread with on ip " + ip + " and port " + port);
			e.printStackTrace();
			return;
		}

	}

};
