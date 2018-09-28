package ca.jvsh.guitardemo;

import java.util.ArrayList;
import java.util.Collections;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import ca.jvsh.guitardemo.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class GuitarView extends View implements MultiTouchObjectCanvas<Object>
{
	private final MultiTouchController<Object>	multiTouchController;
	private final PointInfo						mCurrTouchPoint;

	private static final int[]					TOUCH_COLORS					= { Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.YELLOW,
																				Color.BLUE, Color.WHITE, Color.GRAY, Color.LTGRAY, Color.DKGRAY };

	private static final int					STRINGS							= 6;
	private static final int					FRETS							= 12;

	private static final float[]				STRING_THICKNESS				= { 4.6f, 3.6f, 2.6f, 1.7f, 1.3f, 1.0f };
	private static final float[]				STRING_NOTES					= { 82.407f, 110.0f, 146.83f, 196.00f, 246.94f, 329.63f };

	private final PointF[]						mStringsBegin					= new PointF[STRINGS];
	private final PointF[]						mStringsEnd						= new PointF[STRINGS];

	private final Paint							mFretPaint						= new Paint();
	private final Paint							mPlayFretPaint					= new Paint();
	private final Paint							mStringPaint					= new Paint();
	//private final Paint							mStringPaintGlow  				= new Paint();
	private final Paint							mPaint							= new Paint();

	private final int[]							mTouchPointColors				= new int[MultiTouchController.MAX_TOUCH_POINTS];

	private final PointF[]						mCurPoint						= new PointF[MultiTouchController.MAX_TOUCH_POINTS];
	private final PointF[]						mPrevPoint						= new PointF[MultiTouchController.MAX_TOUCH_POINTS];

	private final boolean						mStringPushedDown[][]			= new boolean[STRINGS][MultiTouchController.MAX_TOUCH_POINTS];

	private final PointF						mStringsPushedDownLeftX[][]		= new PointF[STRINGS][MultiTouchController.MAX_TOUCH_POINTS];
	private final PointF						mStringsPushedDownRightX[][]	= new PointF[STRINGS][MultiTouchController.MAX_TOUCH_POINTS];

	private final boolean						mStringPulledFromDown[][]		= new boolean[STRINGS][MultiTouchController.MAX_TOUCH_POINTS];
	private final boolean						mStringPulledFromUp[][]			= new boolean[STRINGS][MultiTouchController.MAX_TOUCH_POINTS];

	private final boolean						mStringPlay[]					= new boolean[STRINGS];
	private final float							mStringPlayFingerX[]			= new float[STRINGS];

	private final PointF						mStringsPulledPoint[][]			= new PointF[STRINGS][MultiTouchController.MAX_TOUCH_POINTS];

	private final float[]						mFretX							= new float[FRETS];
	public int									playFret						= 0;
	float										prevY;
	private static final float					mFingerThickness				= 25;

	int											mHeight;
	int											mWidth;

	float										mScreenThird;
	private static final float					mStringDistance					= 60;

	float										freq;

	@SuppressWarnings("unchecked")
	private ArrayList<PointF>[]					coordinateList					= new ArrayList[STRINGS];
	private Karpluser[]							mKarplus						= new Karpluser[STRINGS];

	private static final PointCompare			PointComparatorX				= new PointCompare();

	private PointF								prevPoint						= new PointF();
	private PointF								currentPoint					= new PointF();

	private Bitmap								mFretBoard;
	// rectangle on which we will draw fret board bitmap
	private Rect								mDestRect;

	private InputDevice							mLastInputDevice;

	public GuitarView(Context context)
	{
		this(context, null);
	}

	public GuitarView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);

	}

	public GuitarView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		setFocusable(true);
		setFocusableInTouchMode(true);

		multiTouchController = new MultiTouchController<Object>(this);
		mCurrTouchPoint = new PointInfo();

		mFretPaint.setStrokeWidth(4);
		mFretPaint.setColor(Color.WHITE);

		mPlayFretPaint.setColor(Color.argb(44, 255, 255, 255));

		mPaint.setTextSize(60);
		mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mPaint.setAntiAlias(true);

		mStringPaint.setStyle(Style.STROKE);
		mStringPaint.setAntiAlias(true);
		mStringPaint.setDither(true);

		mStringPaint.setStrokeJoin(Paint.Join.ROUND);
		mStringPaint.setStrokeCap(Paint.Cap.ROUND);

		//mStringPaintGlow.set(mStringPaint);
		//mStringPaintGlow.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL)); 

		for (int string = 0; string < STRINGS; string++)
		{
			mStringsBegin[string] = new PointF(0, 0);
			mStringsEnd[string] = new PointF(0, 0);
			coordinateList[string] = new ArrayList<PointF>();

			mKarplus[string] = new Karpluser();

			mKarplus[string].setFrequency(STRING_NOTES[string]);
			mKarplus[string].setK(mKarplus[string].M + 1);
			mKarplus[string].start();
		}

		for (int finger = 0; finger < MultiTouchController.MAX_TOUCH_POINTS; finger++)
		{
			mTouchPointColors[finger] = finger < TOUCH_COLORS.length ? TOUCH_COLORS[finger] : (int) (Math.random() * 0xffffff) + 0xff000000;
			mCurPoint[finger] = new PointF(0, 0);
			mPrevPoint[finger] = new PointF(0, 0);
			for (int string = 0; string < STRINGS; string++)
			{
				mStringsPulledPoint[string][finger] = new PointF(0, 0);
				mStringsPushedDownLeftX[string][finger] = new PointF(0, 0);
				mStringsPushedDownRightX[string][finger] = new PointF(0, 0);
			}
		}

		mFretBoard = BitmapFactory.decodeResource(getResources(), R.drawable.fretboard);
	}

	public void setGuitarType(boolean electric)
	{
		for (int string = 0; string < STRINGS; string++)
		{
			mKarplus[string].setGuitarType(electric);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Pass the event on to the controller
		return multiTouchController.onTouchEvent(event);
	}

	@Override
	public Object getDraggableObjectAtPoint(PointInfo pt)
	{
		// IMPORTANT: to start a multitouch drag operation, this routine must
		// return non-null
		return this;
	}

	@Override
	public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut)
	{
		// We aren't dragging any objects, so this doesn't do anything in this
		// app
	}

	@Override
	public void selectObject(Object obj, PointInfo touchPoint)
	{
		// We aren't dragging any objects in this particular app, but this is
		// called when the point goes up (obj == null) or down (obj != null),
		// save the touch point info
		touchPointChanged(touchPoint);
	}

	@Override
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

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = w;
		mHeight = h;

		mScreenThird = mHeight / 2.0f - 3.0f * mStringDistance;

		mDestRect = new Rect(0, (int) (mScreenThird - mStringDistance / 2.0f), mWidth, (int) (mScreenThird + 5.5f * mStringDistance));

		// calculate frets X
		{
			mFretX[0] = mWidth / 17.817f;

			for (int fret = 1; fret < FRETS; fret++)
			{
				mFretX[fret] = mFretX[fret - 1] + (mWidth - mFretX[fret - 1]) / 17.817f;
			}

			for (int fret = 0; fret < FRETS; fret++)
			{
				mFretX[fret] = mWidth * mFretX[fret] / mFretX[FRETS - 1];
			}
		}

		// calculate normal (without tension) string Y
		for (int string = 0; string < STRINGS; string++)
		{
			mStringsBegin[string].set(0, mScreenThird + string * mStringDistance);
			mStringsEnd[string].set(mWidth, mScreenThird + string * mStringDistance);
		}

	}

	private void drawFrets(Canvas canvas)
	{

		canvas.drawBitmap(mFretBoard, null, mDestRect, mFretPaint);

		//draw frets
		for (int fret = 0; fret < FRETS; fret++)
		{
			canvas.drawLine(mFretX[fret], mScreenThird - mStringDistance / 2.0f, mFretX[fret], mScreenThird + 5.5f * mStringDistance, mFretPaint);
			if (fret == playFret)
			{
				if (playFret == 0)
					canvas.drawRect(0, mScreenThird - mStringDistance / 2.0f, mFretX[fret], mScreenThird + 5.5f * mStringDistance, mPlayFretPaint);
				else
					canvas.drawRect(mFretX[fret - 1], mScreenThird - mStringDistance / 2.0f, mFretX[fret], mScreenThird + 5.5f * mStringDistance,
							mPlayFretPaint);
			}
		}

		//draw fret markers
		canvas.drawCircle((mFretX[1] + mFretX[2]) / 2.0f, mScreenThird + 2.5f * mStringDistance, mFingerThickness, mFretPaint);
		canvas.drawCircle((mFretX[3] + mFretX[4]) / 2.0f, mScreenThird + 2.5f * mStringDistance, mFingerThickness, mFretPaint);
		canvas.drawCircle((mFretX[5] + mFretX[6]) / 2.0f, mScreenThird + 2.5f * mStringDistance, mFingerThickness, mFretPaint);
		canvas.drawCircle((mFretX[7] + mFretX[8]) / 2.0f, mScreenThird + 2.5f * mStringDistance, mFingerThickness, mFretPaint);

		canvas.drawCircle((mFretX[10] + mFretX[11]) / 2.0f, mScreenThird + 1.0f * mStringDistance, mFingerThickness, mFretPaint);
		canvas.drawCircle((mFretX[10] + mFretX[11]) / 2.0f, mScreenThird + 4.0f * mStringDistance, mFingerThickness, mFretPaint);

	}

	private void drawUnstrummedStrings(Canvas canvas)
	{
		for (int string = 0; string < STRINGS; string++)
		{
			//mStringPaintGlow.setStrokeWidth(2 * STRING_THICKNESS[string]);
			//mStringPaintGlow.setColor(mTouchPointColors[string]);
			mStringPaint.setStrokeWidth(2 * STRING_THICKNESS[string]);
			mStringPaint.setColor(mTouchPointColors[string]);
			mStringPaint.setAlpha(175);
			//mStringPaintGlow.setAlpha(175);

			//canvas.drawLine(0, mScreenThird + string * mStringDistance, mWidth, mScreenThird + string * mStringDistance, mStringPaintGlow);
			//setLayerType(View.LAYER_TYPE_HARDWARE, mStringPaint);
			canvas.drawLine(0, mScreenThird + string * mStringDistance, mWidth, mScreenThird + string * mStringDistance, mStringPaint);
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		// draw strings
		drawFrets(canvas);

		final int numPoints = mCurrTouchPoint.getNumTouchPoints();

		if (mCurrTouchPoint.isDown())
		{

			final int[] pointerIds = mCurrTouchPoint.getPointerIds();

			final float[] xs = mCurrTouchPoint.getXs();
			final float[] ys = mCurrTouchPoint.getYs();

			int pointerId = 0;
			// checking what points are still pushing the fret board
			// multitouch controller has pointer ids
			// but not always pointer id == finger (because sometimes we can
			// lift some of the fingers)
			for (int finger = 0; finger < MultiTouchController.MAX_TOUCH_POINTS; finger++)
			{
				if (finger == pointerIds[pointerId] && pointerId < numPoints)
				{
					mCurPoint[finger].set(xs[pointerId], ys[pointerId]);

					pointerId++;
				}
				else
				{
					// if the finger is not pushing the string any more - set
					// its coordinates to zero
					mCurPoint[finger].set(0, 0);
				}
			}

			// check what fingers pushing/pulling on what strings
			for (int string = 0; string < STRINGS; string++)
			{
				//clear coordinates
				coordinateList[string].clear();
				//add coordinates of string beginning and end
				coordinateList[string].add(mStringsBegin[string]);
				coordinateList[string].add(mStringsEnd[string]);

				for (int finger = 0; finger < MultiTouchController.MAX_TOUCH_POINTS; finger++)
				{
					// first checks

					// finger started to push string to the fretboard
					// condition - previous finger was zero.
					// finger thickness cover the string
					if (mCurPoint[finger].y + mFingerThickness > mStringsBegin[string].y && mCurPoint[finger].y - mFingerThickness < mStringsBegin[string].y
							&& mPrevPoint[finger].y == 0 && mStringPushedDown[string][finger] == false)
					{
						mStringPushedDown[string][finger] = true;
						mKarplus[string].setK(mKarplus[string].M + 1);
					}
					// detect whether finger start pulling the string from up
					// condition - previous finger position was less than string
					// y coordinate,
					// but present position is higher
					else if (mCurPoint[finger].y + mFingerThickness > mStringsBegin[string].y
							&& mPrevPoint[finger].y + mFingerThickness <= mStringsBegin[string].y && mPrevPoint[finger].y != 0
							&& mStringPulledFromUp[string][finger] == false)
					{
						mStringPulledFromUp[string][finger] = true;
						mKarplus[string].setK(mKarplus[string].M + 1);
					}
					// detect whether finger start pulling the string from down
					// condition - previous finger position was bigger than
					// string y coordinate,
					// but present position is lower
					else if (mCurPoint[finger].y - mFingerThickness < mStringsBegin[string].y
							&& mPrevPoint[finger].y - mFingerThickness >= mStringsBegin[string].y && mPrevPoint[finger].y != 0
							&& mStringPulledFromDown[string][finger] == false)
					{
						mStringPulledFromDown[string][finger] = true;
						mKarplus[string].setK(mKarplus[string].M + 1);
					}

					// now detecting current situation

					if (mStringPushedDown[string][finger] == true)
					{
						// string was pushed down on previous iteration

						// detect whether we still pushing the string
						if (mCurPoint[finger].y + mFingerThickness > mStringsBegin[string].y
								&& mCurPoint[finger].y - mFingerThickness < mStringsBegin[string].y)
						{
							// detect the closest fret
							for (int fret = 0; fret < FRETS; fret++)
							{
								if (mFretX[fret] > mCurPoint[finger].x)
								{
									mStringsPushedDownRightX[string][finger].set(mFretX[fret], mStringsBegin[string].y);

									if (fret > 0)
									{
										mStringsPushedDownLeftX[string][finger].set(mFretX[fret - 1], mStringsBegin[string].y);
									}
									else
									{
										mStringsPushedDownLeftX[string][finger].set(0, mStringsBegin[string].y);
									}

									break;
								}
							}
							coordinateList[string].add(mStringsPushedDownLeftX[string][finger]);
							coordinateList[string].add(mStringsPushedDownRightX[string][finger]);

						}
						else
						{
							mStringPushedDown[string][finger] = false;
						}
					}
					// detect if we are still pulling the string from up
					else if (mStringPulledFromUp[string][finger] == true)
					{
						// detect if we still pulling the string

						// pulled to much == strummed the string -> play the
						// sound (potentially)
						// and yes, we are not pulling the string any more.
						if (mCurPoint[finger].y + mFingerThickness > mStringsBegin[string].y + 0.8 * mStringDistance)
						{
							mStringPlay[string] = true;
							mStringPlayFingerX[string] = mCurPoint[finger].x;
							mStringPulledFromUp[string][finger] = false;
						}
						// we started pulling from up and we are moved finger up
						// again
						// so no sound to play. However, we stop pulling the
						// string
						else if (mCurPoint[finger].y + mFingerThickness < mStringsBegin[string].y)
						{
							mStringPulledFromUp[string][finger] = false;
						}
						// ok, we are still pulling this string
						else
						{
							mStringsPulledPoint[string][finger].set(mCurPoint[finger].x, mCurPoint[finger].y + mFingerThickness);
							coordinateList[string].add(mStringsPulledPoint[string][finger]);
						}
					}
					// detect if we are still pulling the string from down
					else if (mStringPulledFromDown[string][finger] == true)
					{
						// detect if we still pulling the string

						// pulled to much == strummed the string -> play the
						// sound (potentially)
						// and yes, we are not pulling the string any more.
						if (mCurPoint[finger].y - mFingerThickness < mStringsBegin[string].y - 0.6 * mStringDistance)
						{
							mStringPlay[string] = true;
							mStringPlayFingerX[string] = mCurPoint[finger].x;
							mStringPulledFromDown[string][finger] = false;
						}
						// we started pulling from down and we are moved finger
						// down again
						// so no sound to play. However, we stop pulling the
						// string
						else if (mCurPoint[finger].y - mFingerThickness > mStringsBegin[string].y)
						{
							mStringPulledFromDown[string][finger] = false;
						}
						// ok, we are still pulling this string
						else
						{
							mStringsPulledPoint[string][finger].set(mCurPoint[finger].x, mCurPoint[finger].y - mFingerThickness);
							coordinateList[string].add(mStringsPulledPoint[string][finger]);
						}
					}

				}

				Collections.sort(coordinateList[string], PointComparatorX);

				//mStringPaintGlow.setStrokeWidth(2 * STRING_THICKNESS[string]);
				//mStringPaintGlow.setColor(mTouchPointColors[string]);
				mStringPaint.setStrokeWidth(2 * STRING_THICKNESS[string]);
				mStringPaint.setColor(mTouchPointColors[string]);

				int points = coordinateList[string].size();

				prevPoint.set(coordinateList[string].get(0));

				if (points >= 2)
				{
					for (int point = 1; point < points; point++)
					{
						currentPoint.set(coordinateList[string].get(point));
						if (prevPoint.y != currentPoint.y)
						{
							mStringPaint.setAlpha(255);
							//mStringPaintGlow.setAlpha(255);
						}
						else
						{
							mStringPaint.setAlpha(175);
							//mStringPaintGlow.setAlpha(175);
						}

						//canvas.drawLine(prevPoint.x, prevPoint.y, currentPoint.x, currentPoint.y, mStringPaintGlow);
						canvas.drawLine(prevPoint.x, prevPoint.y, currentPoint.x, currentPoint.y, mStringPaint);

						prevPoint.set(currentPoint);

					}
				}
				// detect whether we have to play the sound
				if (mStringPlay[string] == true)
				{
					prevPoint.set(coordinateList[string].get(points - 2));
					if (prevPoint.y == mStringsBegin[string].y)
					{
						if (mStringPlayFingerX[string] > prevPoint.x)
						{

							//detect the fret
							int playFretTouch = 0;

							// detect the closest fret
							for (int fret = 0; fret < FRETS; fret++)
							{
								if (mFretX[fret] == prevPoint.x)
								{
									playFretTouch = fret + 1;
									break;
								}

							}

							setFrequency(string, playFretTouch);

							mStringPlay[string] = false;
						}
					}
				}

			}

			// draw the points
			for (int idx = 0; idx < numPoints; idx++)
			{
				// Show touch circles
				mPaint.setColor(mTouchPointColors[pointerIds[idx] + 1]);
				canvas.drawCircle(xs[idx], ys[idx], mFingerThickness, mPaint);

				// Label touch points on top of everything else
				canvas.drawText(" " + (pointerIds[idx] + 1), xs[idx] + mFingerThickness, ys[idx] - mFingerThickness, mPaint);
			}

			for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
			{
				mPrevPoint[i].set(mCurPoint[i]);
			}
		}
		else if (numPoints == 1)
		{
			for (int finger = 0; finger < MultiTouchController.MAX_TOUCH_POINTS; finger++)
			{
				mCurPoint[finger].set(0, 0);
				mPrevPoint[finger].set(0, 0);
			}

			for (int string = 0; string < STRINGS; string++)
			{
				mStringPlay[string] = false;
				for (int finger = 0; finger < MultiTouchController.MAX_TOUCH_POINTS; finger++)
				{
					mStringPushedDown[string][finger] = false;
					mStringPulledFromDown[string][finger] = false;
					mStringPulledFromUp[string][finger] = false;
				}
			}
			// draw unstrummed strings
			drawUnstrummedStrings(canvas);
		}
		else
		{
			// draw unstrummed strings
			drawUnstrummedStrings(canvas);
		}

	}

	public void setFrequency(int string, int playFret)
	{
		freq = STRING_NOTES[string] * (float) Math.pow(2.0, playFret / 12.0);

		mKarplus[string].setFrequency(freq);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// Handle DPad keys and fire button on initial down but not on auto-repeat.
		boolean handled = false;
		if (event.getRepeatCount() == 0)
		{
			switch (keyCode)
			{
				case KeyEvent.KEYCODE_BUTTON_A:
					setFrequency(1, playFret);
					handled = true;
					break;
				case KeyEvent.KEYCODE_BUTTON_B:
					setFrequency(2, playFret);
					handled = true;
					break;
				case KeyEvent.KEYCODE_BUTTON_X:
					setFrequency(3, playFret);
					handled = true;
					break;
				case KeyEvent.KEYCODE_BUTTON_Y:
					setFrequency(4, playFret);
					handled = true;
					break;
				case KeyEvent.KEYCODE_BUTTON_L1:
					setFrequency(5, playFret);
					handled = true;
					break;
			}
		}
		if (handled)
		{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{

		// Handle keys going up.
		/*boolean handled = false;
		switch (keyCode) {
		    case KeyEvent.KEYCODE_DPAD_LEFT:
		        mShip.setHeadingX(0);
		        mDPadState &= ~DPAD_STATE_LEFT;
		        handled = true;
		        break;
		    case KeyEvent.KEYCODE_DPAD_RIGHT:
		        mShip.setHeadingX(0);
		        mDPadState &= ~DPAD_STATE_RIGHT;
		        handled = true;
		        break;
		    case KeyEvent.KEYCODE_DPAD_UP:
		        mShip.setHeadingY(0);
		        mDPadState &= ~DPAD_STATE_UP;
		        handled = true;
		        break;
		    case KeyEvent.KEYCODE_DPAD_DOWN:
		        mShip.setHeadingY(0);
		        mDPadState &= ~DPAD_STATE_DOWN;
		        handled = true;
		        break;
		    default:
		        if (isFireKey(keyCode)) {
		            handled = true;
		        }
		        break;
		}
		if (handled) {
		    step(event.getEventTime());
		    return true;
		}*/
		return true;
		//return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event)
	{
		// ensureInitialized();

		// Check that the event came from a joystick since a generic motion event
		// could be almost anything.
		if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
				&& event.getAction() == MotionEvent.ACTION_MOVE)
		{
			// Cache the most recently obtained device information.
			// The device information may change over time but it can be
			// somewhat expensive to query.
			if (mLastInputDevice == null || mLastInputDevice.getId() != event.getDeviceId())
			{
				mLastInputDevice = event.getDevice();
				// It's possible for the device id to be invalid.
				// In that case, getDevice() will return null.
				if (mLastInputDevice == null)
				{
					return false;
				}
			}

			// Ignore joystick while the DPad is pressed to avoid conflicting motions.
			//  if (mDPadState != 0)
			//{
			//    return true;
			// }

			// Process all historical movement samples in the batch.
			final int historySize = event.getHistorySize();
			for (int i = 0; i < historySize; i++)
			{
				processJoystickInput(event, i);
			}

			// Process the current movement sample in the batch.
			processJoystickInput(event, -1);
			return true;
		}
		return super.onGenericMotionEvent(event);
	}

	private void processJoystickInput(MotionEvent event, int historyPos)
	{
		// Get joystick position.
		// Many game pads with two joysticks report the position of the second joystick
		// using the Z and RZ axes so we also handle those.
		// In a real game, we would allow the user to configure the axes manually.

		float y = getCenteredAxis(event, mLastInputDevice, MotionEvent.AXIS_HAT_Y, historyPos);
		if (y == 1.0 && prevY != y && playFret < FRETS - 1)
		{
			playFret++;
			this.invalidate();
		}
		else if (y == -1.0 && prevY != y && playFret > 0)
		{
			playFret--;
			this.invalidate();

		}
		prevY = y;
		// Set the ship heading.
	}

	private static float getCenteredAxis(MotionEvent event, InputDevice device,
			int axis, int historyPos)
	{
		final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
		if (range != null)
		{
			final float flat = range.getFlat();
			final float value = historyPos < 0 ? event.getAxisValue(axis)
					: event.getHistoricalAxisValue(axis, historyPos);

			// Ignore axis values that are within the 'flat' region of the joystick axis center.
			// A joystick at rest does not always report an absolute position of (0,0).
			if (Math.abs(value) > flat)
			{
				return value;
			}
		}
		return 0;
	}
}
