package com.gueei.mario.coinBlock;

import android.graphics.Bitmap;

public interface IAnimatable {

	public abstract boolean AnimationFinished();

	public abstract void Draw(Bitmap canvas);

}