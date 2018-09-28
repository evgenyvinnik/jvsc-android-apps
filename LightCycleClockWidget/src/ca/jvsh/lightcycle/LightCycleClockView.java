package ca.jvsh.lightcycle;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;

public class LightCycleClockView
{
	public static final String	INTENT_ON_CLICK_FORMAT	= "ca.jvsh.reflectius.id.%d.click";

	private int					mHeight;
	private int					mWidth;
	private float				mDensity;

	private static float		scale;
	private static float		digitscale				= 0.4f;
	private static float		eps						= 1.3f;
	float						mMirrorLength			= 4;

	private int					mWidgetId;

	private final Paint			mPaint					= new Paint();
	private final Paint			mPaintBlur				= new Paint();

	Bitmap						mMainBitmap;
	Canvas						mCanvasMain;

	Bitmap						mCoverBitmap;
	Bitmap						mLaserCoverBitmap;
	Bitmap						mFrameGlowBitmap;

	Bitmap						mMirrorsBitmap;
	Canvas						mCanvasMirrors;

	Bitmap						mLaserBitmap;
	Canvas						mCanvasLaser;

	int[]						mOldDigits				= new int[3];
	int[]						mCurrentDigits			= new int[3];

	float[][]					mTargetAngles			= new float[6][17];
	float[][]					mCurrentAngles			= new float[6][17];
	float[][]					mTurnAngles				= new float[6][17];

	float[][][]					mMirrorCoordinates		= new float[6][2][17];
	private final float			mOffsetX				= 80;
	private final float			mDigitOffsetX			= 140;
	private final float			mTenOffsetX				= 255;
	private final float			mOffsetY				= 30;

	Time						mCurrentTime			= new Time();
	float						mLaserX;
	float						mLaserY;
	float						mLaserRotation;
	boolean						mMirrorFound;

	int							mDigit;
	int							mMirror;
	Path						mLaserPath				= new Path();
	int							mTimeFormat				= -1;
	int							mLaserColor				= 0xFFFF0000;
	boolean						mDrawLaserCover			= true;
	boolean						mNeedRedraw				= false;
	SharedPreferences 			mPrefs;


	public LightCycleClockView(Context context, int widgetId)
	{
		DisplayMetrics metrics = LightCycleClockWidgetApp.getMetrics();

		mDensity = metrics.density;
		mWidth = (int) (370 * metrics.density);
		mHeight = (int) (100 * metrics.density);

		scale = metrics.density;
		eps *= scale;
		digitscale *= scale;
		mMirrorLength *= scale;

		mWidgetId = widgetId;

		mPrefs = getContext().getSharedPreferences("prefs", 0);

		//set Paint variables
		{
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);

			mPaintBlur.set(mPaint);
			mPaintBlur.setStyle(Paint.Style.STROKE);
			mPaintBlur.setStrokeWidth(5f * scale);
			mPaintBlur.setMaskFilter(new BlurMaskFilter(5 * scale, BlurMaskFilter.Blur.NORMAL));
		}

		mMainBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasMain = new Canvas(mMainBitmap);

		mLaserBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasLaser = new Canvas(mLaserBitmap);

		mMirrorsBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasMirrors = new Canvas(mMirrorsBitmap);

		drawCover();

		drawLaserCover();

		drawFrameGlow();

		//set mirror coordinates
		float[][] tens = { { 60, 132, 60, 132, 60, 132, 60, 132, 60, 132, 11, 11, 11, 11, 11, 11, 39 }, { 6, 6, 24, 24, 96, 96, 168, 168, 189, 189, 24, 69, 96, 113, 168, 189, 189 } };

		float[][] digits = { { 38, 110, 38, 110, 38, 110, 38, 110, 38, 110, 11, 11, 11, 11, 11, 17, 11 }, { 6, 6, 24, 24, 96, 96, 168, 168, 189, 189, 24, 69, 96, 168, 189, 189, 113 } };

