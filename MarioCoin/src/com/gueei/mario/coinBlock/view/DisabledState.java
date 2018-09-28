package com.gueei.mario.coinBlock.view;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Handler;

import com.gueei.mario.coinBlock.MediaAssets;
import com.gueei.mario.coinBlock.R;
import com.gueei.mario.coinBlock.Sprite;
import com.gueei.mario.coinBlock.SpriteHelper;

class DisabledState implements ICoinBlockViewState {
	public static final int DISABLE_PERIOD = 5000;

	Sprite sp = MediaAssets.getInstance().getSprite(R.drawable.brick_disabled);
	MediaPlayer snd = MediaAssets.getInstance().getSoundPlayer(R.raw.smb_bump);
	CoinBlockView mViewContext;
	
	public DisabledState(CoinBlockView viewContext) {
		this.mViewContext = viewContext;
		// Will return to normal state
		(new Handler()).postDelayed(new Runnable() {
			public void run() {
				mViewContext.setState(new NormalState());
			}
		}, DISABLE_PERIOD); // After 5 second, reutrn to normal
	}

	public void Draw(CoinBlockView viewContext, Bitmap canvas) {
		// Draw the brick at bottom
		SpriteHelper.DrawSprite(canvas, sp, 0, SpriteHelper.DrawPosition.BottomCenter);
	}

	public void OnClick(CoinBlockView viewContext) {
		if (snd.isPlaying()) return;
		snd.seekTo(0);
		snd.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			public void onSeekComplete(MediaPlayer mp) {
				snd.start();
			}
		});
	}

	public boolean NeedRedraw() {
		return false;
	}
}

