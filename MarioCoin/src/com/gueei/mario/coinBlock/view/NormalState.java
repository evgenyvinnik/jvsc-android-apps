package com.gueei.mario.coinBlock.view;

import android.graphics.Bitmap;

import com.gueei.mario.coinBlock.MediaAssets;
import com.gueei.mario.coinBlock.R;
import com.gueei.mario.coinBlock.Sprite;
import com.gueei.mario.coinBlock.SpriteHelper;

class NormalState implements ICoinBlockViewState {
	public void Draw(CoinBlockView viewContext, Bitmap canvas) {
		Sprite sp = MediaAssets.getInstance().getSprite(R.drawable.brick_question);
		// Draw the brick at bottom
		SpriteHelper.DrawSprite(canvas, sp, 0, SpriteHelper.DrawPosition.BottomCenter);
	}

	public void OnClick(CoinBlockView viewContext) {
		long chance = System.currentTimeMillis() % 8;
		if (chance < 3)
			viewContext.setState(new MushroomState(viewContext));
		else if (chance < 6)
			viewContext.setState(new FlowerState(viewContext));
		else
			viewContext.setState(new CoinState(viewContext));
	}

	public boolean NeedRedraw() {
		return false;
	}
}