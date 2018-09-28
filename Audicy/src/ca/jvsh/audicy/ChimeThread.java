package ca.jvsh.audicy;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class ChimeThread extends Thread
{
	//////////////////////////////////////////////////////////////////
	//General Audio parameters
	//////////////////////////////////////////////////////////////////

	// sampling frequency (Hz)
	public static final int		fs			= 22050;

	public final int			BUFFER_SIZE;

	public final float			TEMPO		= 130.0f;

	///////////////////////////////////////////////////////////////////
	//thread control variable
	///////////////////////////////////////////////////////////////////
	// Must be volatile:
	private volatile boolean	stop		= false;

	//////////////////////////////////////////////////////////////////
	//audio track parameters
	//////////////////////////////////////////////////////////////////
	private AudioTrack			track;
	private short				mBuffer[];

	private float[]				soundBuffer;

	float						position;

	ArrayList<Tone>				tones		= new ArrayList<Tone>();
	private final ReentrantLock	toneLock	= new ReentrantLock();

	boolean						play		= true;

	boolean						flagRemove	= false;
	int							length;

	public ChimeThread()
	{
		//audio track init
		{
			//choose buffer to be twice as bigger as a minimum size
			BUFFER_SIZE = AudioTrack.getMinBufferSize(fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

			//track = new AudioTrack(AudioManager.STREAM_MUSIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, BufferSize, AudioTrack.MODE_STREAM);
			track = new AudioTrack(AudioManager.STREAM_MUSIC, fs, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE, AudioTrack.MODE_STREAM);

			//allocate buffer for audio track
			soundBuffer = new float[BUFFER_SIZE];
			mBuffer = new short[BUFFER_SIZE];
		}
	}

	public void addTone(int note)
	{
		toneLock.lock();
		tones.add(new Tone(fs, BUFFER_SIZE, note));
		toneLock.unlock();
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

		//do audio processing
		if (tones.isEmpty())
		{
			for (int i = 0; i < BUFFER_SIZE; i++)
				mBuffer[i] = 0;
		}
		else
		{
			for (int i = 0; i < BUFFER_SIZE; i++)
			{
				soundBuffer[i] = 0.0f;
			}
			//process audio
			toneLock.lock();
			for (Tone tone : tones)
			{
				tone.processAudio(soundBuffer);
			}

			do
			{
				flagRemove = false;
				length = tones.size();
				for (int i = 0; i < length; i++)
				{
					if (tones.get(i).end)
					{
						tones.remove(i);
						flagRemove = true;
						break;
					}
				}
			}
			while (flagRemove);
			toneLock.unlock();

			for (int i = 0; i < BUFFER_SIZE; i++)
				mBuffer[i] = (short) ((soundBuffer[i]) * Short.MAX_VALUE);
		}

		track.write(mBuffer, 0, BUFFER_SIZE);
	}

	public synchronized void requestStop()
	{
		track.stop();
		stop = true;
	}

	public int positionToNumSamples(float position)
	{
		return (int) ((position * 240 * fs) / TEMPO + .5);
	}
}
