package ca.jvsh.dirtywindow;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

public class DirtyWindowView extends View
{
	private final Paint	mPaint				= new Paint();

	private int			mScreenWidth;
	private int			mScreenHeight;
	private int			mScreenRotation;

	// bitmaps for the apps
	private Bitmap		mSponge = null;
	private Bitmap		mWindow = null;
	private Bitmap		mBackBuffer = null;

	private Canvas		mBackBufferCanvas = null;

	private final int	mRadius				= 50;
	private final int	mDiameter			= 2 * mRadius;
	private final int	mRadiusSquare		= mRadius * mRadius;
	private final int	mDiameterSquare		= mDiameter * mDiameter;
	private int[]		backBufferpixels	= new int[(int) (mDiameterSquare)];
	private int[]		windowpixels		= new int[(int) (mDiameterSquare)];

	// rectangle on which we will draw black board bitmap
	private Rect		mDestRect;

	// media player for sound playback
	public MediaPlayer	mMediaPlayer;

	// initial sponge coordinates
	private int			mSpongeX;
	private int			mSpongeY;

	public DirtyWindowView(Context context)
	{
		this(context, null);
	}

	public DirtyWindowView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);

	}

	public DirtyWindowView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);


		// initialize our paint variable
		mPaint.setFilterBitmap(true);

		// initialize the media player
		mMediaPlayer = MediaPlayer.create(context, R.raw.window);
		mMediaPlayer.setLooping(true);
		mMediaPlayer.start();
		mMediaPlayer.setVolume(0, 0);
	}
	

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Pass the event on to the controller
		mSpongeX = (int) event.getX();
		mSpongeY = (int) event.getY();

		RadialGradient g = new RadialGradient(mSpongeX, mSpongeY, mRadius, Color.argb(255, 0, 0, 0), Color.argb(0, 0, 0, 0), TileMode.CLAMP);
		mPaint.setShader(g);
		mBackBufferCanvas.drawCircle(mSpongeX, mSpongeY, mRadius, mPaint);
		colorize(mSpongeX - mRadius, mSpongeY - mRadius);

		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
			mMediaPlayer.setVolume(1, 1);
		}
		else if (event.getAction() == MotionEvent.ACTION_UP)
		{
			mMediaPlayer.setVolume(0, 0);
		}
		invalidate();
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		
		if(mWindow == null)
		{
			init();
		}

		mPaint.setAlpha(200);
		canvas.drawBitmap(mWindow, null, mDestRect, mPaint);
		mPaint.setAlpha(255);
		canvas.drawBitmap(mSponge, mSpongeX - (mSponge.getWidth() / 2), mSpongeY - (mSponge.getHeight() / 2), mPaint);
	}

	// ///////////////////////////////////////////////////
	
	private void init()
	{
		
		Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		mScreenWidth = getWidth();
		mScreenHeight = getHeight();
		
		// we want to tile whole screen with our board bitmap.
		mDestRect = new Rect(0, 0, mScreenWidth, mScreenHeight);

		// get bitmaps
		mSponge = BitmapFactory.decodeResource(getResources(), R.drawable.wipesponge);

		mScreenRotation = display.getRotation();

		Random random = new Random();

		if (mScreenRotation == Surface.ROTATION_90 || mScreenRotation == Surface.ROTATION_270)
		{

			switch (random.nextInt(3))
			{
				case 0:
					mWindow = BitmapFactory.decodeResource(getResources(), R.drawable.window4);
					break;
				case 1:
					mWindow = BitmapFactory.decodeResource(getResources(), R.drawable.window5);
					break;
				case 2:
					mWindow = BitmapFactory.decodeResource(getResources(), R.drawable.window6);
					break;
			}
		}
		else
		{

			switch (random.nextInt(4))
			{
				case 0:
					mWindow = BitmapFactory.decodeResource(getResources(), R.drawable.window);
					break;
				case 1:
					mWindow = BitmapFactory.decodeResource(getResources(), R.drawable.window1);
					break;
				case 2:
					mWindow = BitmapFactory.decodeResource(getResources(), R.drawable.window2);
					break;
				case 3:
					mWindow = BitmapFactory.decodeResource(getResources(), R.drawable.window3);
					break;
			}
		}

		mWindow = Bitmap.createScaledBitmap(mWindow, mScreenWidth, mScreenHeight, true);
		mWindow.setHasAlpha(true);

		//init back buffer
		{
			mBackBuffer = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.ARGB_8888);
			mBackBufferCanvas = new Canvas(mBackBuffer);
			Paint p = new Paint();
			p.setStyle(Paint.Style.FILL);
			p.setColor(0x00000000);
			mBackBufferCanvas.drawRect(0, 0, mScreenWidth, mScreenHeight, p);
		}

		mSpongeX = mScreenWidth / 2;
		mSpongeY = mScreenHeight / 2;
	}

	// main method that will update
	private void colorize(int x, int y)
	{
		if (x + mDiameter > mScreenWidth)
		{
			x = mScreenWidth - mDiameter;
		}
		if (x < 0)
		{
			x = 0;
		}
		if (y < 0)
		{
			y = 0;
		}
		if (y + mDiameter > mScreenHeight)
		{
			y = mScreenHeight - mDiameter;
		}

		mBackBuffer.getPixels(backBufferpixels, 0, mDiameter, x, y, mDiameter, mDiameter);
		mWindow.getPixels(windowpixels, 0, mDiameter, x, y, mDiameter, mDiameter);

		for (int i = 0; i < windowpixels.length; i++)
		{
			if ((i / mDiameter - mRadius) * (i / mDiameter - mRadius) + (i % mDiameter - mRadius) * (i % mDiameter - mRadius) < mRadiusSquare)
			{
				windowpixels[i] = ((0xFF - (backBufferpixels[i] >>> 24)) << 24) | (windowpixels[i] & 0xFFFFFF);
			}
		}
		mWindow.setPixels(windowpixels, 0, mDiameter, x, y, mDiameter, mDiameter);
	}
}
