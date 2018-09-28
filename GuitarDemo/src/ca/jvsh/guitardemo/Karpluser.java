package ca.jvsh.guitardemo;

import java.util.Random;

import java.util.concurrent.locks.ReentrantLock;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


public class Karpluser extends Thread
{
	//////////////////////////////////////////////////////////////////
	//General Audio parameters
	//////////////////////////////////////////////////////////////////

	// sampling frequency (Hz)
	private final int			fs				= 22050;
	// sampling period
	private final float			T				= 1.0f / ((float) fs);

	// length and  number of samples to compute before starting output 0;
	//calculating 9 seconds, then just output zeros
	private float				signalt			= 2;
	public int					M				= (int) (signalt * fs);

	///////////////////////////////////////////////////////////////////
	//thread control variable
	///////////////////////////////////////////////////////////////////
	// Must be volatile:
	private volatile boolean	stop			= false;

	//////////////////////////////////////////////////////////////////
	//audio track parameters
	//////////////////////////////////////////////////////////////////
	private AudioTrack			track;
	private short				mBuffer[];
	private int					BufferSize;

	//////////////////////////////////////////////////////////////
	//extended Karplus-Strong algorithm parameters
	//////////////////////////////////////////////////////////////
	// initial feedback gain
	float						fbg_init[]		= { 0.002f, 0.00f };
	// slope of the feedback gain ramp
	float						fbg_slope[]		= { 0.006f, 0.00f };

	float						fbg_max[]		= { 0.008f, 0.00f };
	//frequency to generate and play
	private float				fo;

	///////////////////////////////////////////////////////////////////
	//General Karplus-Strong Algorithm Parameters
	//////////////////////////////////////////////////////////////////

	// Coefficient of the low-pass filter
	float						lpcoeff			= 0.5f;

	// blend factor - setting to 1 gives string simulation,
	// otherwise it gives a drum simulator
	//float b = 1.0f;
	int							bufsz			= 1000;

	// delay line buffer - max delay=bufsz - corresponds to a
	// frequency of 44.1Hz
	float						buffer[]		= new float[bufsz];

	// feedback delay line buffer - same conditions as buffer
	float						fbbuffer[][]	= new float[2][bufsz];

	///////////////////////////////////////////////////////////////////
	//Frequency dependent parameters
	//////////////////////////////////////////////////////////////////////

	////frequencies favoured for feedback (Hz)
	float						ffb[]			= new float[2];

	// cutoff frequency for DC blocking filter
	float						fco;

	// delay length
	int							N;
	// fraction of the way between sample n and n+1 that we want
	float						eta;

	float						a;

	//////////////////////////////////////////////////////////////
	// feedback loop parameters
	//////////////////////////////////////////////////////////////

	// delay length
	int							Nfb[]			= new int[2];
	float						etafb[]			= new float[2];
	float						afb[]			= new float[2];

	////////////////////////////////////////////////////////////////
	//initialize clipping lookup table
	////////////////////////////////////////////////////////////////

	// the function will be defined from -2 to +2
	float						maxsig			= 2;

	// lookup table step size
	float						ltstep			= 0.0001f;

	// define the lowest input value possible for the lookup gain
	float						inlow			= -2.0f;

	// now define a lookup table with our distortion function
	// defined above
	float[]						lookup			= new float[40000];

	/////////////////////////////////////////////////////////////////////
	// DC Blocking Filter Parameters
	////////////////////////////////////////////////////////////////////
	float						wco;

	// DC blocking filter coeff's - note that
	float						bDC[]			= new float[2];

	// a's, b's as well as sign convention are
	// reversed in the Sullivan paper!
	float						aDC[]			= new float[2];

	///////////////////////////////////////////////////////////////////////
	//Pointer and delay memory initializations
	///////////////////////////////////////////////////////////////////////

	// output pointer for KS delay line 'buffer'
	int							outptr;

	// input pointer for KS buffer
	int							inptr;

	// buffer(outptr-1)
	float						bufoptrm1;

	float						ykm1;

	// single delay for low-pass filter in KS algorithm
	float						blendm1;

	// for DC blocking filter - single delayed output
	float						yDC;
	// for DC blocking filter - single delayed input
	float						xDC;

	// ouput pointer for feedback delay line 'fbbuffer'
	int[]						fboutptr		= new int[2];

