package ca.jvsh.andorion.animation;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.jvsh.andorion.AndorionActivity;

public class DiamondAnimation  extends LedAnimation
{
	int startRow;
	int startColumn;
	int length;
	
	public DiamondAnimation(int animationSize, boolean animationBehavior, int row, int column)
	{
		super(animationSize, animationBehavior, row, column);
	}
	
	public void drawAnimation(Canvas canvas, Paint paint, RectF[][] gridRect)
	{
		{
			startColumn = mColumn - ttl;
			startRow = mRow;
						
			for(int i = 0; i < ttl; i++)
			{
				startColumn++;
				if(startColumn > 0 && startColumn < AndorionActivity.GRID && startRow > 0 && startRow < AndorionActivity.GRID)
					canvas.drawRoundRect(gridRect[startRow][startColumn], 5, 5, paint);
				startRow--;
			}
			
		}
		
		{
			startColumn = mColumn - ttl;
			startRow = mRow;
						
			for(int i = 0; i < ttl; i++)
			{
				startColumn++;
				if(startColumn > 0 && startColumn < AndorionActivity.GRID && startRow > 0 && startRow < AndorionActivity.GRID)
					canvas.drawRoundRect(gridRect[startRow][startColumn], 5, 5, paint);
				startRow++;
			}
			
		}
		
		{
			startColumn = mColumn + ttl ;
			startRow = mRow;
						
			for(int i = 0; i < ttl; i++)
			{
				startColumn--;
				if(startColumn > 0 && startColumn < AndorionActivity.GRID && startRow > 0 && startRow < AndorionActivity.GRID)
					canvas.drawRoundRect(gridRect[startRow][startColumn], 5, 5, paint);
				startRow--;
			}
			
		}
		
		{
			startColumn = mColumn + ttl;
			startRow = mRow;
						
			for(int i = 0; i < ttl; i++)
			{
				startColumn--;
				if(startColumn > 0 && startColumn < AndorionActivity.GRID && startRow > 0 && startRow < AndorionActivity.GRID)
					canvas.drawRoundRect(gridRect[startRow][startColumn], 5, 5, paint);
				startRow++;
			}
			
		}
		
		super.drawAnimation(canvas, paint, gridRect);
	}
}