package ca.jvsh.andorion.animation;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.jvsh.andorion.AndorionActivity;

public class CrossAnimation extends LedAnimation
{
	int startRow;
	int startColumn;
	int length;
	
	public CrossAnimation(int animationSize, boolean animationBehavior, int row, int column)
	{
		super(animationSize, animationBehavior, row, column);
	}
	
	public void drawAnimation(Canvas canvas, Paint paint, RectF[][] gridRect)
	{
		length = 2 * ttl + 1;

		{
			startRow = mRow - ttl - 1;
			startColumn = mColumn - ttl - 1;
			
			for(int i = 0; i < length; i++)
			{
				startRow++;
				startColumn++;
				
				if(startRow < 0 || startColumn < 0 || startRow >= AndorionActivity.GRID || startColumn >= AndorionActivity.GRID)
					continue;

				canvas.drawRoundRect(gridRect[startRow][startColumn], 5, 5, paint);

			}
		}
		{
			startRow = mRow + ttl + 1;
			startColumn = mColumn - ttl - 1;
			
			for(int i = 0; i < length; i++)
			{
				startRow--;
				startColumn++;
				
				if(startRow < 0 || startColumn < 0 || startRow >= AndorionActivity.GRID || startColumn >= AndorionActivity.GRID)
					continue;

				canvas.drawRoundRect(gridRect[startRow][startColumn], 5, 5, paint);

			}
		}
		
		
		super.drawAnimation(canvas, paint, gridRect);
	}
}
