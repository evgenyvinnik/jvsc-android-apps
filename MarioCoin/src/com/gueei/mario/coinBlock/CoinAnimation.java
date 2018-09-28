package com.gueei.mario.coinBlock;

import android.content.Context;
import android.graphics.Bitmap;

public class CoinAnimation implements IAnimatable {
	private Sprite sp = MediaAssets.getInstance().getSprite(R.drawable.money_sprites_4);
	private int currentSprite = 0;
	private int step;
	private float density;

	public CoinAnimation(float Density) {
		step = 10;
		currentSprite = 0;
		density = Density;
	}

	public boolean AnimationFinished() {
		return step == 0;
	}

	public void Draw(Bitmap canvas) {
		// Draw sprite
		SpriteHelper.DrawSprite(canvas, sp, currentSprite, SpriteHelper.DrawPosition.TopCenter, 0,
						(int) (step * 2 * density));
		step--;
		currentSprite++;
		currentSprite %= 4;
	}
}
