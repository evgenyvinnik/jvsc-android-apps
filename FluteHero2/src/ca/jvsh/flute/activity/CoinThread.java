package ca.jvsh.flute.activity;

import android.os.Message;

public class CoinThread extends Thread
{
	private MoneyView	mMoneyView;

	// variable to request thread stop (must be volatile)
	private volatile boolean	mStop	= false;

	public CoinThread(MoneyView moneyView)
	{
		mMoneyView = moneyView;
	}


	//main thread method
	@Override
	public void run()
	{
		while (!mStop)
		{
			try
			{
				Thread.sleep(30);
			}
			catch (Exception e)
			{
			}

			// update the coin position
			mMoneyView.updateCoin();

			// Tell the main view it needs updating (using special handler)
			Message msg = Message.obtain();
			msg.arg1 = MoneyView.MSG_UPDATE;
			mMoneyView.getHandler().sendMessageDelayed(msg, MoneyView.MSG_UPDATE);
		}

	}

	//special synchronized method to stop thread execution
	public synchronized void requestStop()
	{
		mStop = true;
	}
}
