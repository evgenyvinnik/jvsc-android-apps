package ca.jvsh.andorion.layer;

import org.metalev.multitouch.controller.MultiTouchController.PointInfo;

import ca.jvsh.andorion.AndorionActivity;
import ca.jvsh.andorion.util.LoopTicker;
import ca.jvsh.andorion.util.TickRefresher;
import ca.jvsh.andorion.util.Ticker;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.Log;

public class MusicLayer implements TickRefresher
{
	// Debugging tag.
	private static final String				TAG				= "MusicLayer";

	public static final int	padding			= 6;

	protected final Paint	mPaint			= new Paint();
	protected final Paint	mFillPaint		= new Paint();

	protected RectF			gridRect[][]	=
													new RectF[AndorionActivity.GRID][AndorionActivity.GRID];
	public float			gridWidth;

	// The ticker thread which runs the animation.  null if not active.
	private Ticker							musicTicker		= null;



	public MusicLayer()
	{
		mPaint.setColor(Color.argb(255, 90, 90, 90));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(2);
	}

	public void startMusic()
	{
		if (musicTicker != null && musicTicker.isAlive())
			musicTicker.kill();
		Log.i(TAG, "set running: start ticker");
		//animTicker = !optionSet(LOOPED_TICKER) ? new ThreadTicker() : new LoopTicker();
		musicTicker = new LoopTicker(this);
		musicTicker.setAnimationDelay(500);
	}

	public void stopMusic()
	{
		Ticker ticker = musicTicker;
		if (ticker != null && ticker.isAlive())
		{
				ticker.kill();
		}
		musicTicker = null;
	}
	
	public void tick()
	{

	}

	public void setSize(int width, int height)
	{
		int size = Math.min(width, height);
		gridWidth =
				(size - (AndorionActivity.GRID + 1) * padding)
						/ AndorionActivity.GRID;

		float x = 0;
		float y = 0;
		for (int i = 0; i < AndorionActivity.GRID; i++)
		{
			y += padding;
			x = 0;

			for (int j = 0; j < AndorionActivity.GRID; j++)
			{
				x += padding;
				gridRect[i][j] = new RectF(x, y, x + gridWidth, y + gridWidth);
				x += gridWidth;
			}
			y += gridWidth;
		}
	}

	public void drawGrid(Canvas canvas, PointInfo currTouchPoint)
	{
		mFillPaint.setColor(Color.argb(128, 40, 40, 40));
		for (int i = 0; i < AndorionActivity.GRID; i++)
		{
			for (int j = 0; j < AndorionActivity.GRID; j++)
			{
				canvas.drawRoundRect(gridRect[i][j], 5, 5, mPaint);
				canvas.drawRoundRect(gridRect[i][j], 5, 5, mFillPaint);
			}
		}
	}
	
	
	public void setStartColumn(int startLoop)
	{
	}
	
	public void setStopColumn(int stopLoop)
	{
	}

}
