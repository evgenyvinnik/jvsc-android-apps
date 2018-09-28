/**
 * Copyright (C) 2012  Evgeny Vinnik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.jvsh.flute.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import ca.jvsh.flute.R;

/**
 * A slider around a circle, to select a date
 */
final class TurnKnob extends View
{

	private int			width;
	private int			height;
	private int			centerX;
	private int			centerY;
	private int			diameter;
	private int			thickness;
	private float		maxSlider;
	private RectF		outerCircle;
	private RectF		innerCircle;
	private int			innerRadius;

	private final Paint	sliderPaint		= new Paint();
	private final Paint	textPaint		= new Paint();
	private final Paint	knobEdgePaint	= new Paint();

	private int			sliderUndeneathColor;
	private int			sliderStateColor;
	private int			sliderTickColor;
	private int			textColor;
	private int			arrowColor;
	private int			knobColor;
	private int			knobEdgeColor;

	private float		startAngle;
	private float		endAngle;
	private float		currentAngle;

	private float		startValue;
	private float		endValue;
	private float		currentValue;

	private String		textFormat;

	private float		arrowAngle		= 60;
	private float		arrowLength;

	public TurnKnob(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setAttrs(context, attrs);

		if (startValue >= endValue)
			throw new IllegalArgumentException("Start value must be less than end value.");

		if (endAngle == 0)
			throw new IllegalArgumentException("End angle can not equal to zero.");

		if (startAngle < 0 || startAngle > 360)
			throw new IllegalArgumentException("Start angle should be between 0 and 360 degrees.");

		if (endAngle < 0 || endAngle > 360)
			throw new IllegalArgumentException("End angle should be between 0 and 360 degrees.");

		sliderPaint.setStyle(Paint.Style.STROKE);
		sliderPaint.setAntiAlias(true);

		textPaint.setAntiAlias(true);
		textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
		textPaint.setSubpixelText(true);
		textPaint.setTextAlign(Paint.Align.CENTER);

		knobEdgePaint.setColor(knobEdgeColor);
		knobEdgePaint.setStyle(Paint.Style.STROKE);
		knobEdgePaint.setPathEffect(new DashPathEffect(new float[] { 10, 5 }, 0));
		knobEdgePaint.setAntiAlias(true);

	}

	private void setAttrs(Context context, AttributeSet attrs)
	{
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TurnKnob);

		//set colors
		sliderUndeneathColor = a.getColor(R.styleable.TurnKnob_sliderUndeneathColor, Color.rgb(115, 115, 115));
		sliderStateColor = a.getColor(R.styleable.TurnKnob_sliderStateColor, Color.rgb(255, 0, 165));
		sliderTickColor = a.getColor(R.styleable.TurnKnob_sliderTickColor, Color.WHITE);
		textColor = a.getColor(R.styleable.TurnKnob_textColor, Color.WHITE);
		arrowColor = a.getColor(R.styleable.TurnKnob_arrowColor, Color.WHITE);
		knobColor = a.getColor(R.styleable.TurnKnob_knobColor, Color.rgb(0, 0, 0xFF));
		knobEdgeColor = a.getColor(R.styleable.TurnKnob_knobEdgeColor, Color.rgb(0, 0xFF, 0xFF));

		//set values
		startAngle = a.getFloat(R.styleable.TurnKnob_startAngle, 130);
		endAngle = a.getFloat(R.styleable.TurnKnob_endAngle, 280);
		currentAngle = a.getFloat(R.styleable.TurnKnob_currentAngle, 0);

		//set 
		startValue = a.getFloat(R.styleable.TurnKnob_startValue, 0);
		endValue = a.getFloat(R.styleable.TurnKnob_endValue, 100);
		currentValue = a.getFloat(R.styleable.TurnKnob_currentValue, 0);

		arrowAngle = a.getFloat(R.styleable.TurnKnob_arrowAngle, 60);

		textFormat = a.getString(R.styleable.TurnKnob_textFormat);
		if (textFormat == null)
			textFormat = "%.1f";
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (getWidth() != width || getHeight() != height)
		{
			width = getWidth();
			height = getHeight();
			centerX = width / 2;
			centerY = height / 2;

			diameter = Math.min(width, height);
			maxSlider = (diameter * 1.3f) / 2;
			thickness = diameter / 15;
			innerRadius = (diameter - thickness * 3) / 2;

			arrowLength = 2* thickness;

			sliderPaint.setStrokeWidth(thickness);
			textPaint.setTextSize(diameter * 0.32f);
			knobEdgePaint.setStrokeWidth(thickness / 3);

			int left = (width - diameter) / 2;
			int top = (height - diameter) / 2;
			int bottom = top + diameter;
			int right = left + diameter;
			outerCircle = new RectF(left + thickness / 2, top + thickness / 2, right - thickness / 2, bottom - thickness / 2);

			int innerDiameter = diameter - thickness * 2;
			innerCircle = new RectF(left + thickness, top + thickness, left + thickness + innerDiameter, top + thickness + innerDiameter);
		}

