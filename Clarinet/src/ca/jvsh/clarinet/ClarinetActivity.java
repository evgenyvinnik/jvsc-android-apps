package ca.jvsh.clarinet;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;

public class ClarinetActivity extends Activity
{
	
	String TAG = "Clarinet";
	private AudioTrack			mAudioOutput;
	
	int fs = 44100;
	int dur = 10;
	int smpCnt = dur * fs;
	
	float						frequency		=440.0f;
	float						amplitude		=1.0f;
	float						increment;
	float						angle			= 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		increment = (float) (2.0 * Math.PI) * frequency / ((float) fs);


		
		mAudioOutput = new AudioTrack(AudioManager.STREAM_MUSIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * smpCnt, AudioTrack.MODE_STATIC);

		
		Thread clarinet = new Thread()
		{
			public void run()
			{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				
				clarinet();
				//mAudioOutput.stop();
			}
		};
		
		clarinet.start();
		//clarinet();
	}

	void clarinet()
	{
		
		

		float c = 347.0f;
		float rho = 1.2f;

		float l = 0.52f;
		float a = 0.01f;
		float Z0 = rho * c / ((float) Math.PI * a * a);

		int ptr = 0;
		int N = (int) Math.floor(l * fs / c);
		float upper[] = new float[N];
		float lower[] = new float[N];
		float y0[] = new float[smpCnt];
		float yL[] = new float[smpCnt];

		float bRL[] = new float[] { -0.2162f, -0.2171f, -0.0545f };
		float aRL[] = new float[] { 1, -0.6032f, 0.0910f };

		float bTL[] = new float[] { -0.2162f + 1, -0.2171f + -0.6032f, -0.0545f + 0.0910f };
		float aTL[] = new float[] { 1, -0.6032f, 0.0910f };

		//float stateRL[] = new float [3];
		float stateRL[] = new float[2];

		//float stateTL[] = new float [3];
		float stateTL[] = new float[2];

		float bL[] = new float[] { 0.806451596106077f, -1.855863155228909f, 1.371191452991298f, -0.312274852426121f, -0.006883256612646f };
		float aL[] = new float[] { 1.000000000000000f, -2.392436499871960f, 1.891289981326362f, -0.511406512428537f, 0.015235504020645f };

		float R0 = 0.9f;

		float aw = 0.015f;
		float S = 0.034f * aw;
		float k = S * 10000000.0f;
		float H0 = 0.0007f;

		int nDecay = (int) (0.2 * fs);
		int nAttack = (int) (0.1 * fs);

		float multiplier = 5000.0f;
		float pm[] = new float[smpCnt];

		int m;
		for (m = 2; m <= nAttack; m++)
		{
			pm[m] = multiplier * (m-1) / nAttack;
		}

		for (; m < smpCnt - nAttack - nAttack; m++)
		{
			pm[m] = multiplier;
		}

		for (int j = 0; m < smpCnt; m++, j++)
		{
			pm[m] = multiplier * j / nDecay;
		}

		float U[] = new float[smpCnt];

		float dp;
		float x;
		float pr;

		float outL;
		float out0;

		//float stateLU[] = new float[4];
		float stateLU[] = new float[4];

		//float stateLL[] = new float[4];

		float stateLL[] = new float[4];

		for (int n = 1; n < smpCnt; n++)
		{
			dp = (float) Math.abs(pm[n] - y0[n - 1]);
			x = (float) Math.min(H0, dp * S / k);
			U[n] = aw * (H0 - x) * (float) Math.sqrt(dp * 2 / rho);
			pr = U[n] * Z0;

			
			//filters
			{
				outL = bL[0] * upper[ptr] + stateLU[0];
				stateLU[0] = bL[1] * upper[ptr] + stateLU[1] - aL[1] * outL;
				stateLU[1] = bL[2] * upper[ptr] + stateLU[2] - aL[2] * outL;
				stateLU[2] = bL[3] * upper[ptr] + stateLU[3] - aL[3] * outL;
				stateLU[3] = bL[4] * upper[ptr] 			 - aL[4] * outL;
				if(n < 100)
					Log.d(TAG, "outL " + outL);
			}

			{
				out0 = bL[0] * lower[ptr] + stateLL[0];
				stateLL[0] = bL[1] * lower[ptr] + stateLL[1] - aL[1] * out0;
				stateLL[1] = bL[2] * lower[ptr] + stateLL[2] - aL[2] * out0;
				stateLL[2] = bL[3] * lower[ptr] + stateLL[3] - aL[3] * out0;
				stateLL[3] = bL[4] * lower[ptr] 			 - aL[4] * out0;
				
			}

			//writer to delay lines
			upper[ptr] = pr + R0 * out0;
			{
				lower[ptr] = bRL[0] * outL + stateRL[0];
				stateRL[0] = bRL[1] * outL + stateRL[1] - aRL[1] * lower[ptr];
				stateRL[1] = bRL[2] * outL				- aRL[2] * lower[ptr];
			}

			//output buffers
			y0[n] = out0 + upper[ptr];
			{
				yL[n] = bTL[0] * outL + stateTL[0];
				stateTL[0] = bTL[1] * outL + stateTL[1] - aTL[1] * yL[n];
				stateTL[1] = bTL[2] * outL				- aTL[2] * yL[n];
			}

			ptr++;
			if (ptr >= N)
				ptr = 0;
		}
		

		float max = 0;
		for (int i = 0; i < smpCnt; i++)
			if(Math.abs(yL[i]) > max)
				max = Math.abs(yL[i]);
			
		Log.d(TAG, "max " + max);

		for (int i = 0; i < smpCnt; i++)
			yL[i] = yL[i] / max;
		
		short[]	buffer = new short[smpCnt];

		/*for (int i = 0; i < smpCnt; i++)
		{
			yL[i] = amplitude * (float) Math.sin(angle);
			angle += increment;
		}
		
		for (int i = 0; i < smpCnt; i++)
			buffer[i] = (short) (yL[i] * Short.MAX_VALUE);*/

		
		for (int i = 0; i < smpCnt; i++)
			buffer[i] = (short) (yL[i] * Short.MAX_VALUE);
		
	
		
		int written= mAudioOutput.write(buffer, 0, smpCnt);
		Log.d(TAG, "Written " + written);
		try
		{
			mAudioOutput.play();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed to start playback");
			return;
		}
	}

}