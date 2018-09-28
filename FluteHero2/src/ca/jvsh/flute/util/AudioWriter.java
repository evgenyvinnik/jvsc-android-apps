package ca.jvsh.flute.util;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;
import android.media.AudioManager;

public class AudioWriter
{

	/**
	 * Listener for audio writes.
	 */
	public static abstract class WriteListener
	{
		/**
		 * Audio write error code: no error.
		 */
		public static final int	ERR_OK			= 0;

		/**
		 * Audio write error code: the audio write failed to initialise.
		 */
		public static final int	ERR_INIT_FAILED	= 1;

		/**
		 * Audio write error code: an audio write failed.
		 */
		public static final int	ERR_READ_FAILED	= 2;

		/**
		 * An audio write has completed.
		 */
		public abstract void onWriteComplete(/*short[] buffer*/);

		/**
		 * An error has occurred.  The writer has been terminated.
		 * @param   error       ERR_XXX code describing the error.
		 */
		public abstract void onWriteError(int error);
	}

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create an AudioWriter instance.
	 */
	public AudioWriter()
	{

	}

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	/**
	 * Start this writer.
	 * 
	 * @param   rate        The audio sampling rate, in samples / sec.
	 * @param   block       Number of samples of output to write at a time.
	 *                      This is different from the system audio
	 *                      buffer size.
	 * @param   listener    Listener to be notified on each completed write.
	 */
	public void startWriter(int rate, int block, WriteListener listener)
	{
		Log.i(TAG, "Writer: Start Thread");
		synchronized (this)
		{
			// Calculate the required I/O buffer size.
			int audioBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4;
			Log.i(TAG, "Writer audiobuf" + audioBuf);
			// Set up the audio input.
			audioOutput = new AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, audioBuf, AudioTrack.MODE_STREAM);
			outputBlockSize = block;
			sleepTime = (long) (1000f / ((float) rate / (float) block));
			outputBuffer = new short[2][outputBlockSize];
			outputBufferWhich = 0;
			outputBufferIndex = 0;
			outputListener = listener;
			running = true;
			writerThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					writerRun();
				}
			}, "Audio Writer");
			writerThread.start();
		}
	}

	/**
	 * Stop this reader.
	 */
	public void stopWriter()
	{
		Log.i(TAG, "Reader: Signal Stop");
		synchronized (this)
		{
			running = false;
		}
		try
		{
			if (writerThread != null)
				writerThread.join();
		}
		catch (InterruptedException e)
		{
			;
		}
		writerThread = null;

		// Kill the audio output.
		synchronized (this)
		{
			if (audioOutput != null)
			{
				audioOutput.release();
				audioOutput = null;
			}
		}

		Log.i(TAG, "Reader: Thread Stopped");
	}

	// ******************************************************************** //
	// Main Loop.
	// ******************************************************************** //

	/**
	 * Main loop of the audio reader.  This runs in our own thread.
	 */
	private void writerRun()
	{
		short[] buffer;
		int index, writeSize;

		int timeout = 200;
		try
		{
			while (timeout > 0 && audioOutput.getState() != AudioTrack.STATE_INITIALIZED)
			{
				Thread.sleep(50);
				timeout -= 50;
			}
		}
		catch (InterruptedException e)
		{
		}

		if (audioOutput.getState() != AudioTrack.STATE_INITIALIZED)
		{
			Log.e(TAG, "Audio writer failed to initialize");
			writeError(WriteListener.ERR_INIT_FAILED);
			running = false;
			return;
		}

		try
		{
			Log.i(TAG, "Writer: Start Writing");
			audioOutput.play();
			while (running)
			{
				long stime = System.currentTimeMillis();

				if (!running)
					break;

				writeSize = outputBlockSize;
				int space = outputBlockSize - outputBufferIndex;
				if (writeSize > space)
					writeSize = space;
				buffer = outputBuffer[outputBufferWhich];
				index = outputBufferIndex;

				synchronized (buffer)
				{
					int nwrite = audioOutput.write(buffer, index, writeSize);

					boolean done = false;
					if (!running)
						break;

					if (nwrite < 0)
					{
						Log.e(TAG, "Audio write failed: error " + nwrite);
						writeError(WriteListener.ERR_READ_FAILED);
						running = false;
						break;
					}
					int end = outputBufferIndex + nwrite;
					if (end >= outputBlockSize)
					{
						outputBufferWhich = (outputBufferWhich + 1) % 2;
						outputBufferIndex = 0;
						done = true;
					}
					else
						outputBufferIndex = end;

					if (done)
					{
						writeDone();

						// Because our block size is way smaller than the audio
						// buffer, we get blocks in bursts, which messes up
						// the audio analyzer.  We don't want to be forced to
						// wait until the analysis is done, because if
						// the analysis is slow, lag will build up.  Instead
						// wait, but with a timeout which lets us keep the
						// input serviced.
						long etime = System.currentTimeMillis();
						long sleep = sleepTime - (etime - stime);
						if (sleep < 5)
							sleep = 5;
						try
						{
							buffer.wait(sleep);
						}
						catch (InterruptedException e)
						{
						}
					}
				}
			}
		}
		finally
		{

			Log.i(TAG, "Writer: Stop Writing");
			if (audioOutput.getState() == AudioTrack.PLAYSTATE_PLAYING)
				audioOutput.stop();
		}
	}

	/**
	 * Notify the client that a write has completed.
	 * 
	 */
	private void writeDone()
	{
		outputListener.onWriteComplete();
	}
	
	public void writeAudio(short[] buffer)
	{
		// Lock to protect updates to these local variables.  See run().
		synchronized (this)
		{
			//outputBuffer[outputBufferWhich] = buffer;
			//++outputBufferIndex;
		}
	}

	/**
	 * Notify the client that an error has occurred.  The reader has been
	 * terminated.
	 * 
	 * @param   error       ERR_XXX code describing the error.
	 */
	private void writeError(int code)
	{
		outputListener.onWriteError(code);
	}

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	private static final String	TAG					= "AudioWriter";

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Our audio output device.
	private AudioTrack			audioOutput;

	// Our audio input buffer, and the index of the next item to go in.
	private short[][]			outputBuffer		= null;
	private int					outputBufferWhich	= 0;
	private int					outputBufferIndex	= 0;

	// Size of the block to write each time.
	private int					outputBlockSize		= 0;

	// Time in ms to sleep between blocks, to meter the supply rate.
	private long				sleepTime			= 0;

	// Listener for output.
	private WriteListener		outputListener		= null;

	// Flag whether the thread should be running.
	private boolean				running				= false;

	// The thread, if any, which is currently writing.  Null if not running.
	private Thread				writerThread		= null;

}
