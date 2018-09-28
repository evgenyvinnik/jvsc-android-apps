package ca.jvsh.isc;

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

public class IscWall extends WallpaperService
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
		return new IscWallEngine();
	}

	class IscWallEngine extends Engine implements
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

		private final Bitmap		mVerticalBitmap;
		private final Bitmap		mHorizontalBitmap;
		
		private ArrayList<Bitmap>		mBeamList	= new ArrayList<Bitmap>(); 
		private ArrayList<Bitmap>		mParticleBlueList	= new ArrayList<Bitmap>(); 
		private ArrayList<Bitmap>		mParticleList	= new ArrayList<Bitmap>(); 

		private final int PARTICLES = 100;

		float						maxD			= 10.0f;//1.0f;
		float						power			= 0.5f;//0.1f;
		float						friction		= 0.4f;//0.08f;
		float						ratio			= 0.5f;//0.1f;
		float						maxD2			= maxD * maxD;
		float						a				= power / maxD2;

		private Particle[]			particles;

		IscWallEngine()
		{
			mPaint.setDither(true);
			mPaint.setAntiAlias(true);

			mPreferences = IscWall.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
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

			if (mHeight > mWidth)
			{
				mVerticalBitmap = Bitmap.createBitmap((int) mWidth, (int) mHeight, Config.ARGB_8888);
				mPaint.setShader(new RadialGradient((int) mWidth, (int) mHeight, mDiagonal, 0xFF323850, 0xFF26B8C2, android.graphics.Shader.TileMode.CLAMP));
				(new Canvas(mVerticalBitmap)).drawCircle(mWidth, mHeight, mDiagonal, mPaint);

				mHorizontalBitmap = Bitmap.createBitmap((int) mHeight, (int) mWidth, Config.ARGB_8888);
				mPaint.setShader(new RadialGradient((int) mHeight, (int) mWidth, mDiagonal, 0xFF323850, 0xFF26B8C2, android.graphics.Shader.TileMode.CLAMP));
				(new Canvas(mHorizontalBitmap)).drawCircle(mHeight, mWidth, mDiagonal, mPaint);
			}
			else
			{
				mVerticalBitmap = Bitmap.createBitmap((int) mHeight, (int) mWidth, Config.ARGB_8888);
				mPaint.setShader(new RadialGradient((int) mHeight, (int) mWidth, mDiagonal, 0xFF323850, 0xFF26B8C2, android.graphics.Shader.TileMode.CLAMP));
				(new Canvas(mVerticalBitmap)).drawCircle(mHeight, mWidth, mDiagonal, mPaint);

				mHorizontalBitmap = Bitmap.createBitmap((int) mWidth, (int) mHeight, Config.ARGB_8888);
				mPaint.setShader(new RadialGradient((int) mWidth, (int) mHeight, mDiagonal, 0xFF323850, 0xFF26B8C2, android.graphics.Shader.TileMode.CLAMP));
				(new Canvas(mHorizontalBitmap)).drawCircle(mWidth, mHeight, mDiagonal, mPaint);
			}


			particles = new Particle[PARTICLES];
			for (int i = 0; i < PARTICLES; i++)
				particles[i] = new Particle();
			
			//get particle images
			{
				for (int i = 0; i < 2; i++)
					mBeamList.add(BitmapFactory.decodeResource(getResources(), R.drawable.beam1+i));
				for (int i = 0; i < 8; i++)
				{
					mParticleBlueList.add(BitmapFactory.decodeResource(getResources(), R.drawable.particle1+i));
					mParticleList.add(BitmapFactory.decodeResource(getResources(), R.drawable.particleblue1+i));
				}
			}

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

			for (int i = 0; i < PARTICLES; i++)
			{
				particles[i].x =  IscWall.mRandom.nextFloat() * width;
				particles[i].y = IscWall.mRandom.nextFloat() * height;
			}
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
			c.save();
			c.drawColor(0xff000000);

			mPaint.setAlpha(255);

			if (mVertical)
				c.drawBitmap(mVerticalBitmap, 0, 0, mPaint);
			else
				c.drawBitmap(mHorizontalBitmap, 0, 0, mPaint);

			drawParticles(c);
			c.restore();
		}

		void drawParticles(Canvas c)
		{
			float forceX;
			float forceY;
			float disX;
			float disY;
			int signX;
			int signY;
			float dis;					
			float force;
			
			for (int i = 0; i < PARTICLES; i++)
			{
				particles[i].y -= particles[i].mvy + particles[i].mn;
				particles[i].x += particles[i].mvx;
				
				if(mVertical)
				{
					if (particles[i].y < -20)
					{
						particles[i].y = IscWall.mRandom.nextFloat() * mHeight/3 + 2*mHeight/3;
						particles[i].x = IscWall.mRandom.nextFloat() * mWidth;
						particles[i].opacity = 0;
					}
	
					if (particles[i].x > mWidth)
						particles[i].x = -50;
				}
				else
				{
					if (particles[i].y < -20)
					{
						particles[i].y =  IscWall.mRandom.nextFloat() * mWidth/3 + 2*mWidth/3;
						particles[i].x =  IscWall.mRandom.nextFloat() * mHeight;
						particles[i].opacity = 0;
					}
	
					if (particles[i].x > mHeight)
						particles[i].x = -50;
				}

				if (particles[i].y > mTouchY - 50 && particles[i].y < mTouchY + 50 && particles[i].x > mTouchX - 50 && particles[i].x < mTouchX + 50 && particles[i].isBeam == false)
				{
					disX = mTouchX - particles[i].x;
					disY = mTouchY - particles[i].y;
					
					signX = disX > 0 ? -1 : 1;
					
					signY = disY > 0 ? -1 : 1;
					
					dis = disX * disX + disY * disY;

					if (dis < maxD2)
					{
						force = -1 * a * dis / 10.0f * IscWall.mRandom.nextFloat();
						forceX = disX * disX / dis * signX * force / 5.0f;//10.0f;
						forceY = disY * disY / dis * signY * force / 5.0f;//10.0f;
					}
					else
					{
						forceX = 0;
						forceY = 0;
					}
					particles[i].spx = (particles[i].spx * friction - disX * ratio + forceX) * 0.5f;
					particles[i].spy = (particles[i].spy * friction - disY * ratio + forceY) * 0.5f;
				}
				
				particles[i].spx *= 0.95;
				particles[i].spy *= 0.9;
				particles[i].x += particles[i].spx;
				particles[i].y += particles[i].spy * 0.8;

				if (particles[i].y < 50)
					particles[i].opacity = (float) Math.max(0.0f, particles[i].opacity - 0.01f);
				else
					particles[i].opacity = (float) Math.min(0.3f, particles[i].opacity + 0.005f);
				mPaint.setAlpha((int) (255.0f * particles[i].opacity));
					
				switch(particles[i].mParticleType)
				{
				case 0:
				case 1:
					c.drawBitmap(mParticleBlueList.get(particles[i].mParticleSize), particles[i].x, particles[i].y, mPaint);
					break;
				case 2:
					c.drawBitmap(mBeamList.get(particles[i].mParticleSize), particles[i].x, particles[i].y, mPaint);
					break;
				case 3:
				case 4:
					c.drawBitmap(mParticleList.get(particles[i].mParticleSize), particles[i].x, particles[i].y, mPaint);
					break;
					
				}
			}
		}
	}
}