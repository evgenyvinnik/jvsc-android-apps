package ca.jvsh.clarinet2;

import ca.jvsh.clarinet2.R;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

public class Clarinet2Activity extends Activity
{
	
	String TAG = "Clarinet";
	private AudioTrack			mAudioOutput;
	private int					mOutBufferSize;
	
	private int					mInBufferSize;
	private AudioRecord			mAudioInput;
	
	private int					mBufferSize;

	
	int fs = 44100;
	//int dur = 10;
	//int smpCnt = dur * fs;
	
	float						frequency		=440.0f;
	float						amplitude		=1.0f;
	float						increment;
	float						angle			= 0;
	short[]	buffer;	
	short[]	inputBuffer;
	
	private static boolean		mActive		= false;
	
	/////////////////////////////////////////////////////////////////////////
	//clarinet constants
	float c = 347.0f;
	float rho = 1.2f;

	float l = 0.52f;
	float a = 0.01f;
	float Z0 = rho * c / ((float) Math.PI * a * a);

	int ptr = 0;
	int N = (int) Math.floor(l * fs / c);
	float upper[] = new float[N];
	float lower[] = new float[N];
	
	float bRL[] = new float[] { -0.2162f, -0.2171f, -0.0545f };
	float aRL[] = new float[] { 1, -0.6032f, 0.0910f };

	float bTL[] = new float[] { -0.2162f + 1, -0.2171f + -0.6032f, -0.0545f + 0.0910f };
	float aTL[] = new float[] { 1, -0.6032f, 0.0910f };

	float stateRL[] = new float[2];

	float stateTL[] = new float[2];

	float bL[] = new float[] { 0.806451596106077f, -1.855863155228909f, 1.371191452991298f, -0.312274852426121f, -0.006883256612646f };
	float aL[] = new float[] { 1.000000000000000f, -2.392436499871960f, 1.891289981326362f, -0.511406512428537f, 0.015235504020645f };

	float R0 = 0.9f;

	float aw = 0.015f;
	float S = 0.034f * aw;
	float k = S * 10000000.0f;
	float H0 = 0.0007f;
	
	float max = 0;
	
	float y0[];
	float yL[];

	float U[];
	
	float stateLU[] = new float[4];
	float stateLL[] = new float[4];

	
	int nDecay = (int) (0.2 * fs);
	int nAttack = (int) (0.1 * fs);

	float multiplier = 5000.0f;
	private static final float	MAX_16_BIT	= 32768;

	/////////////////////////////////////////////////////////////////////////
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		increment = (float) (2.0 * Math.PI) * frequency / ((float) fs);

		mOutBufferSize = AudioTrack.getMinBufferSize(fs, AudioFormat.CHANNEL_CONFIGURATION_MONO,  AudioFormat.ENCODING_PCM_16BIT) ;
		mInBufferSize = AudioRecord.getMinBufferSize(fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

		
		mAudioOutput = new AudioTrack(AudioManager.STREAM_MUSIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * mOutBufferSize, AudioTrack.MODE_STREAM);
		mAudioInput = new AudioRecord(MediaRecorder.AudioSource.MIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * mInBufferSize);

		
		mBufferSize = (int)Math.min(mOutBufferSize, mInBufferSize);
		
		inputBuffer = new short[mBufferSize];
		buffer = new short[mBufferSize];
		y0 = new float[mBufferSize];
		yL = new float[mBufferSize];
		U = new float[mBufferSize];
		mActive = true;


		
		Thread clarinetThread = new Thread()
		{
			public void run()
			{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				try
				{
					mAudioOutput.play();
				}
				catch (Exception e)
				{
					Log.e(TAG, "Failed to start playback");
					return;
				}
				
				try
				{
					mAudioInput.startRecording();
				}
				catch (Exception e)
				{
					Log.e(TAG, "Failed to start recording");
					mAudioOutput.stop();
					return;
				}
				
				clarinetSound();
				//mAudioOutput.stop();
			}
		};
		
		clarinetThread.start();
		//clarinet();
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mActive = false;

		mAudioOutput.release();
		mAudioInput.release();
	}

	void clarinetSound()
	{
		float dp;
		float x;
		float pr;

		float outL;
		float out0;
		
		float y0_prev = 0;
		
		float pm = multiplier;
		float pm_prev = multiplier;
		float nu = .05f;
		
		try
		{
			
			while (mActive)
			{
				mAudioInput.read(inputBuffer, 0, mBufferSize);
				
				
				// We need longs to avoid running out of bits.
				float sum = 0;
				float sqsum = 0;
				for (int i = 0; i < mBufferSize; i++)
				{
					final long v = inputBuffer[i];
					sum += v;
					sqsum += v * v;
				}

				float power = (sqsum - sum * sum / mBufferSize) / mBufferSize;
				power /= MAX_16_BIT * MAX_16_BIT;
				
				for (int n = 0; n < mBufferSize; n++)
				{
					
					/*
					 envelope following
					 */
					
					pm = 10* power;// ((1 - nu) * Math.abs(inputBuffer[n]) + nu * pm_prev )/(float)(Short.MAX_VALUE);
					if(pm < 0.05f) pm = 0.1f;
					//else if(pm < 0.05f)  pm = 0.8f;
					else if(pm > 1.0f)  pm = 1.0f;
					//else  pm = 1.0f;
					//pm = 1.0f;
					//if(pm > 0)
					//	Log.d(TAG, "pm " + pm);
					
					pm_prev = pm;
					if(n == 0)
					{
						dp = (float) Math.abs( multiplier *pm - y0_prev);
					}
					else
					{
						dp = (float) Math.abs( multiplier * pm - y0[n - 1]);
					}
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
					
					if(Math.abs(yL[n]) > max)
						max = Math.abs(yL[n]);
				}
				
				y0_prev = y0[mBufferSize - 1];

				for (int i = 0; i < mBufferSize; i++)
					buffer[i] = (short) (yL[i] / max * Short.MAX_VALUE);
				
				/*int written=*/ mAudioOutput.write(buffer, 0, mBufferSize);
				//Log.d(TAG, "Written " + written);
			}

	
		}
		catch (Exception e)
		{
			Log.d(TAG, "Error while recording, aborting.");
		}

		try
		{
			mAudioOutput.stop();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Can't stop playback");
			try
			{
				mAudioInput.stop();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Can't stop recording");
				return;
			}
			return;
		}
		
		try
		{
			mAudioInput.stop();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Can't stop recording");
			return;
		}

	}

}