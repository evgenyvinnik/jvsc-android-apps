package ca.jvsh.flute.designer;

import java.util.ArrayList;

import ca.jvsh.flute.designer.StylesFactory;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

public class Controller implements View.OnTouchListener
{
	private Style			style;
	private final Canvas	mCanvas;
	private final int margin = FluteDesigningSurface.CellSize/2;
	private boolean			toDraw	= false;

	public Controller(Canvas canvas)
	{
		clear();
		mCanvas = canvas;
	}

	public void draw()
	{
		if (toDraw)
		{
			style.draw(mCanvas);
		}
	}

	public void setStyle(Style style)
	{
		toDraw = false;
		this.style = style;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		float x = event.getX();
		float y = event.getY(); 
		
		if( y >  mCanvas.getHeight() - margin)
			y =  mCanvas.getHeight() - margin;
		else if (y < margin)
			y = margin;
		
		if( x <  mCanvas.getWidth()/2 + margin)
			x =  mCanvas.getWidth()/2 + margin;
		else if (x < margin)
			x = margin;

		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				toDraw = true;
				style.strokeStart(x, y);
				break;
			case MotionEvent.ACTION_MOVE:
				style.stroke(mCanvas, x, y);
				break;
		}
		return true;
	}

	public void clear()
	{
		toDraw = false;
		StylesFactory.clearCache();
		setStyle(StylesFactory.getCurrentStyle());
	}


	public ArrayList<PointF> getPoints()
	{
		if(style != null)
			style.getPoints();
		
		return null;
	}

}
