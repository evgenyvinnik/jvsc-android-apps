package ca.jvsh.andorion.animation;

import ca.jvsh.andorion.AndorionActivity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class PlusAnimation extends LedAnimation
{
	int start;
	int stop;

	public PlusAnimation(int animationSize, boolean animationBehavior, int row, int column)
	{
		super(animationSize, animationBehavior, row, column);
	}
	
	public void drawAnimation(Canvas canvas, Paint paint, RectF[][] gridRect)
	{
		{
			start = mRow - ttl;
			if(start < 0) 
				start = 0;
			
			stop = mRow + ttl + 1;
			if(stop > AndorionActivity.GRID) 
				stop = AndorionActivity.GRID;
			
			for(int i = start; i < stop; i++)
				canvas.drawRoundRect(gridRect[i][mColumn], 5, 5, paint);
		}

		{
			start = mColumn - ttl;
			if(start < 0) 
				start = 0;
			
			stop = mColumn + ttl + 1;
			if(stop > AndorionActivity.GRID) 
				stop = AndorionActivity.GRID;
			
			for(int i = start; i < stop; i++)
				canvas.drawRoundRect(gridRect[mRow][i], 5, 5, paint);
		}
		
		super.drawAnimation(canvas, paint, gridRect);
	}
}
