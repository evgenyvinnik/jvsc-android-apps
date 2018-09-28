package ca.jvsh.audicy;

import android.os.Message;

public class TenoriThread extends Thread
{
	private TenoriView	mTenoriView;

	// variable to request thread stop (must be volatile)
	private volatile boolean	mStop	= false;

	public TenoriThread(TenoriView tenoriView)
	{
		mTenoriView = tenoriView;
	}


	Message msg;
	Message msg1;
	//main thread method
	@Override
	public void run()
	{
		while (!mStop)
		{
			try
			{
				Thread.sleep(67);
			}
			catch (Exception e)
			{
			}

			// Tell the main view it needs updating (using special handler)
			msg = Message.obtain();
			msg.arg1 = TenoriView.MSG_UPDATE;
			mTenoriView.getHandler().sendMessage(msg);
			
			msg1 = Message.obtain();
			msg1.arg1 = TenoriView.MSG_PLAY;
			mTenoriView.getHandler().sendMessage(msg1);
			
		}

	}

	//special synchronized method to stop thread execution
	public synchronized void requestStop()
	{
		mStop = true;
	}
}
