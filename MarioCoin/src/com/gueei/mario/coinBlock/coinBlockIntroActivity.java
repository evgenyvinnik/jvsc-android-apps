package com.gueei.mario.coinBlock;

import com.gueei.mario.coinBlock.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class coinBlockIntroActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button btnGetWallpaper = (Button)this.findViewById(R.id.btnGetWallpaper);
        btnGetWallpaper.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Uri uri = Uri.parse("http://andytsui.wordpress.com/tag/wallpaper/");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}});
    }
}