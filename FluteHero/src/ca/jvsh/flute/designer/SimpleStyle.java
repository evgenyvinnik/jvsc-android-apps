package ca.jvsh.flute.designer;

import java.util.ArrayList;
import java.util.HashMap;

import ca.jvsh.flute.designer.Style;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;

class SimpleStyle implements Style
{
	private float	prevX;
	private float	prevY;
	ArrayList<PointF> points;

	private Paint	paint	= new Paint();

	{
		paint.setAntiAlias(true);
		paint.setStrokeWidth(2F);
		points = new ArrayList<PointF>();
	}

	@Override
	public void stroke(Canvas c, float x, float y)
	{

		if (prevY < y)
		{
			double angle = Math.atan2(x - prevX, y - prevY);

			if (angle < Math.PI / 3.0 && angle > -Math.PI / 3.0)
			{
				paint.setColor(0xFFC0E0EF);
				paint.setPathEffect(null);
				c.drawLine(prevX, prevY, x, y, paint);

				paint.setColor(0xFF93C9E3);
				paint.setPathEffect(new DashPathEffect(new float[] { 2, 2 }, 0));

				c.drawLine(c.getWidth() - prevX, prevY, c.getWidth() - x, y, paint);

				prevX = x;
				prevY = y;
			}
		}
	}

	@Override
	public void strokeStart(float x, float y)
	{
		prevX = x;
		prevY = y;
		points.clear();
		points.add(new PointF(x, y));
	}

	@Override
	public void draw(Canvas c)
	{
	}


	@Override
	public void saveState(HashMap<Integer, Object> state)
	{
	}

	@Override
	public void restoreState(HashMap<Integer, Object> state)
	{
	}
	
	@Override
	public ArrayList<PointF> getPoints()
	{
		return points;
	}
}
