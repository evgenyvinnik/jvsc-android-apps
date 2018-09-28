package ca.jvsh.flute.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.jvsh.flute.R;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;

public class MoneyView extends View
{
	// screen size
	int							mScreenWidth, mScreenHeight;

	// variable responsible for painting the object on the View pane
	private final Paint			mPaint		= new Paint();

	// bitmaps that we are using in our application - coins and purse
	private List<Bitmap>		mCoinList	= new ArrayList<Bitmap>();
	private Bitmap				mPurse;

	// coordinates for purse and coins
	private int					mPurseX;
	private int					mPurseY;
	private int					mCoinX;
	private int					mCoinY;

	// accelerometer variables
	private final SensorManager	mSensorManager;
	private final Sensor		mAccelerometer;
	private SensorEventListener	mSensorEventListener;

	//make new random variable
	private final Random		mRandom		= new Random();				

	// flags and variables for coin behavior
	private int					mCoinIndex;
	private int					mCoinOpacity;

	private boolean				mSelect		= true;
	private boolean				mAppear;
	private int					mAppearance;
	private boolean				mMoveDown;
	private boolean				mDissapear;
	private int					mDissapearance;

	// variable used for setting saving/loading
	public float				mCents;
	// /////////

	public static final int		MSG_UPDATE	= 1;

	public MoneyView(Context context, int w, int h)
	{
		super(context);

		// get screen parameters
		mScreenWidth = w;
		mScreenHeight = h;
		setFocusable(true);

		// load variable value from the phone memory
		mCents = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("savings", "0.0"));

		// initialize paint variable
		mPaint.setTextSize(60);
		mPaint.setColor(Color.WHITE);

		// initialize purse bitmap
		mPurse = BitmapFactory.decodeResource(getResources(), R.drawable.pouch);
		// and coordinates
		mPurseX = mScreenWidth / 2;
		mPurseY = mScreenHeight - 150;

		// initialize list of coin bitmaps
		mCoinList.add(BitmapFactory.decodeResource(getResources(), R.drawable.cent1));
		mCoinList.add(BitmapFactory.decodeResource(getResources(), R.drawable.cent2));
		mCoinList.add(BitmapFactory.decodeResource(getResources(), R.drawable.cent3));
		mCoinList.add(BitmapFactory.decodeResource(getResources(), R.drawable.cent4));
		mCoinList.add(BitmapFactory.decodeResource(getResources(), R.drawable.cent5));

		// coin coordinates
		mCoinX = mScreenWidth / 2;
		mCoinY = 100;

		// and decide what coin to draw
		mCoinIndex = mRandom.nextInt(5);

		// add accelerometer support
		{
			mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

			mSensorEventListener = new SensorEventListener()
			{

				@Override
				public void onSensorChanged(SensorEvent event)
				{
					// y axis
					if (event.values[1] > 2)
					{
						mPurseX -= 3;
					}
					else if (event.values[1] < -2)
					{
						mPurseX += 3;
					}

					if (mPurseX < 25)
						mPurseX = 25;
					if (mPurseX > mScreenWidth - 25)
						mPurseX = mScreenWidth - 25;
				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy)
				{
				}
			};

			mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		}
	}

	/**
	 * Draw the display
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		// draw text
		String text = "Savings: $" + String.format("%.2f", mCents);
		canvas.drawText(text, (mScreenWidth - mPaint.measureText(text)) / 2, mScreenHeight / 2, mPaint);

		mPaint.setAlpha(mCoinOpacity);
		canvas.drawBitmap(mCoinList.get(mCoinIndex), mCoinX, mCoinY, mPaint);

		mPaint.setAlpha(255);
		canvas.drawBitmap(mPurse, mPurseX, mPurseY, mPaint);

	}

	
	/**
	 * Main method that controls coin behavior
	 */
	public void updateCoin()
	{
		if (mSelect)
		{
			mCoinIndex = mRandom.nextInt(5);
			mAppearance = 0;
			mAppear = true;
			mSelect = false;
			mCoinOpacity = 0;
			mCoinY = 150;
			mCoinX = mRandom.nextInt(mScreenWidth - 80) + 40;
		}
		else if (mAppear)
		{
			mCoinOpacity += 20;
			mAppearance++;
			if (mAppearance > 10)
			{
				mAppear = false;
				mMoveDown = true;
			}
		}
		else if (mMoveDown)
		{
			mCoinY += 5;

			if (mCoinY > (mScreenHeight - 150) && mCoinY < (mScreenHeight - 130))
			{
				if (mCoinX <= mPurseX + 40 && mCoinX >= mPurseX)
				{

					mCents += 0.01;
					// save result
					{
						Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
						editor.putString("savings", String.format("%.2f", mCents));
						editor.commit();
					}

					// Perform action on click: play sound
					MediaPlayer mp = null;
					switch (mRandom.nextInt(5))
					{
					case 0:
						mp = MediaPlayer.create(getContext(), R.raw.coin1);
						break;
					case 1:
						mp = MediaPlayer.create(getContext(), R.raw.coin2);
						break;
					case 2:
						mp = MediaPlayer.create(getContext(), R.raw.coin3);
						break;
					case 3:
						mp = MediaPlayer.create(getContext(), R.raw.coin4);
						break;
					case 4:
						mp = MediaPlayer.create(getContext(), R.raw.coin5);
						break;
					}

					if (mp != null)
					{
						mp.start();
						mp.setOnCompletionListener(new OnCompletionListener()
						{

							@Override
							public void onCompletion(MediaPlayer mp)
							{
								mp.release();
							}
						});
					}

					mMoveDown = false;
					mDissapear = true;
					mDissapearance = 0;
				}
			}
			else if (mCoinY > (mScreenHeight - 40))
			{
				mMoveDown = false;
				mDissapear = true;
				mDissapearance = 0;
			}
		}
		else if (mDissapear)
		{
			mCoinOpacity -= 20;
			mDissapearance++;
			if (mDissapearance > 10)
			{
				mDissapear = false;
				mSelect = true;
			}
		}

	}

	/**
	 * Handle the update message from the thread. Invoke the main display update
	 * each time one is received.
	 */
	private Handler	handler	= new Handler()
							{

								@Override
								public void handleMessage(Message msg)
								{
									switch (msg.arg1)
									{
									case MSG_UPDATE:
										//redraw the whole screen
										invalidate();
										break;
									}
								}
							};

	/**
	 * Get a handle to this classes message handler
	 */
	public Handler getHandler()
	{
		return handler;
	}
}
