package com.gueei.mario.coinBlock;

import android.content.Context;
import android.graphics.Bitmap;

public class MushroomAnimation implements IAnimatable {
	private Sprite sp = MediaAssets.getInstance().getSprite(R.drawable.mushroom);
	private int upstep, movestep;
	private float density;
	
	public MushroomAnimation(float Density)
	{
		upstep = 10;
		movestep = 10;
		density = Density;
	}

	public boolean AnimationFinished()
	{
		return movestep<=0;
	}
	
	public void Draw(Bitmap canvas)
	{
		int top = -(int)((16-upstep) * 2 * density);
		int right = (int)((10-movestep) * 2  * density);
		SpriteHelper.DrawSprite(canvas, sp, 0, SpriteHelper.DrawPosition.BottomCenter, right, top);
		// Two stage
		if (upstep > 0){
		// Draw sprite
			upstep--;
		}
		else{
			movestep--;
		}
	}
}