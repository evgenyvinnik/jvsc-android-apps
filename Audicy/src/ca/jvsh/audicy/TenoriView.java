package ca.jvsh.audicy;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TenoriView extends View
{
	private final Paint			mPaint			= new Paint();
	private final Paint			mFillPaint		= new Paint();
	private final Paint			mWavePaint		= new Paint();

	private final Random		random			= new Random();

	int							mHeight;
	int							mWidth;

	public static final int		MSG_UPDATE		= 1;
	public static final int		MSG_PLAY		= 2;

	private boolean				mVisible;

	public static final int		flapLength		= 16;
	int							padding			= 6;
	
	public boolean				color			= true;
	public boolean				rounded			= true;
	public boolean				smoke			= true;
	public boolean				palette			= false;

	int							pitchOffset		= 0;

	int							flaps;
	float						gridwidth;

	ArrayList<TenoriElement>	elements		= new ArrayList<TenoriElement>();
	boolean						padswitches[]	= new boolean[flapLength * flapLength];

	ArrayList<WaveElement>		waves			= new ArrayList<WaveElement>();
	int							colors[]		= new int[7];
	RadialGradient				gradient;
	int							r, g, b;

	int							row				= 0;
	int							rowplay			= 0;

	int							ttl				= 15;
	TenoriElement				e;														//partiripple
	float						curX;
	float						curY;
	float						prevX;
	float						prevY;
	int							prevIndex		= -1;
	boolean						flagRemove		= false;
	int							length;

	ChimeThread					mChimeThread;
	private final ReentrantLock	chimeLock		= new ReentrantLock();

	public TenoriView(Context context)
	{
		this(context, null);
	}

	public TenoriView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);

	}

	public TenoriView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		mPaint.setColor(Color.argb(255, 90, 90, 90));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(2);

		mWavePaint.setDither(true);

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = w;
		mHeight = h;
		initTenoriElements();
	}

	void initTenoriElements()
	{
		gridwidth = (mHeight - (flapLength + 1) * padding) / flapLength;

		float tx = 0;
		float ty = 0;
		int z = 0;
		for (int i = 0; i < flapLength; i++)
		{
			tx = (mWidth - (gridwidth + padding) * flapLength) / 2.0f;
			;
			ty += padding;

			for (int j = 0; j < flapLength; j++)
			{
				elements.add(new TenoriElement(tx, ty, gridwidth, i, j, z));
				tx += gridwidth + padding;
				z++;
			}
			ty += gridwidth;
		}

		clearSwitches();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		curX = (int) event.getX();
		curY = (int) event.getY();

		for (TenoriElement tenori : elements)
		{
			if (tenori.gridRect.contains(curX, curY))
			{
				if ((Math.abs(curX - prevX) < 4 || Math.abs(curY - prevY) < 4) && tenori.index == prevIndex)
					break;
				padswitches[tenori.index] = !padswitches[tenori.index];
				prevX = curX;
				prevY = curY;
				prevIndex = tenori.index;
				break;
			}
		}
		return true;
	}
	
	public void clearSwitches()
	{
		for (int i = 0; i < flapLength * flapLength; i++)
		{
			padswitches[i] = false;
		}
	}

	@Override
	public void onVisibilityChanged(View changedView, int visibility)
	{
		super.onVisibilityChanged(changedView, visibility);

		if (View.VISIBLE == visibility)
		{
			mVisible = true;
		}
		else
		{
			mVisible = false;
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (mVisible)
		{
			canvas.drawColor(0xFF000000);

			paintGrids(canvas);

			paintRow(canvas);

			drawSmoke(canvas);
		}
	}

	private void paintGrids(Canvas canvas)
	{
		for (TenoriElement tenori : elements)
		{
			paintOneGrid(canvas, tenori);
		}

	}

	private void paintRow(Canvas canvas)
	{
		row++;
		row %= 16;
		int length = elements.size();
		mFillPaint.setColor(Color.argb(255, 255, 255, 255));

		for (int i = row; i < length; i += flapLength)
		{
			e = elements.get(i);
			if (padswitches[e.index])
			{
				if (rounded)
				{
					canvas.drawRoundRect(e.gridRect, 5, 5, mFillPaint);
				}
				else
				{
					canvas.drawRect(e.gridRect, mFillPaint);
				}

				if (smoke)
				{
					waves.add(new WaveElement(e.index, 0));
				}
			}

		}
	}

	private void paintOneGrid(Canvas canvas, TenoriElement tenori)
	{

		if (padswitches[tenori.index])
			mFillPaint.setColor(Color.argb(204, 120, 120, 120));
		else
			mFillPaint.setColor(Color.argb(128, 40, 40, 40));

		if (rounded)
		{
			canvas.drawRoundRect(tenori.gridRect, 5, 5, mFillPaint);
			canvas.drawRoundRect(tenori.gridRect, 5, 5, mPaint);
		}
		else
		{
			canvas.drawRect(tenori.gridRect, mFillPaint);
			canvas.drawRect(tenori.gridRect, mPaint);
		}

	}

	protected void drawSmoke(Canvas canvas)
	{
		if (smoke)
		{
			if (!waves.isEmpty())
			{
				for (WaveElement w : waves)
				{
					e = elements.get(w.index);

					float d = (ttl - w.t) / (float) ttl;// amptitude based on decay 
														//- kind of lineaer (expr as fraction)
					int l = w.t * 25; // size of wave based on time. 
					if (l > 0)
					{
						float ex = e.gridRect.centerX();
						float ey = e.gridRect.centerY();
						if (color)
						{
							if (palette)
							{
								// Palette style
								r = (int) (15.9375f * e.index_x);
								g = (int) (15.9375f * e.index_y);
								b = (int) (w.t * 5);

							}
							else
							{
								// random colors mutiplied by decay factor
								r = (int) (random.nextInt(255) * d * 1.8f);
								g = (int) (random.nextInt(255) * d * 1.8f);
								b = (int) (random.nextInt(255) * d * 1.8f);
								// note *1 gives a little unsaturated colors
								// but *2 creates too much white outs
								// probably a sine curve can normalize the values
							}
						}
						else
						{
							r = g = b = (int) (random.nextInt(100) * d);
						}
						colors[6] = colors[0] = Color.argb(0, r, g, b);
						colors[5] = colors[1] = Color.argb(38, r, g, b);
						colors[4] = colors[2] = Color.argb(76, r, g, b);
						colors[3] = Color.argb(115, r, g, b);

						gradient = new RadialGradient(ex, ey, l, colors, null, android.graphics.Shader.TileMode.CLAMP);

						mWavePaint.setShader(gradient);
						canvas.drawCircle(ex, ey, l, mWavePaint);
					}

					w.t++;

				}

				do
				{
					flagRemove = false;
					length = waves.size();
					for (int i = 0; i < length; i++)
					{
						if (waves.get(i).t > ttl)
						{
							waves.remove(i);
							flagRemove = true;
							break;
						}
					}
				}
				while (flagRemove);
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
										case MSG_PLAY:

										{
											rowplay++;
											rowplay %= 16;
											int length = elements.size();
											for (int i = rowplay; i < length; i += flapLength)
											{
												e = elements.get(i);
												if (padswitches[e.index])
												{
													chimeLock.lock();
													if (mChimeThread != null)
														mChimeThread.addTone(e.index_x);
													chimeLock.unlock();
												}

											}
										}
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

	public void setChimeThread(ChimeThread chimeThread)
	{
		chimeLock.lock();
		this.mChimeThread = chimeThread;
		chimeLock.unlock();
	}
}
