package ca.jvsh.audicy;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SineWaver extends Thread
{
	private final int			SAMPLING_RATE	= 44100;
	// Must be volatile:
	private volatile boolean	stop			= false;

	AudioTrack					track;
	short[]						buffer			= new short[1024];

	float						frequency		= 440.0f;
	float						amplitude		= 1.0f;
	float						increment;
	float						angle			= 0;
	float						samples[]		= new float[1024];

	public SineWaver()
	{
		increment = (float) (2.0 * Math.PI) * frequency / ((float) SAMPLING_RATE);
		int minSize = AudioTrack.getMinBufferSize(SAMPLING_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
	}

	public void freqAmp(float frequency, float amplitude)
	{

		this.frequency = frequency;
		this.amplitude = amplitude;
		// angular increment for each sample
		increment = (float) (2.0 * Math.PI) * frequency / ((float) SAMPLING_RATE);
	}

	public void run()
	{
		track.play();

		while (!stop)
		{

			for (int i = 0; i < samples.length; i++)
			{
				samples[i] = amplitude * (float) Math.sin(angle);
				angle += increment;
			}

			writeSamples(samples);
		}
	}

	public void writeSamples(float[] samples)
	{
		fillBuffer(samples);
		track.write(buffer, 0, samples.length);
	}

	private void fillBuffer(float[] samples)
	{
		if (buffer.length < samples.length)
			buffer = new short[samples.length];

		for (int i = 0; i < samples.length; i++)
			buffer[i] = (short) (samples[i] * Short.MAX_VALUE);
	}

	public synchronized void requestStop()
	{
		track.stop();
		stop = true;
	}

}
