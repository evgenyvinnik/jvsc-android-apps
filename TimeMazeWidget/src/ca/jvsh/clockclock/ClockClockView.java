package ca.jvsh.clockclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;

public class ClockClockView
{
	private static final int	HORIZONTAL_DIALS		= 2;
	private static final int	VERTICAL_DIALS			= 3;
	private static final int	DIAL_ARROWS				= 2;

	private static final int	SEGMENTS				= 2;
	private static final int	DIGIT_PER_SEGMENT		= 2;

	private static final int	CLOCK_DIGITS			= SEGMENTS * DIGIT_PER_SEGMENT;

	private static final int	COORDINATES				= 2;

	private int					mHeight;
	private int					mWidth;
	private float				mDensity;

	private int					mWidgetId;

	private final Paint			mPaint					= new Paint();

	Bitmap						mMainBitmap;
	Canvas						mCanvasMain;

	Bitmap						mCoverBitmap;

	Bitmap						mArrowsBitmap;
	Canvas						mCanvasArrows;

	int[]						mOldDigits				= new int[SEGMENTS];
	int[]						mCurrentDigits			= new int[SEGMENTS];

	float[][][][]				mTargetAngles			= new float[CLOCK_DIGITS][VERTICAL_DIALS][HORIZONTAL_DIALS][DIAL_ARROWS];
	float[][][][]				mCurrentAngles			= new float[CLOCK_DIGITS][VERTICAL_DIALS][HORIZONTAL_DIALS][DIAL_ARROWS];
	int[][][][]					mMadeCircle				= new int[CLOCK_DIGITS][VERTICAL_DIALS][HORIZONTAL_DIALS][DIAL_ARROWS];
	private final float[]		mTurnAngles				= { 22.5f / 12.0f, 22.5f };
	private final int[]			mHandTurns				= { 1, 12 };

	float[][][][]				mDialCenterCoordinates	= new float[CLOCK_DIGITS][VERTICAL_DIALS][HORIZONTAL_DIALS][COORDINATES];

	private final float			mOffsetX				= 3;
	private final float			mOffsetY				= 3;
	private final float			mDialSize				= 42;
	private final float			mDialSizeHalf			= mDialSize / 2;
	private final float			mTensOffsetX			= 21;
	private final float			mDigitOffsetX			= 7;
	private final float[]		mHandLengths			= { mDialSizeHalf * 0.8f, mDialSizeHalf * 0.9f };
	private final float			eps						= 0.4f;

	private final float			mFrameWidth				= 3;

	Time						mCurrentTime			= new Time();

	int							mHandsColor				= 0xFF010101;
	boolean						mNeedRedraw				= false;
	SharedPreferences 			mPrefs;

	public ClockClockView(Context context, int widgetId)
	{
		DisplayMetrics metrics = ClockClockWidgetApp.getMetrics();

		mDensity = metrics.density;
		mWidth = (int) (400 * metrics.density);
		mHeight = (int) (200 * metrics.density);

		mWidgetId = widgetId;

		mPrefs = getContext().getSharedPreferences("prefs", 0);

		//set Paint variables
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setStrokeWidth(mFrameWidth * mDensity);

		mMainBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasMain = new Canvas(mMainBitmap);

		mArrowsBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvasArrows = new Canvas(mArrowsBitmap);

		mCoverBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas canvasCoverBitmap = new Canvas(mCoverBitmap);

		//set arrow 
		for (int i = 0; i < CLOCK_DIGITS; i++)
		{
			for (int j = 0; j < VERTICAL_DIALS; j++)
			{
				for (int k = 0; k < HORIZONTAL_DIALS; k++)
				{

					mDialCenterCoordinates[i][j][k][0] = mOffsetX + (k + i * HORIZONTAL_DIALS) * (mDialSize + mFrameWidth) + mDialSizeHalf;
					mDialCenterCoordinates[i][j][k][0] += mDigitOffsetX * (i % DIGIT_PER_SEGMENT);
					mDialCenterCoordinates[i][j][k][0] += mTensOffsetX * (i / DIGIT_PER_SEGMENT);
					mDialCenterCoordinates[i][j][k][1] = mOffsetY + j * (mDialSize + mFrameWidth) + mDialSizeHalf;

					for (int l = 0; l < COORDINATES; l++)
					{
						mDialCenterCoordinates[i][j][k][l] *= mDensity;
					}

					mPaint.setColor(0xFF000000);
					mPaint.setStyle(Paint.Style.STROKE);
					mPaint.setShader(null);
					canvasCoverBitmap.drawRect(mDialCenterCoordinates[i][j][k][0] - mDialSizeHalf * mDensity, mDialCenterCoordinates[i][j][k][1] - mDialSizeHalf, mDialCenterCoordinates[i][j][k][0] + mDialSizeHalf, mDialCenterCoordinates[i][j][k][1] + mDialSizeHalf, mPaint);

					mPaint.setStyle(Paint.Style.FILL);
					mPaint.setShader(new RadialGradient(mDialCenterCoordinates[i][j][k][0], mDialCenterCoordinates[i][j][k][1], mDialSize + mDialSizeHalf, 0xFFFFFFF0, 0xFFB5E6B5, android.graphics.Shader.TileMode.CLAMP));

					canvasCoverBitmap.drawRect(mDialCenterCoordinates[i][j][k][0] - mDialSizeHalf * mDensity, mDialCenterCoordinates[i][j][k][1] - mDialSizeHalf * mDensity, mDialCenterCoordinates[i][j][k][0] + mDialSizeHalf * mDensity, mDialCenterCoordinates[i][j][k][1] + mDialSizeHalf * mDensity, mPaint);

				}
			}
		}

		mPaint.setShader(null);
		mPaint.setColor(mHandsColor);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(1.2f * mFrameWidth * mDensity);

	}

