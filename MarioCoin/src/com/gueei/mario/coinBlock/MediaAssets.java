package com.gueei.mario.coinBlock;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gueei.mario.coinBlock.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.util.Log;

public class MediaAssets {
	private Hashtable<Integer, MediaPlayer> soundPlayers = new Hashtable<Integer, MediaPlayer>();
	private Hashtable<Integer, Sprite> sprites = new Hashtable<Integer, Sprite>();
	private Resources resources;

	private MediaAssets() {
	}

	private static MediaAssets singleton;

	public static MediaAssets getInstance() {
		if (singleton == null) {
			singleton = new MediaAssets();
			singleton.prepareAssets();
		}
		return singleton;
	}

	private void prepareAssets() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = true;
		Context context = CoinBlockWidgetApp.getApplication();
		resources = context.getResources();
	}

	// Lazy loading
	public Sprite getSprite(int resId) {
		Sprite output = sprites.get(resId);
		if (output == null) {
			try {
				long start = System.currentTimeMillis();
				output = CreateSprite(resId);
				sprites.put(resId, output);
				Log.d("coinBlock", System.currentTimeMillis() - start + " lazy load " + resId);
			} catch (Exception e) {
				output = Sprite.Null;
			}
		}
		return output;
	}

	public MediaPlayer getSoundPlayer(int resId) {
		MediaPlayer output = soundPlayers.get(resId);
		if (output == null) {
			try {
				output = CreateSoundPlayer(resId);
				soundPlayers.put(resId, output);
			} catch (Exception e) {
				output = null;
			}
		}
		return output;
	}

	private MediaPlayer CreateSoundPlayer(int resId) {
		return MediaPlayer.create(CoinBlockWidgetApp.getApplication(), resId);
	}

	private Sprite CreateSprite(int resId) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = true;
		Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, options);
		int frame = parseNumberOfFrame(resId);
		int bwidth = bitmap.getWidth() / frame;
		int bheight = bitmap.getHeight();
		int[][] pixels = new int[frame][bwidth * bheight];
		for (int i = 0; i < frame; i++) {
			bitmap.getPixels(pixels[i], 0, bwidth, bwidth * i, 0, bwidth, bheight);
		}
		Sprite newSprite = new Sprite(pixels, bwidth, bheight, frame);
		return newSprite;
	}

	private int parseNumberOfFrame(int resId) {
		String spriteName = resources.getResourceEntryName(resId);
		int numOfFrame = 1;
		try {
			Pattern p = Pattern.compile("\\d+$", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(spriteName);
			if (m.find()) {
				String strFrame = m.group();
				numOfFrame = Integer.parseInt(strFrame);
			}
		} catch (Exception e) {
			numOfFrame = 1;
		}
		return Math.max(1, numOfFrame);
	}
}
