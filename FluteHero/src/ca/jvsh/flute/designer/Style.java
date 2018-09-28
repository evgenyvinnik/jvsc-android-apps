package ca.jvsh.flute.designer;

import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Canvas;
import android.graphics.PointF;

public interface Style
{
	public void strokeStart(float x, float y);

	public void stroke(Canvas c, float x, float y);

	public void draw(Canvas c);

	public void saveState(HashMap<Integer, Object> state);

	public void restoreState(HashMap<Integer, Object> state);

	public ArrayList<PointF> getPoints();
}
