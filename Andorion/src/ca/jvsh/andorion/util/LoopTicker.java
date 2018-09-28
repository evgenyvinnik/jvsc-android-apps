package ca.jvsh.andorion.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;



/**
 * Looper-based ticker class.  This has the advantage that asynchronous
 * updates can be scheduled by passing it a message.
 */
public class LoopTicker extends Thread implements Ticker
{
	// Debugging tag.
	private static final String				TAG				= "LoopTicker";

	// The time in ms to sleep each time round the main animation loop.
	// If zero, we will not sleep, but will run continuously.
	private long							mAnimationDelay	= 0;

	
	private TickRefresher mViewRefresher;
	// Constructor -- start at once.
	public LoopTicker(TickRefresher viewRefresher)
	{
		super("Surface Runner");
		mViewRefresher = viewRefresher;
		Log.v(TAG, "Ticker: start");		
		start();
	}
	
	@Override
	public void setAnimationDelay(long animationDelay)
	{
		Log.i(TAG, "setAnimationDelay " + animationDelay);
		mAnimationDelay = animationDelay;
	}
	
	

	// Post a tick.  An update will be done near-immediately on the
	// appropriate thread.
	public void post()
	{
		synchronized (this)
		{
			if (msgHandler == null)
				return;

			// Remove any delayed ticks.
			msgHandler.removeMessages(MSG_TICK);

			// Do a tick right now.
			msgHandler.sendEmptyMessage(MSG_TICK);
		}
	}

	// Stop this thread.  There will be no new calls to tick() after this.
	@Override
	public void kill()
	{
		Log.v(TAG, "LoopTicker: kill");

		synchronized (this)
		{
			if (msgHandler == null)
				return;

			// Remove any delayed ticks.
			msgHandler.removeMessages(MSG_TICK);

			// Do an abort right now.
			msgHandler.sendEmptyMessage(MSG_ABORT);
		}
	}

	// Stop this thread and wait for it to die.  When we return, it is
	// guaranteed that tick() will never be called again.
	// 
	// Caution: if this is called from within tick(), deadlock is
	// guaranteed.
	@Override
	public void killAndWait()
	{
		Log.v(TAG, "LoopTicker: killAndWait");

		if (Thread.currentThread() == this)
			throw new IllegalStateException("LoopTicker.killAndWait()"
					+ " called from ticker thread");

		synchronized (this)
		{
			if (msgHandler == null)
				return;

			// Remove any delayed ticks.
			msgHandler.removeMessages(MSG_TICK);

			// Do an abort right now.
			msgHandler.sendEmptyMessage(MSG_ABORT);
		}

		// Wait for the thread to finish.  Ignore interrupts.
		if (isAlive())
		{
			boolean retry = true;
			while (retry)
			{
				try
				{
					join();
					retry = false;
				}
				catch (InterruptedException e)
				{
				}
			}
			Log.v(TAG, "LoopTicker: killed");
		}
		else
		{
			Log.v(TAG, "LoopTicker: was dead");
		}
	}

	@Override
	public void run()
	{
		Looper.prepare();

		msgHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
					case MSG_TICK:
						if(mViewRefresher!= null)
							mViewRefresher.tick();
						if (!msgHandler.hasMessages(MSG_TICK))
							msgHandler.sendEmptyMessageDelayed(MSG_TICK,
									mAnimationDelay);
						break;
					case MSG_ABORT:
						Looper.myLooper().quit();
						break;
				}
			}
		};

		// Schedule the first tick.
		msgHandler.sendEmptyMessageDelayed(MSG_TICK, mAnimationDelay);

		// Go into the processing loop.
		Looper.loop();
	}

	// Message codes.
	private static final int	MSG_TICK	= 6;
	private static final int	MSG_ABORT	= 9;

	// Our message handler.
	private Handler				msgHandler	= null;
}