		//set x and y offsets
		for (int i = 0; i < 17; i++)
		{
			for (int k = 0; k < 2; k++)
			{
				mMirrorCoordinates[4][k][i] = mMirrorCoordinates[2][k][i] = mMirrorCoordinates[0][k][i] = tens[k][i];
				mMirrorCoordinates[5][k][i] = mMirrorCoordinates[3][k][i] = mMirrorCoordinates[1][k][i] = digits[k][i];
			}

			mMirrorCoordinates[0][1][i] = tens[1][i];

			mMirrorCoordinates[0][0][i] += mOffsetX;
			mMirrorCoordinates[1][0][i] += mOffsetX + mDigitOffsetX;
			mMirrorCoordinates[2][0][i] += mOffsetX + mTenOffsetX;
			mMirrorCoordinates[3][0][i] += mOffsetX + mTenOffsetX + mDigitOffsetX;
			mMirrorCoordinates[4][0][i] += mOffsetX + 2 * mTenOffsetX;
			mMirrorCoordinates[5][0][i] += mOffsetX + 2 * mTenOffsetX + mDigitOffsetX;

			for (int j = 0; j < 6; j++)
			{
				mMirrorCoordinates[j][1][i] += mOffsetY;

				mMirrorCoordinates[j][0][i] *= digitscale;
				mMirrorCoordinates[j][1][i] *= digitscale;
			}
		}
	}

	public Context getContext()
	{
		return (LightCycleClockWidgetApp.getApplication());
	}

	public float getDensity()
	{
		return mDensity;
	}

	public int getmWidgetId()
	{
		return mWidgetId;
	}

	public void Redraw(AppWidgetManager appWidgetManager)
	{
		if (mTimeFormat == -1)
		{
			mTimeFormat = mPrefs.getInt("timeformat" + mWidgetId, -1);

			mLaserColor = mPrefs.getInt("color" + mWidgetId, 0xff6FC3DF);
			mDrawLaserCover = mPrefs.getBoolean("lasercover" + mWidgetId, true);

		}

		RemoteViews rviews = new RemoteViews(getContext().getPackageName(), R.layout.lightcycleclock_widget);
		
		if(adjustAngles())
		{

			mCanvasMain.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
			mCanvasLaser.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
			mCanvasMirrors.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
	
			mCanvasMain.drawBitmap(mCoverBitmap, 0, 0, mPaint);
	
			drawMirrorsLaser();
	
			mCanvasMain.drawBitmap(mLaserBitmap, 0, 0, mPaint);
			mCanvasMain.drawBitmap(mMirrorsBitmap, 0, 0, mPaint);
	
			if (mDrawLaserCover)
				mCanvasMain.drawBitmap(mLaserCoverBitmap, 0, 0, mPaint);
	
			mCanvasMain.drawBitmap(mFrameGlowBitmap, 0, 0, mPaint);
		}
		rviews.setImageViewBitmap(R.id.block, mMainBitmap);

		appWidgetManager.updateAppWidget(mWidgetId, rviews);
	}

	//drawing functions
	private void drawCover()
	{
		mCoverBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasCoverBitmap = new Canvas(mCoverBitmap);

		mPaint.setColor(0xFF0C141F);
		canvasCoverBitmap.drawRect(0, 0, mWidth, mHeight, mPaint);

		//draw grid
		mPaint.setColor(0xFF1B374F);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(0.8f);
		float gridSize = 20 * scale;

		float grid = gridSize;
		while (grid < (mWidth * scale))
		{
			canvasCoverBitmap.drawLine(grid, 0, grid, mHeight, mPaint);
			grid += gridSize;
		}

		grid = gridSize;
		while (grid < (mHeight * scale))
		{
			canvasCoverBitmap.drawLine(0, grid, mWidth, grid, mPaint);
			grid += gridSize;
		}

		mPaint.setStyle(Paint.Style.FILL);
	}

	private void drawLaserCover()
	{
		mLaserCoverBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasLaserCoverBitmap = new Canvas(mLaserCoverBitmap);

		Path path = new Path();
		path.addRect(new RectF(0, 0, mWidth, mHeight), Direction.CW);

		RectF windowRect = new RectF(54 * digitscale, 14 * digitscale, 141 * digitscale, 179 * digitscale);

		windowRect.offset(mOffsetX * digitscale, mOffsetY * digitscale);
		path.addRect(windowRect, Direction.CCW);

		windowRect.offset((mDigitOffsetX - 25) * digitscale, 0);
		path.addRect(windowRect, Direction.CCW);

		windowRect.offset((mTenOffsetX - mDigitOffsetX + 25) * digitscale, 0);
		path.addRect(windowRect, Direction.CCW);

		windowRect.offset((mDigitOffsetX - 25) * digitscale, 0);
		path.addRect(windowRect, Direction.CCW);

		windowRect.offset((mTenOffsetX - mDigitOffsetX + 25) * digitscale, 0);
		path.addRect(windowRect, Direction.CCW);

		windowRect.offset((mDigitOffsetX - 25) * digitscale, 0);
		path.addRect(windowRect, Direction.CCW);

		path.setFillType(Path.FillType.WINDING);
		mPaint.setColor(0xAA000000);
		canvasLaserCoverBitmap.drawPath(path, mPaint);

	}

	private void drawFrameGlow()
	{
		mFrameGlowBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasFrameGlowBitmap = new Canvas(mFrameGlowBitmap);

		mPaintBlur.setColor(0xFF009BDD);
		canvasFrameGlowBitmap.drawRect(0, 0, mWidth, mHeight, mPaintBlur);

	}

	private static void setAngles(int timeparam, float[] angles1, float[] angles2, int param4)
	{
		int tens = timeparam / 10;
		int digits = timeparam % 10;

		for (int i = 0; i < 17; i++)
		{
			angles1[i] = 0;
			angles2[i] = 0;
		}

		angles2[16] = 90;

		switch (tens)
		{
			case 0:
			{
				angles1[2] = angles1[9] = -45;
				angles1[3] = angles1[6] = angles1[7] = 45;
				angles1[4] = angles1[5] = 90;
				break;
			}
			case 1:
			{
				angles1[3] = 22.5f;
				angles1[4] = -22.5f;
				angles1[5] = -90;
				angles1[7] = 45;

				break;
			}
			case 2:
			{
				angles1[3] = 45;
				angles1[5] = -67.5f;
				angles1[6] = 67.5f;

				break;
			}
			case 3:
			{
				angles1[3] = angles1[4] = angles1[5] = angles1[16] = 67.5f;
				angles1[6] = -45;
				break;
			}
			case 4:
			{
				angles1[0] = angles1[9] = -45;
				angles1[1] = angles1[4] = angles1[5] = 45;
				angles1[2] = angles1[3] = angles1[7] = -90;
				break;
			}
			case 5:
			{
				angles1[2] = angles1[7] = -45;
				angles1[4] = angles1[5] = 45;
				break;
			}
			case 6:
			{
				angles1[3] = -22.5f;
				angles1[4] = 67.5f;
				angles1[5] = angles1[6] = 45f;
				angles1[6] = -45;
				break;
			}
			case 7:
			{
				angles1[3] = 67.5f;
				angles1[4] = -67.5f;
				angles1[6] = -90;
				angles1[8] = 45;
				break;
			}
			case 8:
			{
				angles1[2] = angles1[7] = -45;
				angles1[3] = angles1[4] = angles1[5] = angles1[6] = 45;
				break;
			}
			case 9:
			{
				angles1[2] = -45;
				angles1[3] = angles1[4] = 45;
				angles1[5] = 67.5f;
				angles1[6] = -22.5f;
				break;
			}
		}

		switch (digits)
		{
			case 0:
			{
				angles2[2] = angles2[9] = -45;
				angles2[3] = angles2[6] = angles2[7] = 45;
				angles2[4] = angles2[5] = 90;
				break;
			}
			case 1:

			{
				angles2[3] = 22.5f;
				angles2[4] = -22.5f;
				angles2[5] = -90;
				angles2[7] = 45;
				break;
			}
			case 2:
			{
				angles2[3] = 45;
				angles2[5] = -67.5f;
				angles2[6] = 67.5f;

				break;
			}
			case 3:
			{
				angles2[3] = angles2[4] = angles2[5] = 67.5f;
				angles2[6] = -45;
				angles2[15] = 67.5f;

				break;
			}
			case 4:
			{
				angles2[0] = angles2[9] = -45;
				angles2[1] = angles2[4] = angles2[5] = 45;
				angles2[2] = angles2[3] = angles2[7] = -90;
				break;
			}
			case 5:
			{
				angles2[2] = angles2[7] = -45;
				angles2[4] = angles2[5] = 45;
				break;
			}
			case 6:
			{
				angles2[3] = -22.5f;
				angles2[4] = 67.5f;
				angles2[5] = angles2[6] = 45;
				angles2[7] = -45;
				break;
			}
			case 7:
			{
				angles2[3] = 67.5f;
				angles2[4] = -67.5f;
				angles2[6] = -90;
				angles2[8] = 45;
				break;
			}
			case 8:
			{
				angles2[2] = angles2[7] = -45;
				angles2[3] = angles2[4] = angles2[5] = angles2[6] = 45;
				break;
			}
			case 9:
			{
				angles2[2] = -45;
				angles2[3] = angles2[4] = 45;
				angles2[5] = 67.5f;
				angles2[6] = -22.5f;
				break;
			}
		}
		if (tens == 0 && digits == 0)
		{
			angles2[6] = -45;
			angles2[7] = -45;
			angles2[8] = 45;
			angles2[9] = 0;
		}
		if ((tens == 0 || tens == 1 || tens == 2) && digits == 1)
		{
			angles2[12] = -45;
			angles2[13] = -45;
		}
		if ((tens == 0 || tens == 1 || tens == 2) && digits == 2)
		{
			angles2[10] = -45;
			angles2[11] = -90;
			angles2[12] = -90;
			angles2[13] = -45;
		}
		if ((tens == 0 || tens == 1 || tens == 2) && digits == 3)
		{
			angles2[10] = -45;
			angles2[11] = -90;
			angles2[12] = -90;
			angles2[13] = -45;
		}
		if (tens == 0 && digits == 4)
		{
			angles2[13] = 45;
			angles2[14] = 45;
		}
		if ((tens == 0 || tens == 1 || tens == 2) && digits == 6)
		{
			angles2[11] = -22.5f;
			angles2[12] = -90;
			angles2[13] = -45;
		}
		if ((tens == 0 || tens == 1 || tens == 2) && digits == 7)
		{
			angles2[8] = 45;
			angles2[10] = -45;
			angles2[11] = -90;
			angles2[12] = -90;
			angles2[13] = -45;
		}
		if ((tens == 0 || tens == 1 || tens == 2) && digits == 8)
		{
			angles2[12] = -45;
			angles2[13] = -45;
		}
		if ((tens == 1 || tens == 2) && digits == 0)
		{
			angles2[6] = -45;
			angles2[7] = -45;
			angles2[8] = 45;
			angles2[9] = 0;
		}
		if (tens == 1 && digits == 4)
		{
			angles1[7] = -90;
			angles1[9] = 45;
		}
		if (tens == 2 && digits == 4)
		{
			angles2[13] = 45;
			angles2[14] = 45;
		}
		if (tens == 3 && digits == 0)
		{
			angles2[13] = -45;
			angles2[14] = -45;
			angles2[6] = -45;
			angles2[7] = -45;
			angles2[8] = 45;
			angles2[9] = 0;
		}
		if (tens == 3 && (digits == 1 || digits == 8))
		{
			angles2[12] = -45;
			angles2[13] = -90;
			angles2[14] = -45;
		}
		if (tens == 3 && (digits == 2 || digits == 3 || digits == 7))
		{
			angles2[10] = -45;
			angles2[11] = -90;
			angles2[12] = -90;
			angles2[13] = -90;
			angles2[14] = -45;
		}
		if (tens == 3 && digits == 5)
		{
			angles2[13] = -45;
			angles2[14] = -45;
		}
		if (tens == 3 && digits == 9)
		{
			angles2[15] = -22.5f;
			angles2[6] = -45;
		}
		if (tens == 3 && digits == 6)
		{
			angles2[11] = -22.5f;
			angles2[12] = -90;
			angles2[13] = -90;
			angles2[14] = -45;
		}
		if (tens == 4 && digits == 0)
		{
			angles2[12] = 45;
			angles2[13] = 45;
			angles2[6] = -45;
			angles2[7] = -45;
			angles2[8] = 45;
			angles2[9] = 0;
		}
		if (tens == 4 && (digits == 2 || digits == 3 || digits == 7))
		{
			angles2[10] = -45;
			angles2[11] = -90;
			angles2[12] = -45;
		}
		if (tens == 4 && digits == 4)
		{
			angles2[12] = 45;
			angles2[13] = -90;
			angles2[14] = 45;
		}
		if (tens == 4 && (digits == 5 || digits == 9))
		{
			angles2[12] = 45;
			angles2[13] = 45;
		}
		if (tens == 4 && digits == 6)
		{
			angles2[11] = -22.5f;
			angles2[12] = -45;
		}
		if (tens == 5 && digits == 0)
		{
			angles2[10] = 45;
			angles2[11] = -90;
			angles2[12] = -90;
			angles2[13] = 45;
			angles2[6] = -45;
			angles2[7] = -45;
			angles2[8] = 45;
			angles2[9] = 0;
		}
		if (tens == 5 && (digits == 1 || digits == 8))
		{
			angles2[10] = 45;
			angles2[11] = -90;
			angles2[12] = 45;
		}
		if (tens == 5 && digits == 4)
		{
			angles2[10] = 45;
			angles2[11] = -90;
			angles2[12] = -90;
			angles2[13] = -90;
			angles2[14] = 45;
		}
		if (tens == 5 && (digits == 5 || digits == 9))
		{
			angles2[10] = 45;
			angles2[11] = -90;
			angles2[12] = -90;
			angles2[13] = 45;
		}
		if (tens == 5 && digits == 6)
		{
			angles2[10] = 45;
			angles2[11] = 67.5f;
		}
		if (tens == 1 && param4 == 0)
		{
			angles1[12] = -45;
			angles1[13] = -90;
			angles1[14] = -90;
			angles1[15] = -45;
		}
		if ((tens == 2 || tens == 3) && param4 == 0)
		{
			angles1[10] = -45;
			angles1[11] = -90;
			angles1[12] = -90;
			angles1[13] = -90;
			angles1[14] = -90;
			angles1[15] = -45;
		}
		if (tens == 5 && param4 == 0)
		{
			angles1[14] = -45;
			angles1[15] = -45;
		}
		if ((param4 == 1 || param4 == 2) && (tens == 0 || tens == 4))
		{
			angles1[14] = 45;
			angles1[15] = 45;
		}
		if ((param4 == 1 || param4 == 2) && tens == 1)
		{
			angles1[12] = -45;
			angles1[13] = -90;
			angles1[14] = -45;
		}
		if ((param4 == 1 || param4 == 2) && (tens == 2 || tens == 3))
		{
			angles1[10] = -45;
			angles1[11] = -90;
			angles1[12] = -90;
			angles1[13] = -90;
			angles1[14] = -45;
		}
		if ((param4 == 3 || param4 == 7) && tens == 1)
		{
			angles1[12] = -45;
			angles1[13] = -90;
			angles1[14] = -90;
			angles1[15] = -45;
		}
		if ((param4 == 3 || param4 == 7) && (tens == 2 || tens == 3))
		{
			angles1[10] = -45;
			angles1[11] = -90;
			angles1[12] = -90;
			angles1[13] = -90;
			angles1[14] = -90;
			angles1[15] = -45;
		}
		if ((param4 == 3 || param4 == 7) && tens == 5)
		{
			angles1[14] = -45;
			angles1[15] = -45;
		}
		if ((param4 == 4 || param4 == 8) && (tens == 0 || tens == 4))
		{
			angles1[12] = 45;
			angles1[13] = -90;
			angles1[14] = -90;
			angles1[15] = 45;
		}
		if ((param4 == 4 || param4 == 8) && (tens == 2 || tens == 3))
		{
			angles1[12] = -45;
			angles1[11] = -90;
			angles1[10] = -45;
		}
		if ((param4 == 4 || param4 == 8) && tens == 5)
		{
			angles1[12] = 45;
			angles1[13] = -90;
			angles1[14] = 45;
		}
		if ((param4 == 5 || param4 == 6) && (tens == 0 || tens == 4))
		{
			angles1[10] = 45;
			angles1[11] = -90;
			angles1[12] = -90;
			angles1[13] = -90;
			angles1[14] = -90;
			angles1[15] = 45;
		}
		if ((param4 == 5 || param4 == 6) && tens == 1)
		{
			angles1[10] = 45;
			angles1[11] = -90;
			angles1[12] = 45;
		}
		if ((param4 == 5 || param4 == 6) && tens == 5)
		{
			angles1[10] = 45;
			angles1[11] = -90;
			angles1[12] = -90;
			angles1[13] = -90;
			angles1[14] = 45;
		}
		if (param4 == 9 && (tens == 0 || tens == 4))
		{
			angles1[13] = 67.5f;
			angles1[14] = -90;
			angles1[15] = 45;
		}
		if (param4 == 9 && tens == 1)
		{
			angles1[13] = -22.5f;
			angles1[12] = -45;
		}
		if (param4 == 9 && (tens == 2 || tens == 3))
		{
			angles1[13] = -22.5f;
			angles1[12] = -90;
			angles1[11] = -90;
			angles1[10] = -45;
		}
		if (param4 == 9 && tens == 5)
		{
			angles1[13] = 67.5f;
			angles1[14] = 45;
		}
	}
	
	private boolean adjustAngles()
	{
		mNeedRedraw = false;

		mCurrentTime.setToNow();

		switch (mTimeFormat)
		{
			case 0:
				mCurrentDigits[0] = mCurrentTime.month + 1;
				mCurrentDigits[1] = mCurrentTime.monthDay;
				mCurrentDigits[2] = mCurrentTime.year % 100;
				break;
			case 1:
				mCurrentDigits[0] = mCurrentTime.monthDay;
				mCurrentDigits[1] = mCurrentTime.month + 1;
				mCurrentDigits[2] = mCurrentTime.year % 100;
				break;
			case 2:
			default:
				mCurrentDigits[0] = mCurrentTime.hour;
				mCurrentDigits[1] = mCurrentTime.minute;
				mCurrentDigits[2] = mCurrentTime.second;
				break;
		}

		if (mCurrentDigits[0] != mOldDigits[0] || mCurrentDigits[1] != mOldDigits[1] || mCurrentDigits[2] != mOldDigits[2])
		{
			mNeedRedraw = true;

			mOldDigits[0] = mCurrentDigits[0];
			mOldDigits[1] = mCurrentDigits[1];
			mOldDigits[2] = mCurrentDigits[2];
			

			setAngles(mCurrentDigits[0], mTargetAngles[0], mTargetAngles[1], 0);
			setAngles(mCurrentDigits[1], mTargetAngles[2], mTargetAngles[3], mCurrentDigits[0] % 10);
			setAngles(mCurrentDigits[2], mTargetAngles[4], mTargetAngles[5], mCurrentDigits[1] % 10);

			for (int j = 0; j < 6; j++)
			{
				for (int i = 0; i < 17; i++)
				{
					if (Math.abs(mCurrentAngles[j][i] - mTargetAngles[j][i]) > 5)
					{
						mTurnAngles[j][i] = (mTargetAngles[j][i] - mCurrentAngles[j][i]) / 3.0f;
					}
				}
			}
		}
		
		for (int j = 0; j < 6; j++)
		{
			for (int i = 0; i < 17; i++)
			{
				if (Math.abs(mCurrentAngles[j][i] - mTargetAngles[j][i]) > 5)
				{
					mNeedRedraw = true;
					mCurrentAngles[j][i] += mTurnAngles[j][i];
				}
				else
				{
					mCurrentAngles[j][i] = mTargetAngles[j][i];
				}

			}
		}

		return mNeedRedraw;
	}

	private void drawMirrorsLaser()
	{

		{
			mLaserPath.reset();
			//propagate laser beam
			{
				mLaserX = 5 * scale;
				mLaserY = (189 + mOffsetY) * digitscale;

				mLaserPath.moveTo(mLaserX, mLaserY);
				mLaserRotation = 0;

				mDigit = -1;
				mMirror = -1;
				//tens
				while (mLaserX >= (5 * scale) && mLaserX <= ((mWidth - 5) * scale) && mLaserY > (5 * scale) && mLaserY < ((mHeight - 5) * scale))
				{

					mLaserX += (float) (Math.cos(mLaserRotation / 180.0f * Math.PI));
					mLaserY += (float) (Math.sin(mLaserRotation / 180.0f * Math.PI));
					mMirrorFound = false;

					for (int j = 0; j < 6; j++)
					{
						for (int i = 0; i < 17; i++)
						{
							if (Math.abs(mMirrorCoordinates[j][0][i] - mLaserX) < eps && Math.abs(mMirrorCoordinates[j][1][i] - mLaserY) < eps)
							{
								if (mMirror != i || mDigit != j)
								{
									mMirror = i;
									mDigit = j;
									mMirrorFound = true;
									mLaserX = mMirrorCoordinates[j][0][i];
									mLaserY = mMirrorCoordinates[j][1][i];

									if (mTimeFormat == 2)
									{
										if (mCurrentAngles[j][i] == 0 || mCurrentAngles[j][i] == 90 || mCurrentAngles[j][i] == 45 || mCurrentAngles[j][i] == -45 || mCurrentAngles[j][i] == 22.5 || mCurrentAngles[j][i] == -22.5 || mCurrentAngles[j][i] == -90 || mCurrentAngles[j][i] == -67.5 || mCurrentAngles[j][i] == 67.5)
										{
											mLaserPath.lineTo(mLaserX, mLaserY);
											mLaserRotation = 2.0f * mCurrentAngles[j][i] - mLaserRotation;
										}
									}
									else
									{
										mLaserPath.lineTo(mLaserX, mLaserY);
										mLaserRotation = 2.0f * mTargetAngles[j][i] - mLaserRotation;
									}
									break;
								}
							}
						}

						if (mMirrorFound)
							break;
					}
				}
				mLaserPath.lineTo(mLaserX, mLaserY);
			}

			for (int j = 0; j < 6; j++)
			{
				for (int i = 0; i < 17; i++)
				{
					drawPixelMirror(mMirrorCoordinates[j][0][i], mMirrorCoordinates[j][1][i], mCurrentAngles[j][i]);
				}
			}

			mPaint.setColor(mLaserColor);
			mPaint.setStyle(Paint.Style.STROKE);
			mCanvasLaser.drawPath(mLaserPath, mPaint);
			mPaint.setStyle(Paint.Style.FILL);

			mPaintBlur.setColor(mLaserColor & 0x00FFFFFF + 0x99000000);
			mCanvasLaser.drawPath(mLaserPath, mPaintBlur);
		}
	}

	private void drawPixelMirror(float centerX, float centerY, float angleDeg)
	{
		mPaint.setStrokeWidth(2);
		mPaint.setColor(0xFFE6FFFF);
		float startX = centerX - mMirrorLength * (float) Math.cos(angleDeg * Math.PI / 180.0f);
		float startY = centerY - mMirrorLength * (float) Math.sin(angleDeg * Math.PI / 180.0f);
		float stopX = centerX + mMirrorLength * (float) Math.cos(angleDeg * Math.PI / 180.0f);
		float stopY = centerY + mMirrorLength * (float) Math.sin(angleDeg * Math.PI / 180.0f);

		mCanvasMirrors.drawLine(startX, startY, stopX, stopY, mPaint);
	}
}
