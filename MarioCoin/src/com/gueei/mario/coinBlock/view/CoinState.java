package com.gueei.mario.coinBlock.view;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;

import com.gueei.mario.coinBlock.MediaAssets;
import com.gueei.mario.coinBlock.R;
import com.gueei.mario.coinBlock.Sprite;
import com.gueei.mario.coinBlock.SpriteHelper;

class CoinState implements ICoinBlockViewState {
	public static final int COLLECTING_PERIOD = 5000;
	public static long endCollectingMoney = Long.MAX_VALUE;

	Sprite sp = MediaAssets.getInstance().getSprite(R.drawable.brick_question);
	MediaPlayer snd = MediaAssets.getInstance().getSoundPlayer(R.raw.smb_coin);
	private int animStage = 0;
	private int[] heightModifier = { 12, 8, 4, 2 };

	public CoinState(CoinBlockView viewContext) {
		init(viewContext);
		endCollectingMoney = System.currentTimeMillis() + COLLECTING_PERIOD;
	}

	void init(CoinBlockView viewContext)
	{
		viewContext.createCoin();
		animStage = 0;
		snd.seekTo(0);
		snd.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			public void onSeekComplete(MediaPlayer mp) {
				snd.start();
			}
		});
	}
	
	public void Draw(CoinBlockView viewContext, Bitmap canvas) {
		// Draw the brick at bottom
		SpriteHelper.DrawSprite(canvas, sp, 0, SpriteHelper.DrawPosition.BottomCenter, 0,
						- (int)(heightModifier[animStage] * viewContext.getDensity()));

		animStage++;
		if (animStage >= heightModifier.length) {
			if (endCollectingMoney > System.currentTimeMillis())
				viewContext.setState(new CoinDispatchedState(this));
			else {
				viewContext.setState(new DisabledState(viewContext));
			}
		}
	}

	public void OnClick(CoinBlockView viewContext) {
	}

	public boolean NeedRedraw() {
		return true;
	}
	
	private class CoinDispatchedState extends NormalState{
		private CoinState mState;
		public CoinDispatchedState(CoinState state){
			mState = state;
		}
		@Override
		public void OnClick(CoinBlockView viewContext) {
			mState.init(viewContext);
			viewContext.setState(mState);
		}
	}
}