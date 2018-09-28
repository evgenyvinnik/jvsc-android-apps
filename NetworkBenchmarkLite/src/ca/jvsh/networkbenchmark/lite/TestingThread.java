package ca.jvsh.networkbenchmark.lite;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import android.util.Log;
import android.util.SparseArray;

public class TestingThread extends Thread
{
	public SparseArray<TestingSequence>	mTestingSequences;
	private int							mId;
	public boolean						mNetworkThreadActive	= false;
	public boolean						mIsUdpSocket			= false;

	private int							mServerOpenPort;
	private InetAddress					mServerIp;

	ClientFragment						mClientFragment;
	private static final String			TAG						= "TestingThread";

	public TestingThread(int num_sequences, int id)
	{
		mTestingSequences = new SparseArray<TestingSequence>(num_sequences);
		mId = id;
	}

	public void setupSocket(InetAddress ip, int port, ClientFragment clientFragment, boolean isUpd)
	{
		mServerIp = ip;
		mServerOpenPort = port;
		mClientFragment = clientFragment;
		mIsUdpSocket = isUpd;
	}

	public void run()
	{
		if (mIsUdpSocket)
		{
			runUdp();
		}
		else
		{
			runTcp();
		}
	}

	private void runTcp()
	{
		mNetworkThreadActive = true;
		Socket socket = null;

		int sequenceId = 0;

		long sleepMilliseconds;
		long sendStop, sendStart;

		byte[] data = new byte[1048576];//assume we can't send more than 1 mb

		while (mNetworkThreadActive)
		{
			sendStart = System.nanoTime();

			TestingSequence sequence = mTestingSequences.get(sequenceId);

			if(sequence.bytes_send != 0 )
			{
				try
				{
					socket = new Socket(mServerIp, mServerOpenPort);
	
					OutputStream os = socket.getOutputStream();
					if (data != null)
						os.write(data, 0, sequence.bytes_send);
					
					socket.close();
	
					mClientFragment.addSendedBytes(sequence.bytes_send);
				}
				catch (IOException e)
				{
					Log.d(TAG, "Can't open socket on thread with id " + mId + " on ip " + mServerIp.toString() + " and port " + mServerOpenPort);
					e.printStackTrace();
					return;
				}
			}
			sendStop = System.nanoTime();
			sleepMilliseconds = sequence.delay_nano - (sendStop - sendStart);
			if (sleepMilliseconds > 0)
			{
				//busy waiting
				while (true)
				{
					if ((System.nanoTime() - sendStop) >= sleepMilliseconds)
						break;
				}
			}

			sequenceId++;
			if (sequenceId >= mTestingSequences.size())
				sequenceId = 0;
		}
	}

	private void runUdp()
	{
		mNetworkThreadActive = true;
		DatagramSocket socket = null;

		int sequenceId = 0;

		long sleepMilliseconds;
		long sendStop, sendStart;

		byte[] data = new byte[1048576];//assume we can't send more than 1 mb
		while (mNetworkThreadActive)
		{
			sendStart = System.nanoTime();

			TestingSequence sequence = mTestingSequences.get(sequenceId);
			
			if(sequence.bytes_send != 0 )
			{
				try
				{
					socket = new DatagramSocket();
	
					DatagramPacket sendPacket = new DatagramPacket(data, sequence.bytes_send, mServerIp, mServerOpenPort);
					socket.send(sendPacket);
					socket.close();
	
					mClientFragment.addSendedBytes(sequence.bytes_send);
				}
				catch (IOException e)
				{
					Log.d(TAG, "Can't open socket on thread with id " + mId + " on ip " + mServerIp.toString() + " and port " + mServerOpenPort);
					e.printStackTrace();
					return;
				}
			}

			sendStop = System.nanoTime();
			sleepMilliseconds = sequence.delay_nano - (sendStop - sendStart);
			if (sleepMilliseconds > 0)
			{
				//busy waiting
				while (true)
				{
					if ((System.nanoTime() - sendStop) >= sleepMilliseconds)
						break;
				}
			}

			sequenceId++;
			if (sequenceId >= mTestingSequences.size())
				sequenceId = 0;

		}
	}

	public class TestingSequence
	{
		public int	time_total;
		public int	bytes_send;
		public long	delay_nano;
		public int	repeat;

		public TestingSequence()
		{
			repeat = -1;
		}
	}
};
