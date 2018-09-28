/**
 * 
 */
package com.gueei.mario.coinBlock;

public class Sprite {
	private int currentFrame;
	public static final int[] EMPTY = new int[0];
	private int _height;
	private int _totalFrame;

	private int _width;

	private int[][] appearance;

	public Sprite(int[][] sequence, int width, int height, int totalFrame) {
		_width = width;
		_height = height;
		_totalFrame = totalFrame;
		appearance = sequence.clone();
		currentFrame = 0;
	}

	public int getHeight() {
		return _height;
	}

	public int[] getSprite(int frame) {
		if ((frame >= 0) && (frame < _totalFrame))
			return appearance[frame];
		return EMPTY;
	}

	public int getTotalFrame() {
		return _totalFrame;
	}

	public int getWidth() {
		return _width;
	}

	public int[] getSprite() {
		return getSprite(NextFrame());
	}
	
	public static final Sprite Null = new Sprite(
		new int[][] {}, 0, 0, 0);
	
	public int NextFrame(){
		int result = currentFrame;
		currentFrame = ++currentFrame % getTotalFrame();
		return result;
	}
}