	public Context getContext()
	{
		return (ClockClockWidgetApp.getApplication());
	}

	public void Redraw(AppWidgetManager appWidgetManager)
	{
		if (mHandsColor == 0xFF010101)
		{
			mHandsColor = mPrefs.getInt("color" + mWidgetId, 0xFF010101);
			mPaint.setColor(mHandsColor);
		}

		RemoteViews rviews = new RemoteViews(getContext().getPackageName(), R.layout.clockclock_widget);

		if (adjustAngles())
		{

			mCanvasMain.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
			mCanvasArrows.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

			mCanvasMain.drawBitmap(mCoverBitmap, 0, 0, mPaint);

			drawArrowHands();

			mCanvasMain.drawBitmap(mArrowsBitmap, 0, 0, mPaint);
		}

		rviews.setImageViewBitmap(R.id.block, mMainBitmap);

		appWidgetManager.updateAppWidget(mWidgetId, rviews);
	}

	//drawing functions
	private static void setDigitAngles(int digit, float[][][] handlesAngles)
	{
		switch (digit)
		{
			case 0:
			{
				handlesAngles[0][0][0] = 90;
				handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = 90;
				handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = 270;
				handlesAngles[1][0][1] = 90;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 90;

				handlesAngles[2][0][0] = 270;
				handlesAngles[2][0][1] = 0;
				handlesAngles[2][1][0] = 270;
				handlesAngles[2][1][1] = 180;
				break;
			}
			case 1:
			{
				handlesAngles[0][0][0] = handlesAngles[0][0][1] = 135;
				handlesAngles[0][1][0] = handlesAngles[0][1][1] = 90;

				handlesAngles[1][0][0] = handlesAngles[1][0][1] = 135;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 90;

				handlesAngles[2][0][0] = handlesAngles[2][0][1] = 135;
				handlesAngles[2][1][0] = handlesAngles[2][1][1] = 270;

				break;
			}
			case 2:
			{
				handlesAngles[0][0][0] = handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = 90;
				handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = 90;
				handlesAngles[1][0][1] = 0;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 180;

				handlesAngles[2][0][0] = 270;
				handlesAngles[2][0][1] = 0;
				handlesAngles[2][1][0] = handlesAngles[2][1][1] = 180;
				break;
			}
			case 3:
			{
				handlesAngles[0][0][0] = handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = 90;
				handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = handlesAngles[1][0][1] = 0;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 180;

				handlesAngles[2][0][0] = handlesAngles[2][0][1] = 0;
				handlesAngles[2][1][0] = 270;
				handlesAngles[2][1][1] = 180;
				break;
			}
			case 4:
			{
				handlesAngles[0][0][0] = handlesAngles[0][0][1] = 90;
				handlesAngles[0][1][0] = handlesAngles[0][1][1] = 90;

				handlesAngles[1][0][0] = 270;
				handlesAngles[1][0][1] = 0;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 90;

				handlesAngles[2][0][0] = handlesAngles[2][0][1] = 135;
				handlesAngles[2][1][0] = handlesAngles[2][1][1] = 270;

				break;
			}
			case 5:
			{
				handlesAngles[0][0][0] = 90;
				handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = 270;
				handlesAngles[1][0][1] = 0;
				handlesAngles[1][1][0] = 90;
				handlesAngles[1][1][1] = 180;

				handlesAngles[2][0][0] = handlesAngles[2][0][1] = 0;
				handlesAngles[2][1][0] = 270;
				handlesAngles[2][1][1] = 180;
				break;
			}
			case 6:
			{
				handlesAngles[0][0][0] = 90;
				handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = 270;
				handlesAngles[1][0][1] = 90;
				handlesAngles[1][1][0] = 90;
				handlesAngles[1][1][1] = 180;

				handlesAngles[2][0][0] = 270;
				handlesAngles[2][0][1] = 0;
				handlesAngles[2][1][0] = 270;
				handlesAngles[2][1][1] = 180;
				break;
			}
			case 7:
			{
				handlesAngles[0][0][0] = handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = 90;
				handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = handlesAngles[1][0][1] = 135;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 90;

				handlesAngles[2][0][0] = handlesAngles[2][0][1] = 135;
				handlesAngles[2][1][0] = handlesAngles[2][1][1] = 270;
				break;
			}
			case 8:
			{
				handlesAngles[0][0][0] = 90;
				handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = 90;
				handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = 270;
				handlesAngles[1][0][1] = 0;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 180;

				handlesAngles[2][0][0] = 270;
				handlesAngles[2][0][1] = 0;
				handlesAngles[2][1][0] = 270;
				handlesAngles[2][1][1] = 180;
				break;
			}
			case 9:
			{
				handlesAngles[0][0][0] = 90;
				handlesAngles[0][0][1] = 0;
				handlesAngles[0][1][0] = 90;
				handlesAngles[0][1][1] = 180;

				handlesAngles[1][0][0] = 270;
				handlesAngles[1][0][1] = 0;
				handlesAngles[1][1][0] = 270;
				handlesAngles[1][1][1] = 90;

				handlesAngles[2][0][0] = handlesAngles[2][0][1] = 0;
				handlesAngles[2][1][0] = 270;
				handlesAngles[2][1][1] = 180;
				break;
			}
		}

	}

