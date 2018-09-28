package ca.jvsh.reflectius;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;

public class ReflectiusView
{
	public static final String	INTENT_ON_CLICK_FORMAT	= "ca.jvsh.reflectius.id.%d.click";
	private int					mRefreshRate			= 40;

	private int					mHeight;
	private int					mWidth;
	private float				mDensity;

	private static float		scale;
	private static float		eps;
	float						mMirrorLength;

	private long				mLastRedrawMillis		= 0;
	private int					mWidgetId;

	private final Paint			mPaint					= new Paint();
	private final Paint			mPaintBlur				= new Paint();

	Bitmap						mMainBitmap;
	Canvas						mCanvasMain;

	Bitmap						mCoverBitmap;
	Bitmap						mCoverGradientBitmap;
	Bitmap						mCoverReflectionBitmap;
	Bitmap						mLaserCoverBitmap;

	Bitmap						mMirrorsBitmap;
	Canvas						mCanvasMirrors;

	Bitmap						mLaserBitmap;
	Canvas						mCanvasLaser;

	Path						mCoverPath;

	int[]						mOldDigits				= new int[2];
	int[]						mCurrentDigits			= new int[2];

	float[][]					mTargetAngles			= new float[4][17];
	float[][]					mCurrentAngles			= new float[4][17];
	float[][]					mTurnAngles				= new float[4][17];

	float[][][]					mMirrorCoordinates		= new float[4][2][17];
	Time						mCurrentTime			= new Time();
	float						mLaserX;
	float						mLaserY;
	float						mLaserRotation;
	boolean						mMirrorFound;

	int							mDigit;
	int							mMirror;
	Path						mLaserPath				= new Path();
	int							mTimeFormat				= -1;
	int 						mLaserColor				= 0xFFFF0000;

	public ReflectiusView(Context context, int widgetId)
	{
		DisplayMetrics metrics = ReflectiusWidgetApp.getMetrics();

		mDensity = metrics.density;
		mWidth = (int) (400 * metrics.density);
		mHeight = (int) (200 * metrics.density);

		scale = (400 * metrics.density) / 663.0f;
		eps = 1.3f * scale;
		mMirrorLength = 5 * scale;

		mWidgetId = widgetId;
		
		
		setState();

		
		//create cover path
		{
			mCoverPath = new Path();
			mCoverPath.moveTo(0.2f * scale, 139 * scale);
			mCoverPath.lineTo(367 * scale, 8 * scale);
			mCoverPath.lineTo(607 * scale, 60 * scale);
			mCoverPath.lineTo(664 * scale, 132 * scale);
			mCoverPath.lineTo(571 * scale, 322 * scale);
			mCoverPath.lineTo(385 * scale, 291 * scale);
			mCoverPath.lineTo(107 * scale, 321 * scale);
			mCoverPath.close();
		}

		//set Paint variables
		{
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);

			mPaintBlur.set(mPaint);
			mPaintBlur.setStyle(Paint.Style.STROKE);
			mPaintBlur.setStrokeWidth(45f * scale);
			mPaintBlur.setMaskFilter(new BlurMaskFilter(45 * scale, BlurMaskFilter.Blur.NORMAL));
		}

		mMainBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasMain = new Canvas(mMainBitmap);

		mLaserBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasLaser = new Canvas(mLaserBitmap);

		mMirrorsBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasMirrors = new Canvas(mMirrorsBitmap);

		drawCover();

		drawLaserCover();

		drawCoverGradient();

		drawCoverReflection();

		//set mirror coordinates
		float[][] tens = { { 60, 132, 60, 132, 60, 132, 60, 132, 60, 132, 11, 11, 11, 11, 11, 11, 39 }, { 6, 6, 24, 24, 96, 96, 168, 168, 189, 189, 24, 69, 96, 113, 168, 189, 189 } };

		float[][] digits = { { 38, 110, 38, 110, 38, 110, 38, 110, 38, 110, 11, 11, 11, 11, 11, 17, 11 }, { 6, 6, 24, 24, 96, 96, 168, 168, 189, 189, 24, 69, 96, 168, 189, 189, 113 } };

