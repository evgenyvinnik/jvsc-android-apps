package ca.jvsh.andorion;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import ca.jvsh.andorion.layer.MusicLayer;
import ca.jvsh.andorion.layer.ScoreLayer;
import ca.jvsh.andorion.util.LoopTicker;
import ca.jvsh.andorion.util.Ticker;
import ca.jvsh.andorion.util.TickRefresher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AndorionView extends SurfaceView implements
		MultiTouchObjectCanvas<Object>, SurfaceHolder.Callback, TickRefresher
{
	private MultiTouchController<Object>	multiTouchController;
	private PointInfo						mCurrTouchPoint;

	// Width, height and pixel format of the surface.
	public int								canvasWidth		= 0;
	public int								canvasHeight	= 0;

	// The surface manager for the view.
	private SurfaceHolder					surfaceHolder	= null;

	// Debugging tag.
	private static final String				TAG				= "AndorionView";

	// Enable flags.  In order to run, we need onSurfaceCreated() and
	// onResume(), which can come in either order.  So we track which ones
	// we have by these flags.  When all are set, we're good to go.  Note
	// that this is distinct from the game state machine, and its pause
	// and resume actions -- the whole game is enabled by the combination
	// of these flags set in enableFlags.
	private static final int				ENABLE_SURFACE	= 0x01;
	private static final int				ENABLE_SIZE		= 0x02;
	private static final int				ENABLE_RESUMED	= 0x04;
	private static final int				ENABLE_STARTED	= 0x08;
	private static final int				ENABLE_FOCUSED	= 0x10;
	private static final int				ENABLE_ALL		=
																	ENABLE_SURFACE
																			| ENABLE_SIZE
																			| ENABLE_RESUMED
																			| ENABLE_STARTED
																			| ENABLE_FOCUSED;

	// Enablement flags; see comment above.
	private int								enableFlags		= 0;

	// The ticker thread which runs the animation.  null if not active.
	private Ticker							animTicker		= null;

	//////////////////////////////////////////////////////////////////////////
	//layers
	//////////////////////////////////////////////////////////////////////////
	private int								mCurrentLayer;
	private MusicLayer[]					mMusicLayers = new MusicLayer[AndorionActivity.GRID];

	/////////////////////////////////////////////////////////////////////////

	public AndorionView(Context context)
	{
		this(context, null);
	}

	public AndorionView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);

	}

	public AndorionView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		multiTouchController = new MultiTouchController<Object>(this);
		mCurrTouchPoint = new PointInfo();

		// Register for events on the surface.
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		setFocusable(true);
		setFocusableInTouchMode(true);

		initMusicLayers();
	}

	private void initMusicLayers()
	{
		/*	switch (AndorionActivity.GRID)
			{
				case 10:
					for (int i = 0; i < 2; i++)
						mMusicLayers[i] = new ScoreLayer();
					for (int i = 2; i < 4; i++)
						mMusicLayers[i] = new RandomLayer();
					for (int i = 4; i < 7; i++)
						mMusicLayers[i] = new DrawLayer();

					mMusicLayers[7] = new BounceLayer();
					mMusicLayers[8] = new PushLayer();
					mMusicLayers[9] = new SoloLayer();
					break;

				case 16:
				default:
					for (int i = 0; i < 7; i++)
						mMusicLayers[i] = new ScoreLayer();
					for (int i = 7; i < 11; i++)
						mMusicLayers[i] = new RandomLayer();
					for (int i = 11; i < 13; i++)
						mMusicLayers[i] = new DrawLayer();

					mMusicLayers[13] = new BounceLayer();
					mMusicLayers[14] = new PushLayer();
					mMusicLayers[15] = new SoloLayer();
					break;

			}*/
		for (int i = 0; i < AndorionActivity.GRID; i++)
			mMusicLayers[i] = new ScoreLayer();
		mCurrentLayer = 0;
	}

	// ******************************************************************** //
	// State Handling.
	// ******************************************************************** //

	/**
	 * This is called immediately after the surface is first created.
	 * Implementations of this should start up whatever rendering code
	 * they desire.
	 * 
	 * Note that only one thread can ever draw into a Surface, so you
	 * should not draw into the Surface here if your normal rendering
	 * will be in another thread.
	 * 
	 * @param	holder		The SurfaceHolder whose surface is being created.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		setEnable(ENABLE_SURFACE, "surfaceCreated");
	}

	/**
	 * This is called immediately after any structural changes (format or
	 * size) have been made to the surface.  This method is always
	 * called at least once, after surfaceCreated(SurfaceHolder).
	 * 
	 * @param	holder		The SurfaceHolder whose surface has changed.
	 * @param	format		The new PixelFormat of the surface.
	 * @param	width		The new width of the surface.
	 * @param	height		The new height of the surface.
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height)
	{
		synchronized (surfaceHolder)
		{
			canvasWidth = width;
			canvasHeight = height;
			
			for (int i = 0; i < AndorionActivity.GRID; i++)
				mMusicLayers[i].setSize(width, height);
		
		}
		
		setEnable(ENABLE_SIZE, "set size " + width + "x" + height);
	}

	/**
	 * This is called immediately before a surface is destroyed.
	 * After returning from this call, you should no longer try to
	 * access this surface.  If you have a rendering thread that directly
	 * accesses the surface, you must ensure that thread is no longer
	 * touching the Surface before returning from this function.
	 * 
	 * @param	holder		The SurfaceHolder whose surface is being destroyed.
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		clearEnable(ENABLE_SURFACE, "surfaceDestroyed");

	}

	/**
	 * We're resuming the app.  Applications must call this from their
	 * Activity.onResume() method.
	 */
	public void onResume()
	{
		setEnable(ENABLE_RESUMED, "onResume");
	}

	/**
	 * Start the surface running.  Applications must call this to set
	 * the surface going.  They may use this to implement their own level
	 * of start/stop control, for example to implement a "pause" button.
	 */
	public void surfaceStart()
	{
		setEnable(ENABLE_STARTED, "surfaceStart");
	}

	/**
	 * Stop the surface running.  Applications may call this to stop
	 * the surface running.  They may use this to implement their own level
	 * of start/stop control, for example to implement a "pause" button.
	 */
	public void surfaceStop()
	{
		clearEnable(ENABLE_STARTED, "surfaceStop");
	}

	/**
	 * Pause the app.  Applications must call this from their
	 * Activity.onPause() method.
	 */
	public void onPause()
	{
		clearEnable(ENABLE_RESUMED, "onPause");
	}

	/**
	 * The application is closing down.  Applications must call
	 * this from their Activity.onStop() method.
	 */
	public void onStop()
	{
		Log.i(TAG, "onStop()");

		// Make sure we're paused.
		onPause();
	}

	/**
	 * Handle changes in focus.  When we lose focus, pause the game
	 * so a popup (like the menu) doesn't cause havoc.
	 * 
	 * @param	hasWindowFocus		True iff we have focus.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus)
	{
		if (!hasWindowFocus)
			clearEnable(ENABLE_FOCUSED, "onWindowFocusChanged");
		else
			setEnable(ENABLE_FOCUSED, "onWindowFocusChanged");
	}

	/**
	 * Set the given enable flag, and see if we're good to go.
	 * 
	 * @param   flag        The flag to set.
	 * @param   why         Short tag explaining why, for debugging.
	 */
	private void setEnable(int flag, String why)
	{
		boolean enabled1 = false;
		boolean enabled2 = false;
		synchronized (surfaceHolder)
		{
			enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
			enableFlags |= flag;
			enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;

			Log.i(TAG, "EN + " + why + " -> " + enableString());
		}

		// Are we all set?
		if (!enabled1 && enabled2)
			startRun();
	}

	/**
	 * Clear the given enable flag, and see if we need to shut down.
	 * 
	 * @param   flag        The flag to clear.
	 * @param   why         Short tag explaining why, for debugging.
	 */
	private void clearEnable(int flag, String why)
	{
		boolean enabled1 = false;
		boolean enabled2 = false;
		synchronized (surfaceHolder)
		{
			enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
			enableFlags &= ~flag;
			enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;

			Log.i(TAG, "EN - " + why + " -> " + enableString());
		}

		// Do we need to stop?
		if (enabled1 && !enabled2)
			stopRun();
	}

	/**
	 * Get the current enable state as a string for debugging.
	 * 
	 * @return              The current enable state as a string.
	 */
	private String enableString()
	{
		char[] buf = new char[5];
		buf[0] = (enableFlags & ENABLE_SURFACE) != 0 ? 'S' : '-';
		buf[1] = (enableFlags & ENABLE_SIZE) != 0 ? 'Z' : '-';
		buf[2] = (enableFlags & ENABLE_RESUMED) != 0 ? 'R' : '-';
		buf[3] = (enableFlags & ENABLE_STARTED) != 0 ? 'A' : '-';
		buf[4] = (enableFlags & ENABLE_FOCUSED) != 0 ? 'F' : '-';

		return String.valueOf(buf);
	}

	/**
	 * Start the animation running.  All the conditions we need to
	 * run are present (surface, size, resumed).
	 */
	private void startRun()
	{
		for (int i = 0; i < AndorionActivity.GRID; i++)
			mMusicLayers[i].startMusic();
		
		synchronized (surfaceHolder)
		{
			if (animTicker != null && animTicker.isAlive())
				animTicker.kill();
			Log.i(TAG, "set running: start ticker");

			animTicker = new LoopTicker(this);
			animTicker.setAnimationDelay(30);
		}
	}

	/**
	 * Stop the animation running.  Our surface may have been destroyed, so
	 * stop all accesses to it.  If the caller is not the ticker thread,
	 * this method will only return when the ticker thread has died.
	 */
	private void stopRun()
	{
		for (int i = 0; i < AndorionActivity.GRID; i++)
			mMusicLayers[i].stopMusic();

		
		// Kill the thread if it's running, and wait for it to die.
		// This is important when the surface is destroyed, as we can't
		// touch the surface after we return.  But if I am the ticker
		// thread, don't wait for myself to die.
		Ticker ticker = null;
		synchronized (surfaceHolder)
		{
			ticker = animTicker;
		}
		if (ticker != null && ticker.isAlive())
		{
			if (onSurfaceThread())
				ticker.kill();
			else
				ticker.killAndWait();
		}
		synchronized (surfaceHolder)
		{
			animTicker = null;
		}
	}

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	/**
	 * Asynchronously schedule an update; i.e. a frame of animation.
	 * This can only be called if the SurfaceRunner was created with
	 * the option LOOPED_TICKER.
	 */
	public void postUpdate()
	{
		synchronized (surfaceHolder)
		{
			if (!(animTicker instanceof LoopTicker))
				throw new IllegalArgumentException("Can't post updates"
						+ " without LOOPED_TICKER set");
			LoopTicker ticker = (LoopTicker) animTicker;
			ticker.post();
		}
	}

	/**
	 * Draw the game board to the screen in its current state, as a one-off.
	 * This can be used to refresh the screen.
	 */
	public void tick()
	{
		Canvas canvas = null;
		try
		{
			canvas = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder)
			{
				canvas.drawColor(Color.BLACK);
				
				mMusicLayers[mCurrentLayer].drawGrid(canvas, mCurrTouchPoint);
			}
		}
		finally
		{
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (canvas != null)
				surfaceHolder.unlockCanvasAndPost(canvas);
		}
	}

	/**
	 * Determine whether the caller is on the surface's animation thread.
	 * 
	 * @return             The resource value.
	 */
	public boolean onSurfaceThread()
	{
		return Thread.currentThread() == animTicker;
	}
	
	
	//////////////////////////////////////////////////
	//multitouch control
	///////////////////////////////////////////////////

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Pass the event on to the controller
		return multiTouchController.onTouchEvent(event);
	}

	public Object getDraggableObjectAtPoint(PointInfo pt)
	{
		// IMPORTANT: to start a multitouch drag operation, this routine must
		// return non-null
		return this;
	}

	public void getPositionAndScale(Object obj,
			PositionAndScale objPosAndScaleOut)
	{
		// We aren't dragging any objects, so this doesn't do anything in this
		// app
	}

	public void selectObject(Object obj, PointInfo touchPoint)
	{
		// We aren't dragging any objects in this particular app, but this is
		// called when the point goes up (obj == null) or down (obj != null),
		// save the touch point info
		touchPointChanged(touchPoint);
	}

	public boolean setPositionAndScale(Object obj,
			PositionAndScale newObjPosAndScale, PointInfo touchPoint)
	{
		// Called during a drag or stretch operation, update the touch point
		// info
		touchPointChanged(touchPoint);
		return true;
	}

	/**
	 * Called when the touch point info changes, causes a redraw.
	 * 
	 * @param touchPoint
	 */
	private void touchPointChanged(PointInfo touchPoint)
	{
		// Take a snapshot of touch point info, the touch point is volatile
		mCurrTouchPoint.set(touchPoint);
	}

}
