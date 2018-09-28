package ca.jvsh.andorion.animation;

import ca.jvsh.andorion.AndorionActivity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class LedAnimation
{
	protected int mAnimationsize;
	protected boolean mAnimationBehavior;//true - shrinking, false (default) - expanding;
	
	protected int mRow;
	protected int mColumn;

	protected int ttl;
	public boolean exterminate;
	
	
	
	public LedAnimation(int animationSize, boolean animationBehavior, int row, int column)
	{
		mAnimationsize = animationSize;
		if(mAnimationsize < 1)
			mAnimationsize =1;
		if(mAnimationsize > AndorionActivity.GRID)
			mAnimationsize =AndorionActivity.GRID;
		
		mAnimationBehavior = animationBehavior;
		mColumn = column;
		mRow = row;
		
		if(mAnimationBehavior)
			ttl = mAnimationsize-1;
		else
			ttl = 0;	
		exterminate = false;
	}
	
	public void drawAnimation(Canvas canvas, Paint paint, RectF[][] gridRect)
	{
		if(mAnimationBehavior)
		{
			ttl--;
			if(ttl == 0)
				exterminate = true;
		}
		else
		{
			ttl++;
			if(ttl == mAnimationsize)
				exterminate = true;
		}
	}
}
