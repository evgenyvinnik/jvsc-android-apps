package ca.jvsh.andorion.layer;

import java.util.ArrayList;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;

import ca.jvsh.andorion.AndorionActivity;
import ca.jvsh.andorion.animation.CircleAnimation;
import ca.jvsh.andorion.animation.CrossAnimation;
import ca.jvsh.andorion.animation.DiamondAnimation;
import ca.jvsh.andorion.animation.LedAnimation;
import ca.jvsh.andorion.animation.PlusAnimation;
import ca.jvsh.andorion.animation.SquareAnimation;

import android.graphics.Canvas;
import android.graphics.Color;

public class ScoreLayer extends MusicLayer
{
	boolean	padSwitches[][]	=
									new boolean[AndorionActivity.GRID][AndorionActivity.GRID];
	float[]	xsPrev			= new float[MultiTouchController.MAX_TOUCH_POINTS];
	float[]	ysPrev			= new float[MultiTouchController.MAX_TOUCH_POINTS];
	int[]	prevIndexX		= new int[MultiTouchController.MAX_TOUCH_POINTS];
	int[]	prevIndexY		= new int[MultiTouchController.MAX_TOUCH_POINTS];

	int		columnPlay = 0;
	
	int startColumn = 0;
	int stopColumn = AndorionActivity.GRID;
	
	private ArrayList<LedAnimation> animations = new ArrayList<LedAnimation>();
	private int animationType = 4;
	private boolean flagRemoveAnimation;
	
	public ScoreLayer()
	{
		super();
		for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
		{
			xsPrev[i] = -1;
			ysPrev[i] = -1;
			prevIndexX[i] = -1;
			prevIndexY[i] = -1;
		}
		
		setStartColumn(0);
		setStopColumn(AndorionActivity.GRID);
		columnPlay = startColumn;
	}

	@Override
	public void startMusic()
	{
		super.startMusic();

	}

	@Override
	public void stopMusic()
	{
		super.stopMusic();
	}

	public void tick()
	{
		columnPlay++;
		if(columnPlay > stopColumn)
		{
			columnPlay = startColumn;
		}
		//double check
		columnPlay %= AndorionActivity.GRID;
		
		int column = columnPlay;
		for(int i = 0; i < AndorionActivity.GRID; i++)
		{
			if(padSwitches[i][column])
			{
				//TODO
				//play sound
				
				//add animation
				switch(animationType)
				{
					case 0:
					default:
						animations.add(new PlusAnimation(5, true, i, column));
						break;
					case 1:
						animations.add(new CrossAnimation(5, true, i, column));
						break;
					case 2:
						animations.add(new SquareAnimation(5, true, i, column));
						break;
					case 3:
						animations.add(new CircleAnimation(5, false, i, column));
						break;
					case 4:
						animations.add(new DiamondAnimation(5, false, i, column));
						break;
				}
				
			}
		}
	}

	@Override
	public void setSize(int width, int height)
	{
		super.setSize(width, height);
	}

	@Override
	public void drawGrid(Canvas canvas, PointInfo currTouchPoint)
	{
		super.drawGrid(canvas, currTouchPoint);

		switchPads(currTouchPoint);


		drawPads(canvas);
		
		drawAnimations(canvas);
		
		drawCurrentColumn(canvas);
		
	}
	
	private void drawPads(Canvas canvas)
	{
		mFillPaint.setColor(Color.argb(204, 120, 120, 120));
		for (int i = 0; i < AndorionActivity.GRID; i++)
		{
			for (int j = 0; j < AndorionActivity.GRID; j++)
			{
				if (padSwitches[i][j])
					canvas.drawRoundRect(gridRect[i][j], 5, 5, mFillPaint);
			}
		}
	}
	
	private void drawCurrentColumn(Canvas canvas)
	{
		mFillPaint.setColor(Color.argb(204, 255, 127, 39));
		int row = 0;
		int column = columnPlay;

		while( row < AndorionActivity.GRID)
		{
			canvas.drawRoundRect(gridRect[row][column], 5, 5, mFillPaint);
			row += 4;
		}
	}
	
	private void drawAnimations(Canvas canvas)
	{
		if(!animations.isEmpty())
		{
			mFillPaint.setColor(Color.argb(204, 112, 146, 190));

			for (LedAnimation animation : animations)
			{
				animation.drawAnimation(canvas, mFillPaint, gridRect);
			}

			do
			{
				flagRemoveAnimation = false;
				for (int i = 0; i < animations.size(); i++)
				{
					if (animations.get(i).exterminate)
					{
						animations.remove(i);
						flagRemoveAnimation = true;
						break;
					}
				}
			}
			while (flagRemoveAnimation);
		}
	}
	
	private void switchPads(PointInfo currTouchPoint)
	{
		int numPoints = currTouchPoint.getNumTouchPoints();

		if (currTouchPoint.isDown())
		{
			float[] xs = currTouchPoint.getXs();
			float[] ys = currTouchPoint.getYs();
			boolean found;

			for (int idx = 0; idx < numPoints; idx++)
			{
				found = false;

				for (int i = 0; i < AndorionActivity.GRID; i++)
				{
					for (int j = 0; j < AndorionActivity.GRID; j++)
					{
						if (gridRect[i][j].contains(xs[idx], ys[idx]))
						{
							if ((prevIndexY[idx] == i && prevIndexX[idx] == j)
									&& (Math.abs(xs[idx] - xsPrev[idx]) < 4 || Math
											.abs(ys[idx] - ysPrev[idx]) < 4))
								break;
							synchronized (padSwitches)
							{
								padSwitches[i][j] = !padSwitches[i][j];
							}
							prevIndexY[idx] = i;
							prevIndexX[idx] = j;
							xsPrev[idx] = xs[idx];
							ysPrev[idx] = ys[idx];
							break;
						}
					}
					if (found)
						break;
				}

			}

		}
		else
		{
			for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
			{
				xsPrev[i] = -1;
				ysPrev[i] = -1;
				prevIndexX[i] = -1;
				prevIndexY[i] = -1;
			}
		}
	}
	
	@Override
	public void setStartColumn(int startLoop)
	{
		startColumn = startLoop;
		if(startColumn < 0 )
			startColumn = 0;
		else if(startColumn > stopColumn)
			startColumn = stopColumn;
	}
	
	@Override
	public void setStopColumn(int stopLoop)
	{
		stopColumn = stopLoop;
		if(stopColumn > AndorionActivity.GRID )
			stopColumn = AndorionActivity.GRID;
		else if(stopColumn < startColumn)
			stopColumn = startColumn;
	}


}
