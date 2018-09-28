package com.gueei.mario.coinBlock.view;

import com.gueei.mario.coinBlock.IAnimatable;
import android.os.Handler;
import com.gueei.mario.coinBlock.MediaAssets;
import com.gueei.mario.coinBlock.R;
import com.gueei.mario.coinBlock.Sprite;
import com.gueei.mario.coinBlock.SpriteHelper;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;

public class FlowerState implements ICoinBlockViewState {
	Sprite sp = MediaAssets.getInstance().getSprite(R.drawable.brick_disabled);
	MediaPlayer snd = MediaAssets.getInstance().getSoundPlayer(R.raw.smb_powerup_appears);
	private int animStage = 0;
	private int[] heightModifier = { 12, 8, 4, 2 };
	FlowerAnimation flowerAnim;
	CoinBlockView context;

	public FlowerState(CoinBlockView viewContext) {
		context = viewContext;
		flowerAnim = new FlowerAnimation();
		viewContext.addAnimatable(flowerAnim);
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
						-(int) (heightModifier[animStage] * viewContext.getDensity()));
		animStage++;
		if (animStage >= heightModifier.length)
			viewContext.setState(new FlowerWaitState(viewContext));
	}

	public boolean NeedRedraw() {
		return true;
	}

	public void OnClick(CoinBlockView viewContext) {
		// TODO Auto-generated method stub
	}

	private class FlowerWaitState implements ICoinBlockViewState {
		Sprite sp = MediaAssets.getInstance().getSprite(R.drawable.brick_disabled);
		MediaPlayer snd = MediaAssets.getInstance().getSoundPlayer(R.raw.smb_powerup);
		CoinBlockView mViewContext;

		public FlowerWaitState(CoinBlockView viewContext) {
			mViewContext = viewContext;
			(new Handler()).postDelayed(new Runnable(){
				public void run() {
					if (mViewContext.getState().getClass() == FlowerWaitState.class)
					{
						mViewContext.removeAnimatable(flowerAnim);
						mViewContext.setState(new NormalState());
					}
				}
			}, 5000);
		}

		public void OnClick(CoinBlockView viewContext) {
			viewContext.removeAnimatable(flowerAnim);
			snd.seekTo(0);
			snd.setOnSeekCompleteListener(new OnSeekCompleteListener() {
				public void onSeekComplete(MediaPlayer mp) {
					snd.start();
				}
			});
			viewContext.setState(new DisabledState(viewContext));
		}

		public void Draw(CoinBlockView viewContext, Bitmap canvas) {
			SpriteHelper.DrawSprite(canvas, sp, 0, SpriteHelper.DrawPosition.BottomCenter);
		}

		public boolean NeedRedraw() {
			return false;
		}

	}

	private class FlowerAnimation implements IAnimatable {
		Sprite flowerSprite = MediaAssets.getInstance().getSprite(R.drawable.flowers_sprites_4);
		private int flowerRaise = 4;

		public boolean AnimationFinished() {
			return false;
		}

		public void Draw(Bitmap canvas) {
			SpriteHelper.DrawSprite(canvas, flowerSprite, flowerSprite.NextFrame(),
							SpriteHelper.DrawPosition.BottomCenter, 0, -(int) (flowerRaise * 4 * context.getDensity()));
			// Draw the flower
			if (flowerRaise < 8) {
				flowerRaise++;
			}
		}
	}
}