		drawClock(canvas);
		drawInternalKnob(canvas);
		drawClockTextAndButtons(canvas);

	}

	public float getCurrentValue()
	{
		return currentValue;
	}

	public void setCurrentValue(float value)
	{
		//check the borders
		if (value < startValue)
			throw new IllegalArgumentException("Value must be greater or equal than start value.");

		if (value > endValue)
			throw new IllegalArgumentException("Value must be equal or less than end value.");

		currentValue = value;

		//calculate the angle
		currentAngle = endAngle * (currentValue - startValue) / (endValue - startValue);

		//calculate the angle
		postInvalidate();
	}

	/**
	 * Draw a circle and an arc of the selected duration from start thru end.
	 */
	private void drawClock(Canvas canvas)
	{

		// the grey empty part of the circle
		sliderPaint.setColor(sliderUndeneathColor);
		canvas.drawArc(outerCircle, startAngle, endAngle, false, sliderPaint);

		// the colored "filled" part of the circle
		sliderPaint.setColor(sliderStateColor);
		canvas.drawArc(outerCircle, startAngle, currentAngle, false, sliderPaint);

		// the white selected part of the circle
		sliderPaint.setColor(sliderTickColor);
		canvas.drawArc(outerCircle, startAngle + currentAngle - 1, 2, false, sliderPaint);

	}

	/**
	 * Write labels in the middle of the circle like so:
	 */
	private void drawClockTextAndButtons(Canvas canvas)
	{
		textPaint.setColor(textColor);
		canvas.drawText(String.format(textFormat, currentValue), centerX, centerY, textPaint);
	}

	private void drawInternalKnob(Canvas canvas)
	{
		//draw main part
		textPaint.setColor(knobColor);
		canvas.drawArc(innerCircle, 0, 360, true, textPaint);
		//draw moving edge  
		canvas.save();
		canvas.rotate(currentAngle, centerX, centerY);

		canvas.drawArc(innerCircle, 0, 358, false, knobEdgePaint);
		canvas.restore();

		//draw an arrow
		{
			float angle = startAngle + currentAngle;

			float x = (float) (centerX + innerRadius * Math.cos(angle / 180 * Math.PI));
			float y = (float) (centerY + innerRadius * Math.sin(angle / 180 * Math.PI));

			textPaint.setColor(arrowColor);
			canvas.drawArc(new RectF(x - arrowLength, y - arrowLength, x + arrowLength, y + arrowLength), angle + 180 - arrowAngle / 2, arrowAngle, true, textPaint);
		}

	}

	/**
	 * Accept a touches near the circle's edge, translate it to an angle, and
	 * update the sweep angle.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (outerCircle == null)
		{
			return true; // ignore all events until the canvas is drawn
		}

		int touchX = (int) event.getX();
		int touchY = (int) event.getY();

		// handle volume slider
		if (((centerX - touchX) * (centerX - touchX) + (centerY - touchY) * (centerY - touchY)) < (maxSlider * maxSlider))
		{
			float angle = pointToAngle(touchX, touchY, centerX, centerY);

			if (angle < startAngle)
			{
				if (startAngle + endAngle > 360)
				{
					float overflow = (startAngle + endAngle) % 360.0f;

					if (angle < overflow)
						angle = endAngle - overflow + angle;
					else if (startAngle - angle > angle - overflow)
						angle = endAngle;
					else
						angle = 0;
				}
				else
				{
					float underflow = 360 - (startAngle + endAngle);

					if (startAngle - angle < angle + underflow)
						angle = 0;
					else
						angle = endAngle;
				}

			}
			else
			{
				if (startAngle + endAngle > 360)
					angle = angle - startAngle;
				else if (angle > startAngle + endAngle)
				{
					if ((angle - (startAngle + endAngle)) < (360 - angle + startAngle))
						angle = endAngle;
					else
						angle = 0;
				}
				else
					angle = angle - startAngle;
			}

			setCurrentValue(startValue + (endValue - startValue) * angle / endAngle);
			return true;

		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns the number of degrees (0-359) for the given point, such that
	 * 3pm is 0 and 9pm is 180.
	 */
	private static float pointToAngle(int x, int y, int centerX, int centerY)
	{

		/* Get the angle from a triangle by dividing opposite by adjacent
		 * and taking the atan. This code is careful not to divide by 0.
		 *
		 *
		 *      adj | opp
		 *          |
		 * opp +180 | +270 adj
		 * _________|_________
		 *          |
		 * adj  +90 | +0   opp
		 *          |
		 *      opp | adj
		 *
		 */
		final double EPS = 0.0006;

		if (x >= centerX && y < centerY)
		{
			double opp = x - centerX;
			double adj = centerY - y;

			//protection from division by zero
			if (Math.abs(adj) < EPS)
				adj = EPS;

			return 270.0f + (float) Math.toDegrees(Math.atan(opp / adj));
		}
		else if (x > centerX && y >= centerY)
		{
			double opp = y - centerY;
			double adj = x - centerX;

			//protection from division by zero
			if (Math.abs(adj) < EPS)
				adj = EPS;

			return (float) Math.toDegrees(Math.atan(opp / adj));
		}
		else if (x <= centerX && y > centerY)
		{
			double opp = centerX - x;
			double adj = y - centerY;

			//protection from division by zero
			if (Math.abs(adj) < EPS)
				adj = EPS;

			return 90.0f + (float) Math.toDegrees(Math.atan(opp / adj));
		}
		else if (x < centerX && y <= centerY)
		{
			double opp = centerY - y;
			double adj = centerX - x;

			//protection from division by zero
			if (Math.abs(adj) < EPS)
				adj = EPS;

			return 180.0f + (float) Math.toDegrees(Math.atan(opp / adj));
		}

		throw new IllegalArgumentException();
	}

}
