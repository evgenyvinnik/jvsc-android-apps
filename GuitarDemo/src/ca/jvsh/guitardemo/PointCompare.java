package ca.jvsh.guitardemo;

import java.util.Comparator;

import android.graphics.PointF;

public class PointCompare implements Comparator<PointF>
{

	public int compare(final PointF a, final PointF b)
	{
		if (a.x < b.x)
		{
			return -1;
		}
		else if (a.x > b.x)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
}
