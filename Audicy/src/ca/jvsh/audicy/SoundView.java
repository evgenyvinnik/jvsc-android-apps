package ca.jvsh.audicy;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Typeface;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class SoundView extends View implements MultiTouchObjectCanvas<Object>
{
	private MultiTouchController<Object>	multiTouchController;
	private PointInfo						mCurrTouchPoint;

	private static final int[]				TOUCH_COLORS			= { Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLUE, Color.WHITE, Color.GRAY, Color.LTGRAY, Color.DKGRAY };

	private final Paint						mPaint	= new Paint();

	private int[]							mTouchPointColors		= new int[MultiTouchController.MAX_TOUCH_POINTS];

	private SineWaver[] mSineWaver = new SineWaver[MultiTouchController.MAX_TOUCH_POINTS];
	
	float mScreenWidth;
	float mScreenHeight;
	
	private Canvas							myCanvas;
	private Bitmap							backbuffer;
	private float							radius;

	
	public SoundView(Context context)
	{
		this(context, null);
	}

	public SoundView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
		
	}

	private void init()
	{
		this.radius = 40f;
		backbuffer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		myCanvas = new Canvas(backbuffer);
		Paint p = new Paint();
		p.setStyle(Paint.Style.FILL);
		p.setColor(Color.TRANSPARENT);
		myCanvas.drawRect(0, 0, getWidth(), getHeight(), p);

	}

	
	
	public SoundView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		
		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		// get information about window size
		mScreenWidth = display.getWidth();
		mScreenHeight = display.getHeight();

		multiTouchController = new MultiTouchController<Object>(this);
		mCurrTouchPoint = new PointInfo();

		mPaint.setTextSize(60);
		mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mPaint.setAntiAlias(true);

		//mPointLabelBg.set(mPointLabelPaint);
		//mPointLabelBg.setColor(Color.BLACK);
		//mPointLabelBg.setAlpha(180);
		//mPointLabelBg.setStyle(Style.STROKE);
		//mPointLabelBg.setStrokeWidth(15);

		for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
		{
			mTouchPointColors[i] = i < TOUCH_COLORS.length ? TOUCH_COLORS[i] : (int) (Math.random() * 0xffffff) + 0xff000000;

			//mSineWaver[i] = new SineWaver();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Pass the event on to the controller
		return multiTouchController.onTouchEvent(event);
	}

	public Object getDraggableObjectAtPoint(PointInfo pt)
	{
		// IMPORTANT: to start a multitouch drag operation, this routine must
		// return non-null
		return this;
	}

	public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut)
	{
		// We aren't dragging any objects, so this doesn't do anything in this
		// app
	}

	public void selectObject(Object obj, PointInfo touchPoint)
	{
		// We aren't dragging any objects in this particular app, but this is
		// called when the point goes up (obj == null) or down (obj != null),
		// save the touch point info
		touchPointChanged(touchPoint);
	}

	public boolean setPositionAndScale(Object obj, PositionAndScale newObjPosAndScale, PointInfo touchPoint)
	{
		// Called during a drag or stretch operation, update the touch point
		// info
		touchPointChanged(touchPoint);
		return true;
	}

	/**
	 * Called when the touch point info changes, causes a redraw.
	 * 
	 * @param touchPoint
	 */
	private void touchPointChanged(PointInfo touchPoint)
	{
		// Take a snapshot of touch point info, the touch point is volatile
		mCurrTouchPoint.set(touchPoint);
		invalidate();
	}

	/*
	 * public void addPoint() { int numPoints =
	 * mCurrTouchPoint.getNumTouchPoints(); float[] xs =
	 * mCurrTouchPoint.getXs(); float[] ys = mCurrTouchPoint.getYs(); float[]
	 * pressures = mCurrTouchPoint.getPressures(); int[] pointerIds =
	 * mCurrTouchPoint.getPointerIds();
	 * 
	 * }
	 */

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		
		if (backbuffer == null)
		{
			init();
		}
		canvas.drawBitmap(backbuffer, 0, 0, mPaint);

		
		int numPoints = mCurrTouchPoint.getNumTouchPoints();
		int[] pointerIds = mCurrTouchPoint.getPointerIds();

		if (mCurrTouchPoint.isDown())
		{
			addPoint();

			float[] xs = mCurrTouchPoint.getXs();
			float[] ys = mCurrTouchPoint.getYs();
			//float[] pressures = mCurrTouchPoint.getPressures();
			float x = mCurrTouchPoint.getX(), y = mCurrTouchPoint.getY();
			float wd = getWidth(), ht = getHeight();

			Log.i("player", " ");
			Log.i("numpoints", "numpoint "+ numPoints);
			// Log touch point indices
			if (/*MultiTouchController.DEBUG*/true)
			{
				StringBuilder buf = new StringBuilder();
				for (int i = 0; i < numPoints; i++)
					buf.append(" " + i + "->" + pointerIds[i]);
				Log.i("MultiTouchVisualizer", buf.toString());
			}
			
			int pointerId = 0;
			for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
			{
				if(i == pointerIds[pointerId] && pointerId < numPoints)
				{
					Log.i("player", "start player "+ i);
					float freq = 440.0f + 440.0f * x/wd;
					float amp =  0.5f + 0.5f*y/ht;
					//Log.i("SoundView", "Freq " + freq + " amp " + amp);
					if(mSineWaver[i] == null)
					{
						mSineWaver[i] = new SineWaver();
						mSineWaver[i].freqAmp(freq, amp);
						mSineWaver[i].start();
					}
					else
					{
						mSineWaver[i].freqAmp(freq, amp);
					}
					pointerId++;
					//if(pointerId >= numPoints)
					//{
					//	break;
					//}
				}
				else
				{
					Log.i("player", "stop player "+ i);
					if(mSineWaver[i] != null)
					{
						mSineWaver[i].requestStop();
						mSineWaver[i] = null;
					}
				}
			}
			
			//if(mSineWaver != null)
			//{
				//float freq = 440.0f + 440.0f * x/wd;
				//float amp =  0.5f + 0.5f*y/ht;
				//Log.i("SoundView", "Freq " + freq + " amp " + amp);
				//mSineWaver.freqAmp(freq, amp);

				//if(mSineWaver.mStart.compareAndSet(false, true))
				//{
					//mSineWaver.mStart.set(true);
					//mSineWaver.start();
				//}
			//}
			
			//float x = mCurrTouchPoint.getX(), y = mCurrTouchPoint.getY();
			//float wd = getWidth(), ht = getHeight();

			for (int idx = 0; idx < numPoints; idx++)
			{
				// Show touch circles
				mPaint.setColor(mTouchPointColors[idx]);
				canvas.drawCircle(xs[idx], ys[idx], 50, mPaint);

				// Label touch points on top of everything else
				String label = (idx + 1) + (idx == pointerIds[idx] ? "" : "(id:" + (pointerIds[idx] + 1) + ")");

				canvas.drawText(label, xs[idx] + 50, ys[idx] - 50, mPaint);
			}
		}
		else if(numPoints == 1)
		{
			Log.i("player", "stop all");
			
			for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
			{
				Log.i("player", "stop all player "+ i);
				if(mSineWaver[i] != null)
				{
					mSineWaver[i].requestStop();
					mSineWaver[i] = null;
				}
			}
		}

	}
	
	public void addPoint()
	{
		int numPoints = mCurrTouchPoint.getNumTouchPoints();
		float[] xs = mCurrTouchPoint.getXs();
		float[] ys = mCurrTouchPoint.getYs();

		for (int i = 0; i < numPoints; i++)
		{
			RadialGradient g = new RadialGradient(xs[i], ys[i], radius, Color.argb(10, 0, 0, 0), Color.TRANSPARENT, TileMode.CLAMP);
			Paint gp = new Paint();
			gp.setShader(g);
			myCanvas.drawCircle(xs[i], ys[i], radius, gp);
			colorize(xs[i] - radius, ys[i] - radius, radius * 2);
		}
		invalidate();
	}

	private void colorize(float x, float y, float d)
	{
		if (x + d > myCanvas.getWidth())
		{
			x = myCanvas.getWidth() - d;
		}
		if (x < 0)
		{
			x = 0;
		}
		if (y < 0)
		{
			y = 0;
		}
		if (y + d > myCanvas.getHeight())
		{
			y = myCanvas.getHeight() - d;
		}

		int[] pixels = new int[(int) (d * d)];
		backbuffer.getPixels(pixels, 0, (int) d, (int) x, (int) y, (int) d, (int) d);
		for (int i = 0; i < pixels.length; i++)
		{
			int r = 0, g = 0, b = 0, tmp = 0;
			int alpha = pixels[i] >>> 24;
			if (alpha <= 255 && alpha >= 240)
			{
				tmp = 255 - alpha;
				r = 255 - tmp;
				g = tmp * 12;
			}
			else if (alpha <= 239 && alpha >= 200)
			{
				tmp = 234 - alpha;
				r = 255 - (tmp * 8);
				g = 255;
			}
			else if (alpha <= 199 && alpha >= 150)
			{
				tmp = 199 - alpha;
				g = 255;
				b = tmp * 5;
			}
			else if (alpha <= 149 && alpha >= 100)
			{
				tmp = 149 - alpha;
				g = 255 - (tmp * 5);
				b = 255;
			}
			else
				b = 255;
			pixels[i] = Color.argb(alpha, r, g, b);
		}
		backbuffer.setPixels(pixels, 0, (int) d, (int) x, (int) y, (int) d, (int) d);
	}
}