	// input pointer for feedback buffer
	int[]						fbinptr			= new int[2];

	// fbbuffer(fboutptr-1)
	float[]						fbbufm1			= new float[2];
	// fb delay line output 1 sample ago
	float[]						fbkm1			= new float[2];

	/////////////////////////////////////////////////////////////////////
	//random data
	////////////////////////////////////////////////////////////////////
	float						meanrand;
	float						initrand[];

	///////////////////////////////////////////////////////////////////
	//for main cycle
	///////////////////////////////////////////////////////////////////

	// initialize 1 string
	float						str;

	//main filling counter
	int							k				= 0;

	//flag that determine guitar type
	boolean electric = true;
	
	float						temp;

	float						fbtemp[]		= new float[2];
	float						tempDC;
	float						y;
	private static final Random	random			= new Random();

	private final ReentrantLock	l				= new ReentrantLock();

	public Karpluser()
	{

		//choose buffer to be twice as bigger as a minimum size
		BufferSize = AudioTrack.getMinBufferSize(fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		track = new AudioTrack(AudioManager.STREAM_MUSIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, BufferSize, AudioTrack.MODE_STREAM);

		//allocate buffer for audio track
		mBuffer = new short[BufferSize];

		//initialization of frequency independent parameters is done above

		//init lookup table

		for (int lookup_counter = 0; lookup_counter < 10000; lookup_counter++)
			lookup[lookup_counter] = -0.66666667f;

		for (int lookup_counter = 0; lookup_counter < 20000; lookup_counter++)
		{
			lookup[lookup_counter + 10000] = (-1.0f + lookup_counter * ltstep);
			// main part of soft clipping function: y=x-(x^3)/3
			lookup[lookup_counter + 10000] = lookup[lookup_counter + 10000] - (lookup[lookup_counter + 10000] * lookup[lookup_counter + 10000] * lookup[lookup_counter + 10000]) / 3.0f;
		}
		for (int lookup_counter = 0; lookup_counter < 10000; lookup_counter++)
			lookup[lookup_counter + 30000] = 0.66666667f;

	}

	/**
	 * Function that initializes frequency dependent parameters
	 */
	public void setFrequency(float freq)
	{
		l.lock();

		fo = freq;

		//frequencies favoured for feedback (Hz)
		ffb[0] = 3.0f * fo;
		ffb[1] = fo;

		// cutoff frequency for DC blocking filter
		fco = fo / 100.0f;

		// delay length

		N = (int) Math.floor(fs / fo);
		// fraction of the way between sample n and n+1 that we want
		eta = (fs / fo) - N;

		a = (1.0f - eta) / (1.0f + eta);
		for (int j = 0; j < bufsz; j++)
		{
			buffer[j] = 0;
		}

		// feedback loop parameters
		// delay length
		for (int i = 0; i < 2; i++)
		{

			for (int j = 0; j < bufsz; j++)
				fbbuffer[i][j] = 0;

			Nfb[i] = (int) Math.floor(fs / ffb[i]);
			etafb[i] = (fs / ffb[i]) - (float) (Nfb[i]);

			afb[i] = (1.0f - etafb[i]) / (1.0f + etafb[i]);

			// input pointer for feedback buffer

			fbinptr[i] = Nfb[i];
			fbbufm1[i] = 0;
			fbkm1[i] = 0;
			fboutptr[i] = 1;
		}

		// DC Blocking Filter Parameters
		wco = (float) (2.0f * Math.PI * fco / fs);
		bDC[0] = 1.0f / (1.0f + 0.5f * wco);
		bDC[1] = -bDC[0];

		aDC[0] = 1;
		aDC[1] = -bDC[0] * (1.0f - 0.5f * wco);

		//Pointer and delay memory initializations

		outptr = 1;
		inptr = N + 1;
		bufoptrm1 = buffer[0];
		ykm1 = 0;

		////////////////////////////////////////////////////////////////////
		//calculate random data
		////////////////////////////////////////////////////////////////////

		meanrand = 0;
		initrand = new float[N];
		for (int i = 0; i < N; i++)
		{
			// binary random data
			if (random.nextFloat() >= 0.5f)
				initrand[i] = 0.99f;
			else
				initrand[i] = -0.99f;

			meanrand += initrand[i];
		}
		meanrand = meanrand / ((float) N);

		// inject random data into the buffer
		for (int i = 0; i < N; i++)
		{
			buffer[i] = initrand[i] - meanrand;
		}

		//set main counter back to zero
		k = 0;

		//set other counters to zero
		blendm1 = 0;
		yDC = 0;
		xDC = 0;
		temp = 0;
		tempDC = 0;

		fbtemp[0] = 0;
		fbtemp[1] = 0;
		y = 0;

		l.unlock();
	}

	public void run()
	{
		track.play();

		while (!stop)
		{
			writeSamples();
		}
	}

	public void writeSamples()
	{
		l.lock();
		if (k < M)
		{
			for (int i = 0; i < BufferSize; i++)
			{

				// read interpolated delay line output
				str = a * (buffer[outptr] - ykm1) + bufoptrm1;

				// These will be str(k-1) once we loop
				ykm1 = str;

				// These will be x(k-1) once we loop
				bufoptrm1 = buffer[outptr];

				// ------------- Low-Pass Filter --------------------

				// Pre-gain must be sufficiently high.
				temp = str;

				str = lpcoeff * (str + blendm1);

				// these will be str(k-1) once we loop
				blendm1 = temp;
				for (int j = 0; j < 2; j++)
				{
					// all-pass interpolated delay line
					fbtemp[j] = afb[j] * (fbbuffer[j][fboutptr[j]] - fbkm1[j]) + fbbufm1[j];

					// This will be the output of the delay line 1 sample ago
					// once we loop
					fbkm1[j] = fbtemp[j];
					// This will be fbbuffer(fboutptr-1) once we loop
					fbbufm1[j] = fbbuffer[j][fboutptr[j]];
				}

				// inject the feedback signal back into the KS loops
				str = str + 0.5f * (fbtemp[0] + fbtemp[1]);

				//------------------------DC blocking filter -----------------------------
				tempDC = bDC[0] * str + bDC[1] * xDC - aDC[1] * yDC;
				// /will be str(k-1) once we loop
				yDC = tempDC;
				// will be x(k-1) once we loop
				xDC = str;

				str = tempDC;

				// split the signal into the output and beginning of the
				// delay line
				buffer[inptr] = str;

				y = str;
				if(electric)
				{
					//apply preamp gain 
					if (k > (signalt -  1.5) * fs)
						y *= (float) (6.0f * Math.exp(-((k - (signalt - 1.5) * fs) * T) / 2.0f));
					else
						y *= 6.0f;
	
					//--------------------------- Distortion Module --------------------
					// apply the lookup table to the output to give it distortion
					if (Math.abs(y) > 1.5f)
						y = Math.signum(y) * 0.66666667f;
					else
						y = lookup[(int) Math.round(((y - inlow) / ltstep)) + 1];
				}
				
				// ------------------------pointer  increments----------------------------
				inptr++;
				outptr++;

				for (int n = 0; n < 2; n++)
				{
					// put the output into the feedback delay line
					if (k < 1 * fs)
					{
						fbbuffer[n][fbinptr[n]] = fbg_init[n] + fbg_slope[n] / 9.0f * k * T * y;
					}
					else
					{
						fbbuffer[n][fbinptr[n]] = fbg_max[n];
					}

					fbinptr[n]++;
					fboutptr[n]++;

					// put the pointer back to the start of the delay line
					if (fbinptr[n] >= bufsz)
						fbinptr[n] = 0;

					// put the pointer back to the start of the delay line
					if (fboutptr[n] >= bufsz)
						fboutptr[n] = 0;

				}

				// put the pointer back to the start of the delay line
				if (outptr >= bufsz)
					outptr = 0;

				// put the pointer back to the start of the delay line
				if (inptr >= bufsz)
					inptr = 0;

				//set up the output buffer
				mBuffer[i] = (short) (y * Short.MAX_VALUE);

				//increment main pointer
				k++;
			}
		}
		else
		{
			for (int i = 0; i < BufferSize; i++)
				mBuffer[i] = 0;
		}
		l.unlock();
		track.write(mBuffer, 0, BufferSize);
	}

	public synchronized void requestStop()
	{
		track.stop();
		stop = true;
	}

	public void setK(int k)
	{
		l.lock();
		this.k = k;
		l.unlock();
	}

	public void setGuitarType(boolean electric)
	{
		l.lock();
		this.electric = electric;
		l.unlock();
	}

}
