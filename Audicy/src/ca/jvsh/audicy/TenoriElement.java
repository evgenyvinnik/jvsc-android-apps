package ca.jvsh.audicy;

import android.graphics.RectF;

public class TenoriElement
{
	public int	index_x;
	public int	index_y;
	public int	index;

	public RectF	gridRect;

	public TenoriElement(float x, float y, float gridWidth, int index_x, int index_y, int index)
	{
		gridRect = new RectF(x, y, x + gridWidth, y + gridWidth);

		this.index_x = index_x;
		this.index_y = index_y;
		this.index = index;
	}
}
