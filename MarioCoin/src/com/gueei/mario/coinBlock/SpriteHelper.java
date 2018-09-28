package com.gueei.mario.coinBlock;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class SpriteHelper {
	public static enum DrawPosition {
		Center, Top, TopLeft, TopRight, BottomLeft, BottomRight, BottomCenter, TopCenter, CenterLeft, CenterRight
	};
	
	public static void DrawSprite(Bitmap canvas, Sprite sp, int frame, int x, int y) {
		int[] pixels = sp.getSprite(frame);
		if (pixels.equals(Sprite.EMPTY))
			return;

		int rx = x;
		int ry = y;
		int rw = sp.getWidth();
		int rh = sp.getHeight();
		int stride = sp.getWidth();
		int offset = 0;
		// Handle overflow
		if ((rx + rw) > canvas.getWidth()) {
			rw = canvas.getWidth() - rx;
		}
		if ((ry + rh) > canvas.getHeight()) {
			rh = canvas.getHeight() - ry;
		}
		if ((rx < 0) && (ry >= 0)) {
			offset = -rx;
			rw = rw + rx;
			rx = 0;
		} else if ((rx < 0) && (ry < 0)) {
			offset = -(ry * stride + rx);
			rw = rw + rx;
			rh = rh + ry;
			rx = 0;
			ry = 0;
		} else if ((rx >= 0) && (ry < 0)) {
			offset = -(ry * stride);
			rh = rh + ry;
			ry = 0;
		}

		setPixels(canvas, pixels, offset, stride, rx, ry, rw, rh);
	}

	private static void setPixels(Bitmap canvas, int[] pixels, int offset, int stride, int x, int y, int w, int h) {
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				int pixel = pixels[offset + i * stride + j];
				if (pixel != Color.TRANSPARENT)
					canvas.setPixel(x + j, y + i, pixel);
			}
		}
	}

	public static void DrawSprite(Bitmap canvas, Sprite sp, int frame, DrawPosition position) {
		DrawSprite(canvas, sp, frame, position, 0, 0);
	}

	public static void DrawSprite(Bitmap canvas, Sprite sp, int frame, DrawPosition position, int offsetX, int offsetY) {
		DrawSprite(canvas, sp, frame, 
			getX(canvas, sp, position) + offsetX,
			getY(canvas, sp, position) + offsetY
		);
	}
	
	private static int getX(Bitmap canvas, Sprite sp, DrawPosition position)
	{
		int x = 0;
		if ((position.equals(DrawPosition.Center)) || (position.equals(DrawPosition.BottomCenter))
						|| (position.equals(DrawPosition.TopCenter))) {
			x = (canvas.getWidth() - sp.getWidth()) / 2;
		}
		return x;
	}

	private static int getY(Bitmap canvas, Sprite sp, DrawPosition position)
	{
		int y = 0;
		if ((position.equals(DrawPosition.CenterRight)) || (position.equals(DrawPosition.Center))
						|| (position.equals(DrawPosition.CenterRight))) {
			y = (canvas.getHeight() - sp.getHeight()) / 2;
		}
		if ((position.equals(DrawPosition.BottomCenter)) || (position.equals(DrawPosition.BottomLeft))
						|| (position.equals(DrawPosition.BottomRight))) {
			y = canvas.getHeight() - sp.getHeight();
		}
		return y;
	}

}
