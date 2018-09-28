/*
 * Copyright (C) 2010 John Vinnik Software House
 */

package ca.jvsh.smpte.testpattern;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import android.os.Handler;

import android.service.wallpaper.WallpaperService;

import android.view.MotionEvent;
import android.view.SurfaceHolder;

/*
 * This animated wallpaper draws a rotating wireframe cube.
 */
public class SmpteTestPattern extends WallpaperService
{

	private final Handler	mHandler	= new Handler();

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine()
	{
		return new CubeEngine();
	}

	enum RectangleColors
	{
		WideUpperGray,
		UpperLightGray,
		UpperYellow,
		UpperCyan,
		UpperGreen,
		UpperMagenta,
		UpperRed,
		UpperBlue,
		UpperGray,
		MiddleCyan,
		MiddleDarkBlue,
		MiddleGray,
		MiddleBlue,
		MiddleYellow,
		MiddlePurple,
		MiddleRed,
		Lower1,//gray
		Lower2,//black
		Lower3,//white
		Lower4,//darkgray
		Lower5,//black
		Lower6,//darkgray
		Lower7,//gray
		Lower8,//darkgray
		Lower9,//gray
		Lower10,//darkgray
		Lower11,//gray
		RectangleCount
	}

	class CubeEngine extends Engine
	{

		private final Paint		mPaint		= new Paint();
		private float			mTouchX		= -1;
		private float			mTouchY		= -1;
		private int				mFrameCounter		= 0;

		private Rect 			mRectFrame;


		private Rect[]			mColorRectangles = new Rect[27];
		private int[] 			rectColor = {0xFF696969,
											 0xFFC1C1C1,
											 0xFFC1C100,
											 0xFF00C1C1,
											 0xFF00C100,
											 0xFFC100C1,
											 0xFFC10000,
											 0xFF0000C1,
											 0xFF696969,
											 0xFF00FFFF,
											 0xFF052550,
											 0xFFC1C1C1,
											 0xFF0000FF,
											 0xFFFFFF00,
											 0xFF36056D,
											 0xFFFF0000,
											 0xFF2B2B2B,
											 0xFF050505,
											 0xFFFFFFFF,
											 0xFF050505,
											 0xFF000000,
											 0xFF050505,
											 0xFF0A0A0A,
											 0xFF050505,
											 0xFF0D0D0D,
											 0xFF050505,
											 0xFF2b2b2b};
		//width and heights
		private int upperWideHeigth			= 1;
		private int upperWideWidth			= 1;

		private final Runnable	mDrawSmpte	= new Runnable()
											{
												public void run()
												{
													drawFrame();
												}
											};
		private boolean			mVisible;

		CubeEngine()
		{
			// Create a Paint to draw the lines for our cube
			final Paint paint = mPaint;
			paint.setColor(0xffffffff);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder)
		{
			super.onCreate(surfaceHolder);

			// By default we don't get touch events, so enable them.
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			mHandler.removeCallbacks(mDrawSmpte);
		}

		@Override
		public void onVisibilityChanged(boolean visible)
		{
			mVisible = visible;
			if (visible)
			{
				drawFrame();
			}
			else
			{
				mHandler.removeCallbacks(mDrawSmpte);
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height)
		{
			super.onSurfaceChanged(holder, format, width, height);

			initFrameParams(width, height);

			drawFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder)
		{
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder)
		{
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawSmpte);
		}

		/*
		 * Store the position of the touch event so we can use it for drawing
		 * later
		 */
		@Override
		public void onTouchEvent(MotionEvent event)
		{
			if (event.getAction() == MotionEvent.ACTION_MOVE)
			{
				mTouchX = event.getX();
				mTouchY = event.getY();
			}
			else
			{
				mTouchX = -1;
				mTouchY = -1;
			}
			super.onTouchEvent(event);
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here. This example draws a wireframe cube.
		 */
		void drawFrame()
		{
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas c = null;
			try
			{
				c = holder.lockCanvas();
				if (c != null)
				{
					// draw something
					drawCube(c);
					drawTouchPoint(c);
				}
			}
			finally
			{
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			// Reschedule the next redraw
			mHandler.removeCallbacks(mDrawSmpte);
			if (mVisible)
			{
				mHandler.postDelayed(mDrawSmpte, 1000 / 25);
			}
		}

		/*
		 * Draw a wireframe cube by drawing 12 3 dimensional lines between
		 * adjacent corners of the cube
		 */
		void drawCube(Canvas c)
		{
			c.save();
			c.drawColor(0xff000000);

			Rect drawRect = new Rect();
			drawRect.left = upperWideHeigth;
			drawRect.right = mRectFrame.width();
			drawRect.top = mFrameCounter;
			drawRect.bottom = upperWideWidth + mFrameCounter;

			Paint paint = new Paint();
			paint.setARGB(255, 105, 105, 105);
			c.drawRect(drawRect, paint);

			mFrameCounter++;
			if (mFrameCounter > mRectFrame.bottom)
				mFrameCounter = 0;
			c.restore();
		}

		void initFrameParams(int width, int height)
		{

			mRectFrame = new Rect(0, 0, height, width );
			System.out.println("Rect " + mRectFrame );
			upperWideHeigth = mRectFrame.height() * 5 /12;
			upperWideWidth = mRectFrame.width() / 8;
			System.out.println("upperWideHeigth " + upperWideHeigth );
			System.out.println("upperWideWidth " + upperWideWidth );
		}
		/*
		 * Draw a circle around the current touch point, if any.
		 */
		void drawTouchPoint(Canvas c)
		{
			if (mTouchX >= 0 && mTouchY >= 0)
			{
				c.drawCircle(mTouchX, mTouchY, 80, mPaint);
			}
		}

	}
}
