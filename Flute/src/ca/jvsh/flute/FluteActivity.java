package ca.jvsh.flute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ca.jvsh.flute.R;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

public class FluteActivity extends Activity
{

	String						TAG		= "Flute";
	private AudioTrack			mAudioOutput;
	private int					mOutBufferSize;
	
	private int					mInBufferSize;
	private AudioRecord			mAudioInput;
	
	private int					mBufferSize;
	short[]	buffer;	
	short[]	inputBuffer;

	private static boolean		mActive		= false;
	
	int							fs		= 44100;

	private final static Random	rand	= new Random();

	//inputs
	int							length	= 80;
	float						vents[]	= new float[] { 40.3f, 60.1f };
	int							degree	= 2;
	int NRofVents = vents.length;	


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mOutBufferSize = AudioTrack.getMinBufferSize(fs, AudioFormat.CHANNEL_CONFIGURATION_MONO,  AudioFormat.ENCODING_PCM_16BIT) ;
		mInBufferSize = AudioRecord.getMinBufferSize(fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

		
		mAudioOutput = new AudioTrack(AudioManager.STREAM_MUSIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * mOutBufferSize, AudioTrack.MODE_STREAM);
		mAudioInput = new AudioRecord(MediaRecorder.AudioSource.MIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * mInBufferSize);
		
		mBufferSize = (int)Math.min(mOutBufferSize, mInBufferSize);
		inputBuffer = new short[mBufferSize];
		buffer = new short[mBufferSize];

		
		mActive = true;

		Thread fluteThread = new Thread()
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
				
				
				flute();
			}
		};

		fluteThread.start();
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mActive = false;

		mAudioOutput.release();
		mAudioInput.release();
	}
	
	void flute()
	{
		//resolve delay line lengths
		int fluteLength = length;

		int jetLength1 = 2 * fluteLength;

		int jetLength2 = 2 * (int) Math.round(vents[0]);

		int jetLength = jetLength1;
		int jetMaxLength = (int) Math.max(jetLength1, jetLength2);

		//constants
		float ventStep = 1;

		//filter parameters
		float RB0 = -0.4794f; // Reflection filter coefficients
		float RB1 = -0.2603f;
		float DCA = 0.9950f; // DC killer (high-pass filter) coefficient

		int degree = 2;


		float[][] coeffs = new float[Math.max(1, NRofVents)][fluteLength];

		for (int i = 0; i < NRofVents; i++)
		{
			coeffs[i] = fluteInterp(fluteLength, degree, vents[i]);
		}

		//finger hole filter parameters
		float AR = -0.8632f;
		float BR = -(1 + AR);
		float ARI = 0;
		float BRI = 0;

		float ventTarget = 0;
		float ventState = 0;

		//compute vent operation times
		//int openVentHere = Math.round(1 * samples / 3);
		//int closeVentHere = Math.round(2 * samples / 3);

		//variable parameters of the flute model

		//frequency dependent loss filter
		float lossFreq = (float) Math.exp(0.006f * fluteLength) - 0.85f;
		float lossGain = 0.992f - 0.0005f * fluteLength;
		float lossScale = 1 - lossFreq;

		float backGain = -0.97f; // Real reflection coefficient for the mouth end
		float noiseGain = 4.0f; // Gain of the white noise signal
		float inputGain = 100.0f; // Maximum amplitude of input
		float voicedGain = 0.02f; // Gain of the feedback from the tube to the nonlin
		float dirFeedback = -0.1f; // Gain from the nonlinearity back to itself
		float sigMin = 0.001f; // Input gain of the sigmoid function
		float sigmOffset = 0.0f; // Offset of the sigmoid function
		float sigmOut = 300.0f; // Output gain of the sigmoid function

		//Coefficient of the leaky integrator (if 0.0, integrator is removed)
		float integra = 0.0f;

		/*------------------------------------------------------------------------------
		% Init delay lines
		%
		% 27.11.1995 Rami Hanninen
		%
		% Replaced the traditional ring-buffer delay line implementation with a more
		% direct vector approach.  Even if the ring-buffered implementation is usually
		% the most efficient method to implement a delay line, this is not the case
		% with MatLab because executing for-loops and mod-operations in MatLab is much
		% slower than the fast array operations MatLab offers.
		%
		% In the new implementation, the newest element in the delay line is LINE(1)
		% and the oldest is LINE(LINELENGTH), exept with the lower delay line which is
		% reversed so that the newest element is LOWER(LENGTH) and the oldest is
		% LOWER(1).
		%
		*/
		float[] jet = new float[jetMaxLength]; // Delay line for the air jet
		float[] upper = new float[fluteLength]; // Upper trail of the digital waveguide
		float[] lower = new float[fluteLength]; // Lower trail of the digital waveguide

		
		//Allocate memory for output vectors

		float max = 0;
		float[] uppOut = new float[mBufferSize];
		//float[] lowOut = new float[mBufferSize];
		//float[] jetOut = new float[mBufferSize];
		//float[] sigOut = new float[mBufferSize];
		//float[][] venOut = new float[NRofVents][mBufferSize];
		//float[] staOut = new float[mBufferSize];

		//float[] pressure = new float[samples];
		//Input of the flute model
		//for (int i = 0; i < samples; i++)
		//	pressure[i] = noiseGain * 2 * (rand.nextFloat() - 0.5f);
		
		

		//int nAttack = 2000;
		//int nDecay = 5000;
		/*float[] inputAmpl = new float[samples];
		

		int m;
		for (m = 0; m < nAttack; m++)
			inputAmpl[m] = inputGain * m / nAttack;

		for (; m < samples - nAttack - nDecay; m++)
			inputAmpl[m] = inputGain;

		for (int j = 0; m < samples; m++, j++)
			inputAmpl[m] = inputGain * j / nDecay;*/

		// State variables

		float lossInput = 0.0f; //Input for the freq-dependent loss filter
		float reflDelayX = 0.0f; //First unit delay of the reflection function
		float reflDelayY = 0.0f; //Second unit delay of the reflection function
		float sigmInput = 0.0f; //Input of the sigmoid function
		float sigmOutput = 0.0f; //Output of the sigmoid function
		float dcxOutput0 = 0.0f; //output of the DC killer filter
		float dcxOutput1 = 0.0f; //Last output of the DC killer filter
		float integrInput = 0.0f; //Input of the leaky integrator
		float integrOutput = 0.0f; //Output of the integrator
		float[] ventOutput = new float[NRofVents]; //3-port vent output	
		float[] ventOutputPrev = new float[NRofVents]; //3-port vent output	

		float[] deinter = new float[fluteLength];
		float temp;

		try
		{
			while (mActive)
			{
				mAudioInput.read(inputBuffer, 0, mBufferSize);
				
				//y0_prev = y0[mBufferSize - 1];
				for (int n = 0; n < mBufferSize; n++)
				{
					ARI = ventState * AR;
					BRI = ventState * BR;

					//feed jet line
					jet[0] = inputGain * (noiseGain * 2 * (rand.nextFloat() - 0.5f) + integrOutput);

					//sigmoid nonlinearity
					sigmOutput = sigmOut * (float) Math.tanh(inputGain * sigmOffset - sigMin * jet[jetLength - 1]);

					// DC killer (a 1st order high-pass filter, direct form II)
					temp = sigmOutput + DCA * dcxOutput1;
					dcxOutput0 = temp - dcxOutput1; //differentiation removes DC component
					dcxOutput1 = temp;

					// Add (subtract) the output of the sigmoid function (DC killed) and the
					// reflected signal from the end of the lower delay line and feed the
					// result into the beginning of the upper delay line.

					lossInput = dcxOutput0 + backGain * lower[0];

					// Boundary losses (1st order all-pole low-pass filter):
					// Note that because the delay line have been shifted when we come
					// here the next time, the previous output is now in UPPER(2),
					// not in UPPER(1) as one might think.

					upper[0] = lossScale * lossGain * lossInput + lossFreq * upper[1];

					// FRACTIONAL DELAY 3-PORT:
					// COEFFS  = Lagrange interpolator coefficients
					// DEILINE = delay line (state) of the deinterpolator
					// BR & AR = coefficients of the reflection filter

					/*
					%
					% Because our delay lines are now ordinary vectors, the 3-port computation
					% becomes a very simple matrix operation (the old implementation that used
					% ring buffered delay lines took over 20 lines of code and included two
					% slow for-loops).
					%
					% The input interpolation is computed like this:
					%
					% VENTINPUT = (UPPER + LOWER) * COEFF
					%
					% UPPER:      1 x LENGTH vector     Upper delay line
					% LOWER:      1 x LENGTH vector     Lower delay line (reversed)
					% COEFF:      LENGTH x VENTS matrix Lagrangian interpolation coefficients
					% VENTINPUT : 1 x VENTS vector      Input for N vents
					%
					% Note, that this implementation allows us to compute the input for multiple
					% 3-port vents in just a single operation.
					%
					% The output of the 3-port is computed like this
					%
					% VENTOUTPUT = BR * VENTINPUT - AR * VENTOUTPUT
					%
					% BR: scalar
					% AR: scalar
					% VENTOUTPUT: 1 x VENTS vector
					%
					% And again, note this allows the simultanious computation of several vents.
					% Note also that VENTOUTPUT holds the previous vent outputs
					%
					% When combined, we finally get (ta-daa) the following:
					*/

					/*for (int k = 0; k < NRofVents; k++)
						ventOutput[k] = 0;

					for (int k = 0; k < NRofVents; k++)
						for (int j = 0; j < fluteLength; j++)
						{
							ventOutput[k] += (upper[j] + lower[j]) * coeffs[k][j];
						}
						
					for (int k = 0; k < NRofVents; k++)
					{
						ventOutput[k] = BRI * ventOutput[k] - ARI * ventOutputPrev[k];
						ventOutputPrev[k] = ventOutput[k];
					}
					



					//ventOutput  = BRI * ((upper + lower) * coeffs) - ARI * ventOutput;

					// Deinterpolation reverses the process
					for (int j = 0; j < fluteLength; j++)
					{
						deinter[j] = 0;
						for (int k = 0; k < NRofVents; k++)
						{
							deinter[j] +=  ventOutput[k] * coeffs[k][j];
						}
					}

					for (int j = 0; j < fluteLength; j++)
					{
						upper[j] = upper[j] + deinter[j];
						lower[j] = lower[j] + deinter[j];
					}*/
					// Reflection filter

					reflDelayY = RB0 * upper[fluteLength - 1] + RB1 * (reflDelayX - reflDelayY);
					reflDelayX = upper[fluteLength - 1]; // Update unit delay of the reflection filter

					// Feed reflected signal into the beginning of the lower delay line

					lower[fluteLength - 1] = reflDelayY;

					// Feedback loop:

					integrInput = voicedGain * (lower[0] + dirFeedback * dcxOutput0);

					// Integration:

					integrOutput = (1 - integra) * integrInput + integra * integrOutput;

					// Output of the model:
					// Subtract output of refl. filter from its input (and differentiate):

					uppOut[n] = upper[fluteLength - 1];

					if(Math.abs(uppOut[n]) > max)
						max = Math.abs(uppOut[n]);
					//lowOut[i] = lower[0];
					//jetOut[i] = jet[jetLength - 1];
					//sigOut[i] = upper[fluteLength - 1] - reflDelayY;

					//for (int k = 0; k < NRofVents; k++)
					//	venOut[k][i] = ventOutput[k];
					//staOut[i] = ventState;

					//SIGOUT(1,I) = SIGMOUTPUT;
					//DCXOUT(1,I) = DCXOUTPUT0;

					// Move delay lines
					for (int j = jetMaxLength-1; j >0; j--)
						jet[j] = jet[j - 1];
					jet[0] = 0;
					
					for (int j = fluteLength-1; j >0; j--)
						upper[j] = upper[j - 1];
					upper[0] = 0;

					for (int j = 0; j < fluteLength - 1; j++)
						lower[j] = lower[j + 1];
					lower[fluteLength - 1] = 0;
				}

				for (int i = 0; i < mBufferSize; i++)
					buffer[i] = (short) (uppOut[i] / max * Short.MAX_VALUE);
				
				mAudioOutput.write(buffer, 0, mBufferSize);
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
		
		
		//for (int i = 0; i < samples; i++)
		//{
			/* Open and close the vent. Note that we also adjust the jet line length
			% by moving the place from which samples are taken from the jetline.
			%
			% VENTSTATE:  Current vent openess value
			% VENTTARGET: Desired vent openess value
			*/
			/*if (i == openVentHere)
			{
				jetLength = jetLength2;
				ventTarget = 1;
			}
			else if (i == closeVentHere)
			{
				jetLength = jetLength1;
				ventTarget = 0;
			}*/

			//open vent
			//if (ventTarget > ventState)
			//	ventState = Math.min(ventTarget, ventState + ventStep);
			//close vent
			//else if (ventTarget < ventState)
			//	ventState = Math.max(ventTarget, ventState - ventStep);

			/*			
			% Below is an exponential vent regulator, but it does not function well
			% when the vent is closing.
			%
			% VENTSTATE = VENTSTATE + (VENTTARGET - VENTSTATE) / 10.0;
			*/
			

		//}

	}

	float[] fluteInterp(int delayLineLength, int interpolatorDegree, float fractionalDelay)
	{
		int intDelay = (int) Math.floor(fractionalDelay);
		float fraction = fractionalDelay - intDelay;

		// If the interpolator degree is even and the fractional part is greater than
		// 0.5, we must move the interpolator one step forward because even Langrange
		// interpolators work best when the fractional part is between [-0.5 0.5].
		if (interpolatorDegree / 2.0f == Math.round(interpolatorDegree / 2.0f) && fraction > 0.5f)
		{
			fraction--;
			intDelay++;
		}

		//Compute Langrange coefficients
		float[] lagrange = hlagr(interpolatorDegree + 1, fraction);

		//Construct coefficient vector that can be multiplied with a delay line vector
		int begin = intDelay - (int) Math.floor((float) interpolatorDegree / 2.0f);
		int end = begin + lagrange.length;

		float[] line = new float[delayLineLength];

		if (begin < 0 || end >= delayLineLength)
			return line;

		for (int i = 0; i < lagrange.length; i++)
			line[begin + i] = lagrange[i];

		return line;
	}

	float[] hlagr(int filterLength, float fractionalDelay)
	{
		int N = filterLength - 1; // filter order
		float M = (float) N / 2.0f; // middle value

		float D;
		// integer part closest to middle
		if ((M - Math.round(M)) == 0)
			D = fractionalDelay + M;
		else
			D = fractionalDelay + M - 0.5f;

		float[] h = new float[N + 1];
		for (int i = 0; i < N + 1; i++)
			h[i] = 1;

		for (int n = 0; n < N + 1; n++)
		{
			for (int k = 0; k < N + 1; k++)
			{
				if (k != n)
					h[n] = h[n] * (float) (D - k) / (float) (n - k);
			}
		}

		return h;
	}

}