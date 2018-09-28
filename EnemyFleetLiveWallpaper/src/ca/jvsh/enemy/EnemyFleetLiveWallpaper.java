package ca.jvsh.enemy;

import ca.jvsh.enemy.R;
//import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.graphics.Matrix;

public class EnemyFleetLiveWallpaper extends WallpaperService 
{
	//public static final String	SHARED_PREFS_NAME	= "enemyfleetsettings";

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
		return new EnemyFleetEngine();
	}
	

	class EnemyFleetEngine extends Engine
	{

		//private SharedPreferences	mPreferences;
		private static final int			SPEED = 15;
		private static final int			STROKE_WIDTH = 4;
		private static final int			ENEMIES = 24;
		private static final int			COLORS = 5;
		private static final int			STARS = 6;
		private static final int			STARS_COUNT = 50;
		private static final int			CLOUDS = 6;

		private final Handler		mHandler		=   new Handler();

		private final Runnable		mDrawPattern	=   new Runnable()
														{
															public void run()
															{
																drawFrame();
															}
														};
		private boolean				mVisible;


		//! patterns that we drawing
		private Bitmap[] 			mEnemy;
		private Bitmap				mRotatedEnemy;
		private Matrix				mMatrix;

		private Bitmap[]			mStars;
		private int[]				nStarIndex;
		private int[]				mStarsX;
		private int[]				mStarsY;
		private int[]				mStarsAlpha;

		private Bitmap[]			mClouds;
		private int[]				mCloudsX;
		private int[]				mCloudsY;

		private int[]				mEnemySizeX;
		private int[]				mEnemySizeY;

		private int					mScreenSizeX;
		private int					mScreenSizeY;

		private int					mCurrentEnemy;
		private int					mCurrentDirection;
		private int					mChange;
		private int					mCurrentShape;
		private int					mCurrentFit;
		private int					mCurrentWidth;
		private int					mCurrentOffset;
		private int					mShift;

		//coordinates
		private float				mPrevX;
		private float				mPrevY;

		private float				mX;
		private float				mY;

		private float 				mRotateAngle;

		private int					mCurrentAmplitude;
		private int[]				mCurrentColor;
		
		private final Paint 		paint = new Paint();

		private Resources 			mRes;
		private java.util.Random 	mRandom;

		private int					mPoints;


		EnemyFleetEngine()
		{
			paint.setStrokeWidth(STROKE_WIDTH);
			paint.setAntiAlias(true);
			paint.setFilterBitmap(true);

			mRes = getResources();

			mRandom = new java.util.Random();
			mMatrix = new Matrix();

			//Setting patterns
			{
				mEnemy = new Bitmap[ENEMIES];

				//load patterns
				mEnemy[0] = BitmapFactory.decodeResource(mRes, R.drawable.enemy0);
				mEnemy[1] = BitmapFactory.decodeResource(mRes, R.drawable.enemy1);
				mEnemy[2] = BitmapFactory.decodeResource(mRes, R.drawable.enemy2);
				mEnemy[3] = BitmapFactory.decodeResource(mRes, R.drawable.enemy3);
				mEnemy[4] = BitmapFactory.decodeResource(mRes, R.drawable.enemy4);
				mEnemy[5] = BitmapFactory.decodeResource(mRes, R.drawable.enemy5);
				mEnemy[6] = BitmapFactory.decodeResource(mRes, R.drawable.enemy6);
				mEnemy[7] = BitmapFactory.decodeResource(mRes, R.drawable.enemy7);
				mEnemy[8] = BitmapFactory.decodeResource(mRes, R.drawable.enemy8);
				mEnemy[9] = BitmapFactory.decodeResource(mRes, R.drawable.enemy9);
				mEnemy[10] = BitmapFactory.decodeResource(mRes, R.drawable.enemy10);
				mEnemy[11] = BitmapFactory.decodeResource(mRes, R.drawable.enemy11);
				mEnemy[12] = BitmapFactory.decodeResource(mRes, R.drawable.enemy12);
				mEnemy[13] = BitmapFactory.decodeResource(mRes, R.drawable.enemy13);
				mEnemy[14] = BitmapFactory.decodeResource(mRes, R.drawable.enemy14);
				mEnemy[15] = BitmapFactory.decodeResource(mRes, R.drawable.enemy15);
				mEnemy[16] = BitmapFactory.decodeResource(mRes, R.drawable.enemy16);
				mEnemy[17] = BitmapFactory.decodeResource(mRes, R.drawable.enemy17);
				mEnemy[18] = BitmapFactory.decodeResource(mRes, R.drawable.enemy18);
				mEnemy[19] = BitmapFactory.decodeResource(mRes, R.drawable.enemy19);
				mEnemy[20] = BitmapFactory.decodeResource(mRes, R.drawable.enemy20);
				mEnemy[21] = BitmapFactory.decodeResource(mRes, R.drawable.enemy21);
				mEnemy[22] = BitmapFactory.decodeResource(mRes, R.drawable.enemy22);
				mEnemy[23] = BitmapFactory.decodeResource(mRes, R.drawable.enemy23);

				//load stars
				mStars = new Bitmap[STARS];
				mStars[0] = BitmapFactory.decodeResource(mRes, R.drawable.star0);
				mStars[1] = BitmapFactory.decodeResource(mRes, R.drawable.star1);
				mStars[2] = BitmapFactory.decodeResource(mRes, R.drawable.star2);
				mStars[3] = BitmapFactory.decodeResource(mRes, R.drawable.star3);
				mStars[4] = BitmapFactory.decodeResource(mRes, R.drawable.star4);
				mStars[5] = BitmapFactory.decodeResource(mRes, R.drawable.star5);

				nStarIndex = new int [STARS_COUNT];
				mStarsX = new int [STARS_COUNT];
				mStarsY = new int [STARS_COUNT];
				mStarsAlpha = new int [STARS_COUNT];

				//load clouds
				mClouds = new Bitmap[CLOUDS];
				mClouds[0] = BitmapFactory.decodeResource(mRes, R.drawable.cloud0);
				mClouds[1] = BitmapFactory.decodeResource(mRes, R.drawable.cloud1);
				mClouds[2] = BitmapFactory.decodeResource(mRes, R.drawable.cloud2);
				mClouds[3] = BitmapFactory.decodeResource(mRes, R.drawable.cloud3);
				mClouds[4] = BitmapFactory.decodeResource(mRes, R.drawable.cloud4);
				mClouds[5] = BitmapFactory.decodeResource(mRes, R.drawable.cloud5);

				mCloudsX = new int [CLOUDS];
				mCloudsY = new int [CLOUDS];

				//set size
				mEnemySizeX = new int[ENEMIES];
				mEnemySizeY = new int[ENEMIES];
	
				for(int i = 0; i < ENEMIES; i++)
				{
					mEnemySizeX[i] = mEnemy[i].getWidth();
					mEnemySizeY[i] = mEnemy[i].getHeight();
				}
			}

			mCurrentEnemy = 0;
			mCurrentColor = new int[COLORS];
		}

		private void setNextEnemy()
		{
			mCurrentEnemy ++;
			if(mCurrentEnemy >= ENEMIES)
				mCurrentEnemy = 0;

			mCurrentDirection = mRandom.nextInt(4);
			switch(mCurrentDirection)
			{
			case 0:
				mPoints = 0;
				mChange = SPEED;
				break;
			case 1:
				mPoints = mScreenSizeX;
				mChange = -SPEED;
				break;
			case 2:
				mPoints = 0;
				mChange = SPEED;
				break;
			case 3:
				mPoints = mScreenSizeY;
				mChange = -SPEED;
				break;
			}

			mCurrentShape = mRandom.nextInt(3);
			setCurrentColors();

			mCurrentAmplitude = mRandom.nextInt(40) + 40;
			setCurrentFit();
		}

		private void setCurrentFit()
		{
			switch(mCurrentDirection)
			{
			case 0://left
			case 1://right
				mCurrentWidth = mEnemySizeY[mCurrentEnemy] + 2 * mCurrentAmplitude;
				mCurrentFit = mScreenSizeY / mCurrentWidth;
				mCurrentOffset = (mScreenSizeY - mCurrentFit * mCurrentWidth - 4 * STROKE_WIDTH  + mCurrentWidth ) / 2;
				break;
			case 2://up
			case 3://down
				mCurrentWidth = mEnemySizeX[mCurrentEnemy] + 2 * mCurrentAmplitude;
				mCurrentFit = mScreenSizeX / mCurrentWidth;
				mCurrentOffset = (mScreenSizeX - mCurrentFit * mCurrentWidth - 4 * STROKE_WIDTH  + mCurrentWidth ) / 2;
				break;
			default:
				mCurrentFit = 1;
				break;
			}
		}
		private void setCurrentColors()
		{
			for(int i = 0; i < COLORS; i++)
			{
				mCurrentColor[i] = Color.argb(0xff, mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255));
			}
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder)
		{
			super.onCreate(surfaceHolder);
			surfaceHolder.setFormat(android.graphics.PixelFormat.RGBA_8888);
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
		public void onSurfaceChanged(SurfaceHolder holder,
									 int format,
									 int width,
									 int height)
		{
			super.onSurfaceChanged(holder, format, width, height);

			initFrameParams();

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
			mHandler.removeCallbacks(mDrawPattern);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels)
		{
			drawFrame();
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here.
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
					drawPattern(c);
				}
			}
			finally
			{
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			mHandler.removeCallbacks(mDrawPattern);
			if (mVisible)
			{
				mHandler.postDelayed(mDrawPattern, 1000 / 25);
			}
		}

		void drawPattern(Canvas c)
		{
			c.save();
			c.drawColor(0xff3989C2);

			drawStars(c);

			paint.setAlpha(255);

			switch(mCurrentDirection)
			{
			case 0:
				drawHorizontal(c, true);
				break;
			case 1:
				drawHorizontal(c, false);
				break;
			case 2:
				drawVertical(c, true);
				break;
			case 3:
				drawVertical(c, false);
				break;
			}

			drawClouds(c);

			c.restore();
		}

		void drawStars(Canvas c)
		{
			for(int i = 0; i < STARS_COUNT; i++)
			{
				paint.setAlpha(mStarsAlpha[i]);
				c.drawBitmap(mStars[nStarIndex[i]],
							mStarsX[i],
							mStarsY[i],
							paint);
				--mStarsAlpha[i];
				if(mStarsAlpha[i] < 5)
				{
					nStarIndex[i] = mRandom.nextInt(STARS);
					mStarsX[i] = mRandom.nextInt(mScreenSizeX);
					mStarsY[i] = mRandom.nextInt(mScreenSizeY);
					mStarsAlpha[i] = mRandom.nextInt(250) + 5;
				}
			}
		}
		
		void drawHorizontal(Canvas c, boolean left)
		{
			

			if(left)
			{
				mX =  0.0f;
			}
			else
			{
				mX = mScreenSizeX;
			}

			mRotateAngle = 0.0f;
			mY = FloatMath.sin(0.01745f * mX) * mCurrentAmplitude;

			boolean bIncrease = true;
			int nDirection = 0;
			int nAddiction = 0;

			while(true)
			{
				if(left)
				{
					if(mX > mPoints)
						break;
				}
				else
				{
					if(mX < mPoints)
						break;
				}

				mPrevX = mX;
				mPrevY = mY;

				mX += mChange;

				switch(mCurrentShape)
				{
				case 0:
					{
						if(bIncrease)
							mY += SPEED;
						else
							mY -= SPEED;
		
						if(mY >= mCurrentAmplitude)
							bIncrease = false;
						else if(mY <= -mCurrentAmplitude)
							bIncrease = true;
					}
					break;
				case 1:
					mY = FloatMath.sin(0.01745f * mX) * mCurrentAmplitude;
					break;
				case 2:
					{
						nAddiction += SPEED;
						switch(nDirection)
						{
						case 0:
							if(nAddiction > mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 1;
							}
							break;
						case 1:
							mY += SPEED;
							if(nAddiction > mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 2;
							}
							break;
						case 2:
							if(nAddiction > mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 3;
							}
							break;
						case 3:
							mY -= SPEED;
							if(nAddiction > 2 * mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 0;
							}
							break;
							
						}
					}
					break;
				}

				for(int j = 0; j < COLORS; j++ )
				{
					paint.setColor(mCurrentColor[j]);
					for(int k = 0; k < mCurrentFit; k++)
					{
						mShift =  mCurrentOffset + k * mCurrentWidth + j * STROKE_WIDTH * 2;
						c.drawLine( mPrevX,
									mShift + mPrevY,
									mX,
									mShift + mY,
									paint);
					}
				}
			}

			mRotateAngle = (float) Math.asin( (mX-mPrevX) / FloatMath.sqrt( (mX-mPrevX)* (mX-mPrevX) + (mY-mPrevY)* (mY-mPrevY) )) * 57.29f;

			if(mY-mPrevY < 0)
				mRotateAngle = mRotateAngle + 270;
			else
				mRotateAngle = - mRotateAngle - 270;

			mMatrix.setRotate(mRotateAngle);

			mRotatedEnemy = Bitmap.createBitmap(mEnemy[mCurrentEnemy],
												0, 0,
												mEnemySizeX[mCurrentEnemy],
												mEnemySizeY[mCurrentEnemy],
												mMatrix, true);

			for(int k = 0; k < mCurrentFit; k++)
			{
				c.drawBitmap(mRotatedEnemy,
							mX - mRotatedEnemy.getWidth() / 2,
							mCurrentOffset + k * mCurrentWidth + 4 * STROKE_WIDTH + mY - mRotatedEnemy.getHeight() / 2,
							null);
			}

			mPoints += mChange;
			if(left)
			{
				if(mPoints > mScreenSizeX)
				{
					setNextEnemy();
				}
			}
			else
			{
				if(mPoints < 0)
				{
					setNextEnemy();
				}
			}
		}

		void drawVertical(Canvas c, boolean down)
		{
			if(down)
			{
				mY =  0.0f;
			}
			else
			{
				mY = mScreenSizeY;
			}

			mRotateAngle = 0.0f;
			mX = FloatMath.sin(0.01745f * mY) * mCurrentAmplitude;

			boolean bIncrease = true;
			int nDirection = 0;
			int nAddiction = 0;

			while(true)
			{
				if(down)
				{
					if(mY > mPoints)
						break;
				}
				else
				{
					if(mY < mPoints)
						break;
				}

				mPrevX = mX;
				mPrevY = mY;

				mY += mChange;

				switch(mCurrentShape)
				{
				case 0:
					{	
						if(bIncrease)
							mX += SPEED;
						else
							mX -= SPEED;
		
						if(mX >= mCurrentAmplitude)
							bIncrease = false;
						else if(mX <= -mCurrentAmplitude)
							bIncrease = true;
					}
					break;
				case 1:
					mX = FloatMath.sin(0.01745f * mY) * mCurrentAmplitude;
					break;
				case 2:
					{
						nAddiction += SPEED;
						switch(nDirection)
						{
						case 0:
							if(nAddiction > mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 1;
							}
							break;
						case 1:
							mX += SPEED;
							if(nAddiction > mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 2;
							}
							break;
						case 2:
							if(nAddiction > mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 3;
							}
							break;
						case 3:
							mX -= SPEED;
							if(nAddiction > 2 * mCurrentAmplitude)
							{
								nAddiction = 0;
								nDirection = 0;
							}
							break;
						}
					}
					break;
				}

				for(int j = 0; j < COLORS; j++ )
				{
					paint.setColor(mCurrentColor[j]);
					for(int k = 0; k < mCurrentFit; k++)
					{
						mShift =  mCurrentOffset + k * mCurrentWidth + j * STROKE_WIDTH * 2;
						c.drawLine( mShift + mPrevX,
									mPrevY,
									mShift + mX,
									mY,
									paint);
					}
				}
			}

			mRotateAngle = (float) Math.asin( (mY-mPrevY) / FloatMath.sqrt( (mX-mPrevX)* (mX-mPrevX) + (mY-mPrevY)* (mY-mPrevY) )) * 57.29f;

			if(mX-mPrevX < 0)
				mRotateAngle = - mRotateAngle + 180;

			mMatrix.setRotate(mRotateAngle);

			mRotatedEnemy = Bitmap.createBitmap(mEnemy[mCurrentEnemy],
												0, 0,
												mEnemySizeX[mCurrentEnemy],
												mEnemySizeY[mCurrentEnemy],
												mMatrix, true);

			for(int k = 0; k < mCurrentFit; k++)
			{
				c.drawBitmap(mRotatedEnemy,
							mCurrentOffset + k * mCurrentWidth + 4 * STROKE_WIDTH + mX - mRotatedEnemy.getWidth() / 2,
							mY - mRotatedEnemy.getHeight() / 2,
							null);
			}

			mPoints += mChange;
			if(down)
			{
				if(mPoints > mScreenSizeY)
				{
					setNextEnemy();
				}
			}
			else
			{
				if(mPoints < 0)
				{
					setNextEnemy();
				}
			}
		}

		void drawClouds(Canvas c)
		{
			for(int i = 0; i < CLOUDS; i++)
			{
				c.drawBitmap(mClouds[i],
							mCloudsX[i],
							mCloudsY[i],
							null);
				mCloudsX[i]++;
				if(mCloudsX[i] > mScreenSizeX)
				{
					mCloudsX[i] = -mClouds[i].getWidth() - mRandom.nextInt(50);
					mCloudsY[i] = mRandom.nextInt(mScreenSizeY);
				}
			}
		}

		void initFrameParams()
		{
			DisplayMetrics metrics = new DisplayMetrics();
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			display.getMetrics(metrics);

			mScreenSizeX = metrics.widthPixels;
			mScreenSizeY = metrics.heightPixels;

			//set stars

			for(int i = 0; i < STARS_COUNT; i++)
			{
				nStarIndex[i] = mRandom.nextInt(STARS);
				mStarsX[i] = mRandom.nextInt(mScreenSizeX);
				mStarsY[i] = mRandom.nextInt(mScreenSizeY);
				mStarsAlpha[i] = mRandom.nextInt(250) + 5;
			}

			//set clouds
			for(int i = 0; i < CLOUDS; i++)
			{
				mCloudsX[i] = mRandom.nextInt(mScreenSizeX);
				mCloudsY[i] = mRandom.nextInt(mScreenSizeY);
			}

			setNextEnemy();
		}
	}
}