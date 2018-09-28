package com.dinpattern;

import com.dinpattern.R;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class TiledPatternLiveWallpaper extends WallpaperService 
{
	public static final String	SHARED_PREFS_NAME	= "tiledpatternsettings";


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
		return new TiledPatternEngine();
	}

	class TiledPatternEngine extends Engine
							 implements SharedPreferences.OnSharedPreferenceChangeListener
	{

		private final Handler		mHandler		=   new Handler();

		private final Runnable		mDrawPattern	=   new Runnable()
														{
															public void run()
															{
																drawFrame();
															}
														};
		private boolean				mVisible;
		private SharedPreferences	mPreferences;

		private final int			PATTERNS = 6;

		private final int			DIRECTION_CHANGE_COUNTER = 1500;
		private final int			LOGO_SHOW_COUNTER = 1500;
		private final int			PATTERN_CHANGE_COUNTER_OFTEN = 2000;
		private final int			PATTERN_CHANGE_COUNTER_RARE = 4000;

		//! patterns that we drawing
		private Bitmap[] 			mPattern;

		private int[]				tile_size_x;
		private int[]				tile_size_y;

		private int					screen_size_x;
		private int					screen_size_y;

		private float				tile_shift_x;
		private float				tile_shift_y;
		private float				tile_shift_x_next;
		private float				tile_shift_y_next;
		private float				movement_speed_x;
		private float				movement_speed_y;

		private int[]				fit_x;
		private int[]				fit_y;

		private int[]				remain_x;
		private int[]				remain_y;

		private int					mCurrentPattern;
		private int					mNextPattern;

		//! logo that we overlay from time to time;
		private Bitmap[] 			mLogo;
		private int[]				logo_size_x;
		private int[]				logo_size_y;

		//! flag that showing which logo to show
		private boolean				mShowWhite = false;

		private final Paint 		paint = new Paint();
		private int 				mPreviousOffset;
		private Resources 			mRes;
		private java.util.Random 	mRandom;

		//flags that respond for various settings
		private boolean[]			mAvailablePatterns;

		//! flags that show available patterns
		private int					mSpeed;

		//! flag that show how often we want to change patterns
		private int					mChangeFrequency;
		
		//! flag that show if we move randomly or not
		private boolean				mRandomDirection;

		//! flag that show if we switching pattern on home screen switch
		private boolean				mHomeScreenSwitch;

		private boolean				mChangeRandomly;

		private boolean				mChangingScreen;
		
		private boolean				mShowLogo = false;
		private boolean				mAppearLogo = false;

		//counters that helps to gently switch between patterns and logos

		//!counter that show that we need to change direction if we move randomly
		private int					mChangeRandomDirectionCounter = 0;

		//!counter that show that we have to change pattern
		private int					mPatternChangeCounter = 0;

		//!counter that show that we have to show logo
		private int					mShowLogoCounter = 0;

		private int					mTransparency;
		
		private int					mLogoTransparency;

		TiledPatternEngine()
		{
			mRes = getResources();

			mRandom = new java.util.Random();

			//Setting patterns
			{
				mPattern = new Bitmap[PATTERNS];
				mAvailablePatterns = new boolean[PATTERNS];

				//load patterns
				mPattern[0] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_aloha_turkey);
				mPattern[1] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_haunted_2_regal);
				mPattern[2] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_hollyhock);
				mPattern[3] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_kiwi);
				mPattern[4] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_simple_paisley);
				mPattern[5] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_twirll_2);

				//set size
				tile_size_x = new int[PATTERNS];
				tile_size_y = new int[PATTERNS];
	
				for(int i = 0; i < PATTERNS; i++)
				{
					tile_size_x[i] = mPattern[i].getWidth();
					tile_size_y[i] = mPattern[i].getHeight();

					mAvailablePatterns[i] = true;
				}
	
				fit_x =  new int[PATTERNS];
				fit_y =  new int[PATTERNS];
				remain_x =  new int[PATTERNS];
				remain_y =  new int[PATTERNS];
			}

			//get logo and its size
			{
				mLogo = new Bitmap[2];
				logo_size_x = new int[2];
				logo_size_y = new int[2];

				mLogo[0] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_logo_black);
				logo_size_x[0] = mLogo[0].getWidth();
				logo_size_y[0] = mLogo[0].getHeight();

				mLogo[1] = BitmapFactory.decodeResource(mRes, R.drawable.dinpattern_logo_white);
				logo_size_x[1] = mLogo[1].getWidth();
				logo_size_y[1] = mLogo[1].getHeight();
			}

			tile_shift_x = 0;
			tile_shift_y = 0;
			tile_shift_x_next = 0;
			tile_shift_y_next = 0;

			mPreviousOffset = 0;

			mPreferences = TiledPatternLiveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
			mPreferences.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPreferences, null);
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
												String key)
		{
			//check available pattern settings
			String rawval = prefs.getString("selected_patterns", "all");
			if(rawval != null && rawval.compareTo("") != 0)
			{
				String[] selected = ListPreferenceMultiSelect.parseStoredValue(rawval);

				//user selected all patterns
				if(selected[0].compareTo("all") == 0)
				{
					for(int i = 0; i < PATTERNS; i++)
					{
						mAvailablePatterns[i] = true;
					}
				}
				else
				{
					for(int i = 0; i < PATTERNS; i++)
					{
						mAvailablePatterns[i] = false;
					}

					//yeah, yeah not really optimal code but it works:
					//we checking all selected values if they are equals any of patterns
					for(int i = 0; i < selected.length; i++)
					{
						if(selected[i].compareTo("aloha_turkey") == 0)
						{
							mAvailablePatterns[0] = true;
						}
						else if(selected[i].compareTo("haunted_2_regal") == 0)
						{
							mAvailablePatterns[1] = true;
						}
						else if(selected[i].compareTo("hollyhock") == 0)
						{
							mAvailablePatterns[2] = true;
						}
						else if(selected[i].compareTo("kiwi") == 0)
						{
							mAvailablePatterns[3] = true;
						}
						else if(selected[i].compareTo("simple_paisley") == 0)
						{
							mAvailablePatterns[4] = true;
						}
						else if(selected[i].compareTo("twirll_2") == 0)
						{
							mAvailablePatterns[5] = true;
						}
					}
				}
			}
			else
			{
				//user haven't selected any patterns 
				//(this mean he really don't care what he want to see)
				//that is why we make all of them available to him
				for(int i = 0; i < PATTERNS; i++)
				{
					mAvailablePatterns[i] = true;
				}
			}

			// check movement speed setting
			String movement_speed = prefs.getString("movement_speed", "slow");
			if(movement_speed.compareTo("still") == 0)
			{
				mSpeed = 0;
			}
			else if(movement_speed.compareTo("slow") == 0)
			{
				mSpeed = 1;
			}
			else if(movement_speed.compareTo("fast") == 0)
			{
				mSpeed = 2;
			}

			//! check movement direction setting
			String movement_direction = prefs.getString("movement_direction", "random");
			mRandomDirection = false;

			if(movement_direction.compareTo("random") == 0)
			{
				mRandomDirection = true;
				ChooseRandomDirection();
			}
			else if(movement_direction.compareTo("north") == 0)
			{
				movement_speed_x = 0;
				movement_speed_y = -mSpeed;
			}
			else if(movement_direction.compareTo("west") == 0)
			{
				movement_speed_x = mSpeed;
				movement_speed_y = 0;
			}
			else if(movement_direction.compareTo("southwest") == 0)
			{
				movement_speed_x = mSpeed;
				movement_speed_y = mSpeed;
			}
			else if(movement_direction.compareTo("south") == 0)
			{
				movement_speed_x = 0;
				movement_speed_y = mSpeed;
			}
			else if(movement_direction.compareTo("southeast") == 0)
			{
				movement_speed_x = -mSpeed;
				movement_speed_y = mSpeed;
			}
			else if(movement_direction.compareTo("east") == 0)
			{
				movement_speed_x = -mSpeed;
				movement_speed_y = 0;
			}
			else if(movement_direction.compareTo("northeast") == 0)
			{
				movement_speed_x = -mSpeed;
				movement_speed_y = -mSpeed;
			}

			// check movement speed setting
			String change_frequency = prefs.getString("change_frequency", "often");
			if(change_frequency.compareTo("never") == 0)
			{
				mChangeFrequency = 0;
			}
			else if(change_frequency.compareTo("rare") == 0)
			{
				mChangeFrequency = PATTERN_CHANGE_COUNTER_RARE;
			}
			else if(change_frequency.compareTo("often") == 0)
			{
				mChangeFrequency = PATTERN_CHANGE_COUNTER_OFTEN;
			}

			// if we are changing pattern on home screen change
			mHomeScreenSwitch = prefs.getBoolean("homescreen_change", true);

			// how we are changing te
			String change_order = prefs.getString("change_order", "random");
			if(change_order.compareTo("random") == 0)
			{
				mChangeRandomly = true;
			}
			else if(change_order.compareTo("one_by_one") == 0)
			{
				mChangeRandomly = false;
			}

			mCurrentPattern = GetPatternId(0);
			//try several times until new pattern is different from current pattern
			for(int j = 0; j < PATTERNS; j++)
			{
				mNextPattern = GetPatternId(mCurrentPattern);
				if(mNextPattern != mCurrentPattern)
					break;
			}
		}

		private void ChooseRandomDirection()
		{
			if(mSpeed != 0)
			{
				if(mRandom.nextBoolean())
				{
					movement_speed_x = mRandom.nextInt(mSpeed)  + 1;
				}
				else
				{
					movement_speed_x = -mRandom.nextInt(mSpeed) - 1;
				}
	
				if(mRandom.nextBoolean())
				{
					movement_speed_y = mRandom.nextInt(mSpeed) + 1;
				}
				else
				{
					movement_speed_y = -mRandom.nextInt(mSpeed) - 1;
				}
			}
		}

		private int GetPatternId(int i)
		{
			i++;
			if(i >= PATTERNS)
				return 0;

			while(true)
			{
				if(mChangeRandomly)
				{
					i = mRandom.nextInt(PATTERNS);
					if(mAvailablePatterns[i])
					{
						return i;
					}
				}
				else
				{
					if(mAvailablePatterns[i])
					{
						return i;
					}
					else
					{
						i++;
						if(i >= PATTERNS)
							return 0;
					}
				}
			}
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
			if(mHomeScreenSwitch)
			{
				int offset = (int)(xOffset * 100);
				if(offset%25 == 0 && offset != mPreviousOffset)
				{
					mPreviousOffset = offset;
	
					mChangingScreen = true;
					mTransparency = 255;
					//try several times until new pattern is different from current pattern
					for(int j = 0; j < PATTERNS; j++)
					{
						mNextPattern = GetPatternId(mCurrentPattern);
						if(mNextPattern != mCurrentPattern)
							break;
					}
					if(mNextPattern == mCurrentPattern)
					{
						mChangingScreen = false;
						mTransparency = 0;
					}
				}
			}

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
			c.drawColor(0xff000000);

			if(mChangingScreen)
			{
				//draw next pattern
				{
					paint.setAlpha(255 - mTransparency);
		
					for(int x = -1; x < fit_x[mNextPattern] + 1; x++)
						for(int y = -1; y < fit_y[mNextPattern] + 1; y++)
						{
		
							if( (x == -1 && tile_shift_x_next <= 0) ||
								(y == -1 && tile_shift_y_next <= 0) ||
								( (x == fit_x[mNextPattern] ) && (tile_size_x[mNextPattern] * x + tile_shift_x_next >= screen_size_x ) ) ||
								( (y == fit_y[mNextPattern] ) && (tile_size_y[mNextPattern] * y + tile_shift_y_next >= screen_size_y ) ) )
							{
								continue;
							}
		
							c.drawBitmap(mPattern[mNextPattern], tile_size_x[mNextPattern] * x + tile_shift_x_next, tile_size_y[mNextPattern] * y + tile_shift_y_next, paint);
						}

					tile_shift_x_next += movement_speed_x;
					tile_shift_y_next += movement_speed_y;

					if(tile_shift_x_next > tile_size_x[mNextPattern] )
						tile_shift_x_next = 0;
					if(tile_shift_y_next > tile_size_y[mNextPattern] )
						tile_shift_y_next = 0;

					if(tile_shift_x_next < -(tile_size_x[mNextPattern] + remain_x[mNextPattern]) )
						tile_shift_x_next = -remain_x[mNextPattern];
					if(tile_shift_y_next < -(tile_size_y[mNextPattern] + remain_y[mNextPattern]) )
						tile_shift_y_next = -remain_y[mNextPattern];

				}

				//draw current pattern
				{
					paint.setAlpha(mTransparency);
	
					for(int x = -1; x < fit_x[mCurrentPattern] + 1; x++)
						for(int y = -1; y < fit_y[mCurrentPattern] + 1; y++)
						{
		
							if( (x == -1 && tile_shift_x <= 0) ||
								(y == -1 && tile_shift_y <= 0) ||
								( (x == fit_x[mCurrentPattern] ) && (tile_size_x[mCurrentPattern] * x + tile_shift_x >= screen_size_x ) ) ||
								( (y == fit_y[mCurrentPattern] ) && (tile_size_y[mCurrentPattern] * y + tile_shift_y >= screen_size_y ) ) )
							{
								continue;
							}
		
							c.drawBitmap(mPattern[mCurrentPattern], tile_size_x[mCurrentPattern] * x + tile_shift_x, tile_size_y[mCurrentPattern] * y + tile_shift_y, paint);
						}
				}

				mTransparency -= 5;
				if(mTransparency <= 0)
				{
					mChangingScreen = false;
					mCurrentPattern = mNextPattern;
					tile_shift_x = tile_shift_x_next;
					tile_shift_y = tile_shift_y_next;
					tile_shift_x_next = 0;
					tile_shift_y_next = 0;
				}
			}
			else
			{
				for(int x = -1; x < fit_x[mCurrentPattern] + 1; x++)
					for(int y = -1; y < fit_y[mCurrentPattern] + 1; y++)
					{
	
						if( (x == -1 && tile_shift_x <= 0) ||
							(y == -1 && tile_shift_y <= 0) ||
							( (x == fit_x[mCurrentPattern] ) && (tile_size_x[mCurrentPattern] * x + tile_shift_x >= screen_size_x ) ) ||
							( (y == fit_y[mCurrentPattern] ) && (tile_size_y[mCurrentPattern] * y + tile_shift_y >= screen_size_y ) ) )
						{
							continue;
						}
	
						c.drawBitmap(mPattern[mCurrentPattern], tile_size_x[mCurrentPattern] * x + tile_shift_x, tile_size_y[mCurrentPattern] * y + tile_shift_y, null);
					}
			}

			tile_shift_x += movement_speed_x;
			tile_shift_y += movement_speed_y;

			if(tile_shift_x > tile_size_x[mCurrentPattern] )
				tile_shift_x = 0;
			if(tile_shift_y > tile_size_y[mCurrentPattern] )
				tile_shift_y = 0;

			if(tile_shift_x < -(tile_size_x[mCurrentPattern] + remain_x[mCurrentPattern]) )
				tile_shift_x = -remain_x[mCurrentPattern];
			if(tile_shift_y < -(tile_size_y[mCurrentPattern] + remain_y[mCurrentPattern]) )
				tile_shift_y = -remain_y[mCurrentPattern];

			if(mRandomDirection)
			{
				mChangeRandomDirectionCounter++;
				if(mChangeRandomDirectionCounter > DIRECTION_CHANGE_COUNTER)
				{
					ChooseRandomDirection();
					mChangeRandomDirectionCounter = 0;
				}
			}

			if(mChangeFrequency != 0)
			{
				mPatternChangeCounter++;
				if(mPatternChangeCounter > mChangeFrequency)
				{
					mPatternChangeCounter = 0;
					mChangingScreen = true;
					mTransparency = 255;
					//try several times until new pattern is different from current pattern
					for(int j = 0; j < PATTERNS; j++)
					{
						mNextPattern = GetPatternId(mCurrentPattern);
						if(mNextPattern != mCurrentPattern)
							break;
					}
					if(mNextPattern == mCurrentPattern)
					{
						mChangingScreen = false;
						mTransparency = 0;
					}
				}
			}

			mShowLogoCounter++;
			if(mShowLogoCounter > LOGO_SHOW_COUNTER)
			{
				mShowLogoCounter = 0;
				mShowLogo = true;
				mAppearLogo = true;
				mLogoTransparency = 5;
			}

			//draw logo in the middle of the screen
			if(mShowLogo)
			{
				paint.setAlpha(mLogoTransparency);
				if(mShowWhite)
				{
					c.drawBitmap(mLogo[1], (screen_size_x - logo_size_x[1] ) / 2, screen_size_y/2 , paint);
				}
				else
				{
					c.drawBitmap(mLogo[0], (screen_size_x - logo_size_x[0] ) / 2, screen_size_y/2 , paint);
				}
				if(mAppearLogo)
					mLogoTransparency += 5;
				else
					mLogoTransparency -= 5;
				if(mLogoTransparency > 250)
				{
					mAppearLogo = false;
				}
				else if(mLogoTransparency <= 0)
				{
					mShowLogo = false;
					mShowWhite =! mShowWhite;
				}
			}

			c.restore();
		}

		void initFrameParams()
		{
			DisplayMetrics metrics = new DisplayMetrics();
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			display.getMetrics(metrics);

			screen_size_x = metrics.widthPixels;
			screen_size_y = metrics.heightPixels;

			for(int i = 0; i < PATTERNS; i++)
			{
				fit_x[i] = (int)FloatMath.ceil( (float)(screen_size_x) / (float)(tile_size_x[i]) );
				fit_y[i] = (int)FloatMath.ceil( (float)(screen_size_y) / (float)(tile_size_y[i]) );

				remain_x[i] = fit_x[i] * tile_size_x[i] - screen_size_x;
				remain_y[i] = fit_y[i] * tile_size_y[i] - screen_size_y;

			}
		}
	}
}