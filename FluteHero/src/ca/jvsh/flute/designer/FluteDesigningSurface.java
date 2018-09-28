package ca.jvsh.flute.designer;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public final class FluteDesigningSurface extends SurfaceView implements Callback
{
	public final class DrawThread extends Thread
	{
		private boolean	mRun	= true;
		private boolean	mPause	= false;

		@Override
		public void run()
		{
			waitForBitmap();

			final SurfaceHolder surfaceHolder = getHolder();
			Canvas canvas = null;

			while (mRun)
			{
				try
				{
					while (mRun && mPause)
					{
						Thread.sleep(100);
					}

					canvas = surfaceHolder.lockCanvas();
					if (canvas == null)
					{
						break;
					}

					synchronized (surfaceHolder)
					{
						controller.draw();
						canvas.drawBitmap(bitmap, 0, 0, null);
					}

					Thread.sleep(10);
				}
				catch (InterruptedException e)
				{
				}
				finally
				{
					if (canvas != null)
					{
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}

		private void waitForBitmap()
		{
			while (bitmap == null)
			{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}

		public void stopDrawing()
		{
			mRun = false;
		}

		public void pauseDrawing()
		{
			mPause = true;
		}

		public void resumeDrawing()
		{
			mPause = false;
		}
	}

	private DrawThread			drawThread;
	private final Canvas		drawCanvas			= new Canvas();
	private final Controller	controller			= new Controller(drawCanvas);
	private Bitmap				initialBitmap;
	private Bitmap				bitmap;
	private final HistoryHelper	mHistoryHelper		= new HistoryHelper(this);

	private final static Paint	paint				= new Paint();
	public final static int	CellSize			= 15;
	private final static int	GridSize			= 5 * CellSize;

	// FIXME shouldn't be that complex for drawing thread lifecycle
	private boolean				isSurfaceCreated	= false;

	public FluteDesigningSurface(Context context, AttributeSet attributes)
	{
		super(context, attributes);

		getHolder().addCallback(this);
		setFocusable(true);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				mHistoryHelper.undo();
				break;
			case MotionEvent.ACTION_UP:
				mHistoryHelper.saveState();
				break;
		}
		return controller.onTouch(this, event);
	}

	public void setStyle(Style style)
	{
		controller.setStyle(style);
	}

	public DrawThread getDrawThread()
	{
		if (drawThread == null)
		{
			drawThread = new DrawThread();
			if (isSurfaceCreated)
				// it starts only if canvas is created. It means the thread was
				// destroyed and should be started again
				drawThread.start();
		}
		return drawThread;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		drawCanvas.setBitmap(bitmap);

		//get middle point;
		int middleWidth = width / 2;

		paint.setStrokeWidth(1.0f);
		paint.setColor(0xFF2559AF);
		drawCanvas.drawRect(0, 0, middleWidth, height, paint);
		paint.setColor(0xFF5387DD);
		drawCanvas.drawRect(middleWidth, 0, width, height, paint);

		paint.setColor(0xFF6397E0);
		for(int i = middleWidth; i < width; i+= CellSize)
			drawCanvas.drawLine(i, 0, i, height, paint);
		
		for(int i = 0; i < height; i+= CellSize)
			drawCanvas.drawLine(middleWidth, i, width, i, paint);

		
		paint.setColor(0xFF91C9E4);
		for(int i = middleWidth; i < width; i+= GridSize)
			drawCanvas.drawLine(i, 0, i, height, paint);

		for(int i = 0; i < height; i+= GridSize)
			drawCanvas.drawLine(middleWidth, i, width, i, paint);

		
		
		
		paint.setColor(0xFF2B72D2);
		
		for(int i = middleWidth; i > 0; i-= CellSize)
			drawCanvas.drawLine(i, 0, i, height, paint);
		for(int i = 0; i < height; i+= CellSize)
			drawCanvas.drawLine(0, i, middleWidth, i, paint);
		
		
		paint.setColor(0xFF50A0D0);
		for(int i = middleWidth; i > 0; i-= GridSize)
			drawCanvas.drawLine(i, 0, i, height, paint);
		for(int i = 0; i < height; i+= GridSize)
			drawCanvas.drawLine(0, i, middleWidth, i, paint);

		
		
		/*paint.setColor(0xFF6397E0);
		for (int i = middleWidth; i > 0; i -= CellSize)
			drawCanvas.drawLine(i, 0, i, height, paint);
		for (int i = middleHeight; i > 0; i -= CellSize)
			drawCanvas.drawLine(0, i, width, i, paint);

		paint.setColor(0xFF2B72D2);
		for (int i = middleWidth; i < width; i += CellSize)
			drawCanvas.drawLine(i, 0, i, height, paint);
		for (int i = middleHeight; i < height; i += CellSize)
			drawCanvas.drawLine(0, i, width, i, paint);*/

		/*paint.setColor(0xFF91C9E4);
		for (int i = middleWidth; i > 0; i -= GridSize)
			drawCanvas.drawLine(i, 0, i, height, paint);
		for (int i = middleHeight; i > 0; i -= GridSize)
			drawCanvas.drawLine(0, i, width, i, paint);
		
		paint.setColor(0xFF56ACD6);
		for (int i = middleWidth; i < width; i += GridSize)
			drawCanvas.drawLine(i, 0, i, height, paint);
		for (int i = middleHeight; i < height; i += GridSize)
			drawCanvas.drawLine(0, i, width, i, paint);*/

		paint.setStrokeWidth(4.0F);
		paint.setPathEffect(new DashPathEffect(new float[] { 10, 5 }, 0));
		drawCanvas.drawLine(middleWidth, 0, middleWidth, height, paint);
		paint.setPathEffect(null);


		if (initialBitmap != null)
		{
			drawCanvas.drawBitmap(initialBitmap, 0, 0, null);
		}
		mHistoryHelper.saveState();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		getDrawThread().start();
		isSurfaceCreated = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		isSurfaceCreated = false;
		getDrawThread().stopDrawing();
		while (true)
		{
			try
			{
				getDrawThread().join();
				break;
			}
			catch (InterruptedException e)
			{
			}
		}
		drawThread = null;
	}

	public void clearBitmap()
	{
		bitmap.eraseColor(Color.WHITE);
		controller.clear();
		mHistoryHelper.saveState();
	}

	public void setInitialBitmap(Bitmap initialBitmap)
	{
		this.initialBitmap = initialBitmap;
	}

	public Bitmap getBitmap()
	{
		return bitmap;
	}

	public ArrayList<PointF> getPoints()
	{
		if (controller != null)
			controller.getPoints();

		return null;
	}

	public void undo()
	{
		mHistoryHelper.undo();
	}
}