	private boolean adjustAngles()
	{
		mNeedRedraw = false;

		mCurrentTime.setToNow();

		mCurrentDigits[0] = mCurrentTime.hour;
		mCurrentDigits[1] = mCurrentTime.minute;

		if (mCurrentDigits[0] != mOldDigits[0] || mCurrentDigits[1] != mOldDigits[1])
		{
			mNeedRedraw = true;

			mOldDigits[0] = mCurrentDigits[0];
			mOldDigits[1] = mCurrentDigits[1];

			setDigitAngles(mCurrentDigits[0] / 10, mTargetAngles[0]);
			setDigitAngles(mCurrentDigits[0] % 10, mTargetAngles[1]);
			setDigitAngles(mCurrentDigits[1] / 10, mTargetAngles[2]);
			setDigitAngles(mCurrentDigits[1] % 10, mTargetAngles[3]);
			for (int i = 0; i < CLOCK_DIGITS; i++)
			{
				for (int j = 0; j < VERTICAL_DIALS; j++)
				{
					for (int k = 0; k < HORIZONTAL_DIALS; k++)
					{
						for (int l = 0; l < DIAL_ARROWS; l++)
						{
							mMadeCircle[i][j][k][l] = 0;
						}
					}
				}
			}

		}

		for (int i = 0; i < CLOCK_DIGITS; i++)
		{
			for (int j = 0; j < VERTICAL_DIALS; j++)
			{
				for (int k = 0; k < HORIZONTAL_DIALS; k++)
				{
					for (int l = 0; l < DIAL_ARROWS; l++)
					{
						if (mMadeCircle[i][j][k][l] < mHandTurns[l])
						{
							mCurrentAngles[i][j][k][l] += mTurnAngles[l];
							mCurrentAngles[i][j][k][l] %= 360;
							mNeedRedraw = true;
						}

						if (l == 1)
						{
							if (mMadeCircle[i][j][k][0] >= mHandTurns[0] && mMadeCircle[i][j][k][1] < mHandTurns[1])
							{
								mMadeCircle[i][j][k][1] = mHandTurns[1] - 1;
							}
						}

						if (Math.abs(mCurrentAngles[i][j][k][l] - mTargetAngles[i][j][k][l]) < eps)
						{
							mMadeCircle[i][j][k][l]++;
						}

					}
				}
			}
		}

		return mNeedRedraw;
	}

	private void drawArrowHands()
	{
		for (int i = 0; i < CLOCK_DIGITS; i++)
		{
			for (int j = 0; j < VERTICAL_DIALS; j++)
			{
				for (int k = 0; k < HORIZONTAL_DIALS; k++)
				{
					for (int l = 0; l < DIAL_ARROWS; l++)
					{
						mDialCenterCoordinates[i][j][k][l] *= mDensity;

						drawClockHand(mDialCenterCoordinates[i][j][k][0], mDialCenterCoordinates[i][j][k][1], mCurrentAngles[i][j][k][l], mHandLengths[l]);
					}
				}
			}
		}
	}

	private void drawClockHand(float centerX, float centerY, float angleDeg, float handLength)
	{

		mCanvasArrows.drawLine(centerX, centerY, centerX + handLength * (float) Math.cos(angleDeg * Math.PI / 180.0f) * mDensity, centerY + handLength * (float) Math.sin(angleDeg * Math.PI / 180.0f) * mDensity, mPaint);
	}
}
