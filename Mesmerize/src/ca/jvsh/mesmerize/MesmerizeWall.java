package ca.jvsh.mesmerize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class MesmerizeWall extends WallpaperService
{
	private final Handler		mHandler			= new Handler();

	public static final String	SHARED_PREFS_NAME	= "iscsettings";
	
	public static final Random mRandom = new Random();

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
		return new MesmerizeWallEngine();
	}

	class MesmerizeWallEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener
	{

		private float				mTouchX			= -1;
		private float				mTouchY			= -1;
		private final Paint			mPaint			= new Paint();
		private final Runnable		mDrawPattern	= new Runnable()
													{
														public void run()
														{
															drawFrame();
														}
													};
		private boolean				mVisible;
		private SharedPreferences	mPreferences;

		// screen parameters
		private float			mWidth;
		private float			mHeight;
		private float			mDiagonal;

		private boolean				mVertical;

	
		MesmerizeWallEngine()
		{
			mPaint.setDither(true);
			mPaint.setAntiAlias(true);

			mPreferences = MesmerizeWall.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
			mPreferences.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPreferences, null);

			// create bitmaps
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

			try
			{
				Method mGetRawW = Display.class.getMethod("getRawWidth");
				Method mGetRawH = Display.class.getMethod("getRawHeight");
				mWidth = (Integer) mGetRawW.invoke(display);
				mHeight = (Integer) mGetRawH.invoke(display);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				mHeight = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getWidth();
				mWidth = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getHeight();
			}

			mDiagonal = (int) Math.sqrt(mHeight * mHeight + mWidth * mWidth);

		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key)
		{
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder)
		{
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			mHandler.removeCallbacks(mDrawPattern);
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
				mHandler.removeCallbacks(mDrawPattern);
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height)
		{
			super.onSurfaceChanged(holder, format, width, height);

			mVertical = height > width;

			drawFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder)
		{
			super.onSurfaceCreated(holder);
			holder.setFormat(PixelFormat.RGBA_8888);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder)
		{
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawPattern);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels)
		{

			drawFrame();
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
					drawBackground(c);
					// drawParticles(c);
				}
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}
			catch (Exception ex)
			{

			}
			finally
			{
			}

			mHandler.removeCallbacks(mDrawPattern);
			if (mVisible)
			{
				mHandler.postDelayed(mDrawPattern, 1000 / 60);
			}
		}

		void drawBackground(Canvas c)
		{
			//c.save();

			//mPaint.setAlpha(255);

			//if (mVertical)
			//	c.drawBitmap(mVerticalBitmap, 0, 0, mPaint);
			//else
			//	c.drawBitmap(mHorizontalBitmap, 0, 0, mPaint);

			//c.restore();
		}

	}
}