		//set x and y offsets
		for (int i = 0; i < 17; i++)
		{
			for (int k = 0; k < 2; k++)
			{
				mMirrorCoordinates[2][k][i] = mMirrorCoordinates[0][k][i] = tens[k][i];
				mMirrorCoordinates[3][k][i] = mMirrorCoordinates[1][k][i] = digits[k][i];
			}

			mMirrorCoordinates[0][1][i] = tens[1][i];

			mMirrorCoordinates[0][0][i] += 110;
			mMirrorCoordinates[1][0][i] += 250;
			mMirrorCoordinates[2][0][i] += 365;
			mMirrorCoordinates[3][0][i] += 505;

			for (int j = 0; j < 4; j++)
			{
				mMirrorCoordinates[j][1][i] += 120;

				mMirrorCoordinates[j][0][i] *= scale * 0.89f;
				mMirrorCoordinates[j][1][i] *= scale * 0.89f;
			}
		}
	}

	public Context getContext()
	{
		return (ReflectiusWidgetApp.getApplication());
	}

	public float getDensity()
	{
		return mDensity;
	}

	public int getmWidgetId()
	{
		return mWidgetId;
	}

	public void OnClick()
	{
	}

	public void Redraw(Context context)
	{
		if(mTimeFormat == -1)
		{
			SharedPreferences prefs = getContext().getSharedPreferences("prefs", 0);
			mTimeFormat = prefs.getInt("timeformat" + mWidgetId, -1);
			
			switch (mTimeFormat)
			{
				case 0:
					mRefreshRate = 1000;
					break;
				case 1:
					mRefreshRate = 1000;
					break;
				case 2:
					mRefreshRate = 40;
					break;
			}
			
			mLaserColor = prefs.getInt("color"+ mWidgetId, 0xffff0000);

		}
		
		RemoteViews rviews = new RemoteViews(context.getPackageName(), R.layout.reflectius_widget);

		mCanvasMain.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
		mCanvasLaser.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
		mCanvasMirrors.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

		mCanvasMain.drawBitmap(mCoverBitmap, 0, 0, mPaint);

		drawMirrorsLaser();

		mCanvasMain.drawBitmap(mLaserBitmap, 0, 0, mPaint);
		mCanvasMain.drawBitmap(mMirrorsBitmap, 0, 0, mPaint);

		mCanvasMain.drawBitmap(mLaserCoverBitmap, 0, 0, mPaint);
		mCanvasMain.drawBitmap(mCoverGradientBitmap, 0, 0, mPaint);
		mCanvasMain.drawBitmap(mCoverReflectionBitmap, 0, 0, mPaint);

		rviews.setImageViewBitmap(R.id.block, mMainBitmap);

		updateClickIntent(rviews);
		AppWidgetManager.getInstance(context).updateAppWidget(mWidgetId, rviews);
		mLastRedrawMillis = SystemClock.uptimeMillis();

		scheduleRedraw();
	}

	private void scheduleRedraw()
	{
		long nextRedraw = mLastRedrawMillis + mRefreshRate;
		nextRedraw = nextRedraw > SystemClock.uptimeMillis() ? nextRedraw : SystemClock.uptimeMillis() + mRefreshRate;
		scheduleRedrawAt(nextRedraw);
	}

	private void scheduleRedrawAt(long timeMillis)
	{
		(new Handler()).postAtTime(new Runnable()
		{
			public void run()
			{
				Redraw(getContext());
			}
		}, timeMillis);
	}

	public void setState()
	{
		scheduleRedraw();
	}

	private void updateClickIntent(RemoteViews rviews)
	{
		Intent intent = new Intent(String.format(INTENT_ON_CLICK_FORMAT, mWidgetId));
		intent.setClass(getContext(), ReflectiusWidgetProvider.class);
		intent.putExtra("widgetId", mWidgetId);
		PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		rviews.setOnClickPendingIntent(R.id.widget, pi);
	}

	//drawing functions
	private void drawCover()
	{
		mCoverBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasCoverBitmap = new Canvas(mCoverBitmap);

		mPaint.setColor(0xFF0D091D);

		canvasCoverBitmap.drawPath(mCoverPath, mPaint);
	}

	private void drawCoverGradient()
	{
		mCoverGradientBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasCoverGradientBitmap = new Canvas(mCoverGradientBitmap);

		int[] colors = { 0x29A0B5EA, 0x00A0B5EA };
		float[] positions = { 0.0627f, 0.8f };
		LinearGradient gradient = new LinearGradient(0, 0, 0, 600 * scale, colors, positions, android.graphics.Shader.TileMode.CLAMP);

		mPaint.setColor(0xFFFFFFFF);
		mPaint.setShader(gradient);

		canvasCoverGradientBitmap.drawPath(mCoverPath, mPaint);
		mPaint.setShader(null);
	}

	private void drawCoverReflection()
	{
		mCoverReflectionBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasBitmapCoverReflection = new Canvas(mCoverReflectionBitmap);

		int[] colors = { 0x33A6B3EB, 0x10A6B3EB, 0x33A6B3EB, 0x00A6B3EB };
		float[] positions = { 0.0627f, 0.274f, 0.667f, 0.8f };
		LinearGradient gradient = new LinearGradient(0, 0, 720 * scale, 200 * scale, colors, positions, android.graphics.Shader.TileMode.CLAMP);
		mPaint.setColor(0xFFFFFFFF);
		mPaint.setShader(gradient);
		mPaint.setMaskFilter(new BlurMaskFilter(1.0f * scale, Blur.INNER));

		Path coverReflectionPath = new Path();
		coverReflectionPath.moveTo(57 * scale, 239 * scale);

		coverReflectionPath.lineTo(614 * scale, 239 * scale);

		coverReflectionPath.lineTo(571 * scale, 322 * scale);
		coverReflectionPath.lineTo(385 * scale, 291 * scale);
		coverReflectionPath.lineTo(107 * scale, 321 * scale);
		coverReflectionPath.close();

		canvasBitmapCoverReflection.drawPath(coverReflectionPath, mPaint);
		mPaint.setShader(null);
		mPaint.setMaskFilter(null);
	}

	private void drawLaserCover()
	{
		mLaserCoverBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasLaserCoverBitmap = new Canvas(mLaserCoverBitmap);

		mPaint.setColor(0xFF0D091D);

		Path hidePath = new Path();
		hidePath.moveTo(0.2f * scale, 139 * scale);
		hidePath.lineTo(120 * scale, 100 * scale);

		hidePath.lineTo(120 * scale, 319 * scale);
		hidePath.lineTo(107 * scale, 321 * scale);
		hidePath.close();

		canvasLaserCoverBitmap.drawPath(hidePath, mPaint);

		Path path = new Path();
		path.addPath(mCoverPath);
		path.addRoundRect(new RectF(144.18f * scale, 120.15f * scale, 224.28f * scale, 267 * scale), 17.8f * scale, 17.8f * scale, Direction.CCW);
		path.addRoundRect(new RectF(247.42f * scale, 120.15f * scale, 327.52f * scale, 267 * scale), 17.8f * scale, 17.8f * scale, Direction.CCW);
		path.addRoundRect(new RectF(369.35f * scale, 120.15f * scale, 449.45f * scale, 267 * scale), 17.8f * scale, 17.8f * scale, Direction.CCW);
		path.addRoundRect(new RectF(472.59f * scale, 120.15f * scale, 552.69f * scale, 267 * scale), 17.8f * scale, 17.8f * scale, Direction.CCW);

		path.setFillType(Path.FillType.WINDING);
		mPaint.setColor(0xCC000000);
		canvasLaserCoverBitmap.drawPath(path, mPaint);

	}

	void setAngles(int timeparam, float[] angles1, float[] angles2, int param4)
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

	private void drawMirrorsLaser()
	{

		mCurrentTime.setToNow();

		switch (mTimeFormat)
		{
			case 0:
				mCurrentDigits[0] = mCurrentTime.month + 1;
				mCurrentDigits[1] = mCurrentTime.monthDay;
				break;
			case 1:
				mCurrentDigits[0] = mCurrentTime.hour;
				mCurrentDigits[1] = mCurrentTime.minute;
				break;
			case 2:
			default:
				mCurrentDigits[0] = mCurrentTime.minute;
				mCurrentDigits[1] = mCurrentTime.second;
				break;
		}

		if (mCurrentDigits[0] != mOldDigits[0] || mCurrentDigits[1] != mOldDigits[1])
		{
			mOldDigits[0] = mCurrentDigits[0];
			mOldDigits[1] = mCurrentDigits[1];

			setAngles(mCurrentDigits[0], mTargetAngles[0], mTargetAngles[1], 0);
			setAngles(mCurrentDigits[1], mTargetAngles[2], mTargetAngles[3], mCurrentDigits[0] % 10);

			for (int j = 0; j < 4; j++)
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
		
		for (int j = 0; j < 4; j++)
		{
			for (int i = 0; i < 17; i++)
			{
				if (Math.abs(mCurrentAngles[j][i] - mTargetAngles[j][i]) > 5)
				{
					mCurrentAngles[j][i] += mTurnAngles[j][i];
				}
				else
				{
					mCurrentAngles[j][i] = mTargetAngles[j][i];
				}
				
				
			}
		}

		{
			mLaserPath.reset();
			//propagate laser beam
			{
				mLaserX = 114 * scale *0.89f;
				mLaserY = 309 * scale *0.89f;

				mLaserPath.moveTo(mLaserX, mLaserY);
				mLaserRotation = 0;

				mDigit = -1;
				mMirror = -1;
				//tens
				while (mLaserX > (98 * scale) && mLaserX < (580 * scale) && mLaserY > (107 * scale) && mLaserY < (294 * scale))
				{

					mLaserX += (float) (Math.cos(mLaserRotation / 180.0f * Math.PI));
					mLaserY += (float) (Math.sin(mLaserRotation / 180.0f * Math.PI));
					mMirrorFound = false;

					for (int j = 0; j < 4; j++)
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

									if(mTimeFormat == 2)
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

			for (int j = 0; j < 4; j++)
			{
				for (int i = 0; i < 17; i++)
				{
					if (Math.abs(mCurrentAngles[j][i] - mTargetAngles[j][i]) > 5)
					{
						mCurrentAngles[j][i] += mTurnAngles[j][i];
					}
					else
					{
						mCurrentAngles[j][i] = mTargetAngles[j][i];
					}
					
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
		mPaint.setColor(0xFFDEDEDE);
		float startX = centerX - mMirrorLength * (float) Math.cos(angleDeg * Math.PI / 180.0f);
		float startY = centerY - mMirrorLength * (float) Math.sin(angleDeg * Math.PI / 180.0f);
		float stopX = centerX + mMirrorLength * (float) Math.cos(angleDeg * Math.PI / 180.0f);
		float stopY = centerY + mMirrorLength * (float) Math.sin(angleDeg * Math.PI / 180.0f);

		mCanvasMirrors.drawLine(startX, startY, stopX, stopY, mPaint);
	}
}
