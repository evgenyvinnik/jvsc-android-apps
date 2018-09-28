package ca.jvsh.andorion.animation;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import ca.jvsh.andorion.AndorionActivity;

public class CircleAnimation extends LedAnimation
{

	public CircleAnimation(int animationSize, boolean animationBehavior,
			int row, int column)
	{
		super(animationSize, animationBehavior, row, column);
	}

	public void drawAnimation(Canvas canvas, Paint paint, RectF[][] gridRect)
	{
		int radius = ttl;
		int x = -radius, y = 0, err = 2 - 2 * radius; /* II. Quadrant */
		do
		{

			if (mRow - x < AndorionActivity.GRID
					&& mColumn + y < AndorionActivity.GRID)
				canvas.drawRoundRect(gridRect[mRow - x][mColumn + y], 5, 5,
						paint);
			if (mRow - y > 0 && mColumn - x < AndorionActivity.GRID)
				canvas.drawRoundRect(gridRect[mRow - y][mColumn - x], 5, 5,
						paint);
			if (mRow + x > 0 && mColumn - y > 0)
				canvas.drawRoundRect(gridRect[mRow + x][mColumn - y], 5, 5,
						paint);
			if (mRow + y < AndorionActivity.GRID && mColumn + x > 0)
				canvas.drawRoundRect(gridRect[mRow + y][mColumn + x], 5, 5,
						paint);

			radius = err;
			if (radius > x)
				err += ++x * 2 + 1; /* e_xy+e_x > 0 */
			if (radius <= y)
				err += ++y * 2 + 1; /* e_xy+e_y < 0 */
		}
		while (x < 0);

		super.drawAnimation(canvas, paint, gridRect);
	}
}
