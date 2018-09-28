/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package ca.jvsh.audalizer;

import ca.jvsh.audalizer.SurfaceRunner;
import ca.jvsh.audalizer.AudioReader;

import ca.jvsh.audalizer.SignalPower;

import android.os.Bundle;

/**
 * An {@link Instrument} which analyses an audio stream in various ways.
 * 
 * <p>To use this class, your application must have permission RECORD_AUDIO.
 */
public class AudioAnalyser extends Instrument
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
	 * @param   parent          Parent surface.
	 */
	public AudioAnalyser(SurfaceRunner parent)
	{
		super(parent);
		parentSurface = parent;

		audioReader = new AudioReader();
	}

	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Set the sample rate for this instrument.
	 * 
	 * @param   rate        The desired rate, in samples/sec.
	 */
	public void setSampleRate(int rate)
	{
		sampleRate = rate;

	}

	/**
	 * Set the input block size for this instrument.
	 * 
	 * @param   size        The desired block size, in samples.  Typical
	 *                      values would be 256, 512, or 1024.  Larger block
	 *                      sizes will mean more work to analyse the spectrum.
	 */
	public void setBlockSize(int size)
	{
		inputBlockSize = size;

	}

	/**
	 * Set the decimation rate for this instrument.
	 * 
	 * @param   rate        The desired decimation.  Only 1 in rate blocks
	 *                      will actually be processed.
	 */
	public void setDecimation(int rate)
	{
		sampleDecimate = rate;
	}

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	/**
	 * The application is starting.  Perform any initial set-up prior to
	 * starting the application.  We may not have a screen size yet,
	 * so this is not a good place to allocate resources which depend on
	 * that.
	 */
	@Override
	public void appStart()
	{
	}

	/**
	 * We are starting the main run; start measurements.
	 */
	@Override
	public void measureStart()
	{
		audioProcessed = audioSequence = 0;
		readError = AudioReader.Listener.ERR_OK;

		audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener()
		{
			@Override
			public final void onReadComplete(short[] buffer)
			{
				receiveAudio(buffer);
			}

			@Override
			public void onReadError(int error)
			{
				handleError(error);
			}
		});
	}

	/**
	 * We are stopping / pausing the run; stop measurements.
	 */
	@Override
	public void measureStop()
	{
		audioReader.stopReader();
	}

	/**
	 * The application is closing down.  Clean up any resources.
	 */
	@Override
	public void appStop()
	{
	}

	// ******************************************************************** //
	// Gauges.
	// ******************************************************************** //

	/**
	 * Get a signal power gauge for this audio analyser.
	 * 
	 * @param   surface     The surface in which the gauge will be displayed.
	 * @return              A gauge which will display the signal power in
	 *                      a dB meter.
	 */
	public PowerGauge getPowerGauge(SurfaceRunner surface)
	{
		if (powerGauge != null)
			throw new RuntimeException("Already have a PowerGauge" + " for this AudioAnalyser");
		powerGauge = new PowerGauge(surface);
		return powerGauge;
	}

	/**
	 * Reset all Gauges before choosing new ones.
	 */
	public void resetGauge()
	{
		synchronized (this)
		{
			powerGauge = null;
		}
	}

	// ******************************************************************** //
	// Audio Processing.
	// ******************************************************************** //

	/**
	 * Handle audio input.  This is called on the thread of the audio
	 * reader.
	 * 
	 * @param   buffer      Audio data that was just read.
	 */
	private final void receiveAudio(short[] buffer)
	{
		// Lock to protect updates to these local variables.  See run().
		synchronized (this)
		{
			audioData = buffer;
			++audioSequence;
		}
	}

	/**
	 * An error has occurred.  The reader has been terminated.
	 * 
	 * @param   error       ERR_XXX code describing the error.
	 */
	private void handleError(int error)
	{
		synchronized (this)
		{
			readError = error;
		}
	}

	// ******************************************************************** //
	// Main Loop.
	// ******************************************************************** //

	/**
	 * Update the state of the instrument for the current frame.
	 * This method must be invoked from the doUpdate() method of the
	 * application's {@link SurfaceRunner}.
	 * 
	 * <p>Since this is called frequently, we first check whether new
	 * audio data has actually arrived.
	 * 
	 * @param   now         Nominal time of the current frame in ms.
	 */
	@Override
	public final void doUpdate(long now)
	{
		short[] buffer = null;
		synchronized (this)
		{
			if (audioData != null && audioSequence > audioProcessed)
			{
				parentSurface.statsCount(1, (int) (audioSequence - audioProcessed - 1));
				audioProcessed = audioSequence;
				buffer = audioData;
			}
		}

		// If we got data, process it without the lock.
		if (buffer != null)
			processAudio(buffer);

		if (readError != AudioReader.Listener.ERR_OK)
			processError(readError);
	}

	/**
	 * Handle audio input.  This is called on the thread of the
	 * parent surface.
	 * 
	 * @param   buffer      Audio data that was just read.
	 */
	private final void processAudio(short[] buffer)
	{
		// Process the buffer.  While reading it, it needs to be locked.
		synchronized (buffer)
		{
			// Calculate the power now, while we have the input
			// buffer; this is pretty cheap.
			final int len = buffer.length;

			// If we have a power gauge, calculate the signal power.
			if (powerGauge != null)
				currentPower = SignalPower.calculatePowerDb(buffer, 0, len);

			// Tell the reader we're done with the buffer.
			buffer.notify();
		}

		// If we have a power gauge, display the signal power.
		if (powerGauge != null)
			powerGauge.update(currentPower);
	}

	/**
	 * Handle an audio input error.
	 * 
	 * @param   error       ERR_XXX code describing the error.
	 */
	private final void processError(int error)
	{
		// Pass the error to all the gauges we have.
		if (powerGauge != null)
			powerGauge.error(error);
	}

	// ******************************************************************** //
	// Save and Restore.
	// ******************************************************************** //

	/**
	 * Save the state of the system in the provided Bundle.
	 * 
	 * @param   icicle      The Bundle in which we should save our state.
	 */
	@Override
	protected void saveState(Bundle icicle)
	{
		//      gameTable.saveState(icicle);
	}

	/**
	 * Restore the system state from the given Bundle.
	 * 
	 * @param   icicle      The Bundle containing the saved state.
	 */
	@Override
	protected void restoreState(Bundle icicle)
	{
		//      gameTable.pause();
		//      gameTable.restoreState(icicle);
	}

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	@SuppressWarnings("unused")
	private static final String	TAG				= "instrument";

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Our parent surface.
	private SurfaceRunner		parentSurface;

	// The desired sampling rate for this analyser, in samples/sec.
	private int					sampleRate		= 8000;

	// Audio input block size, in samples.
	private int					inputBlockSize	= 256;

	// The desired decimation rate for this analyser.  Only 1 in
	// sampleDecimate blocks will actually be processed.
	private int					sampleDecimate	= 1;

	// Our audio input device.
	private final AudioReader	audioReader;

	// The gauges associated with this instrument.  Any may be null if not
	// in use.
	private PowerGauge			powerGauge		= null;

	// Buffered audio data, and sequence number of the latest block.
	private short[]				audioData;
	private long				audioSequence	= 0;

	// If we got a read error, the error code.
	private int					readError		= AudioReader.Listener.ERR_OK;

	// Sequence number of the last block we processed.
	private long				audioProcessed	= 0;

	// Current signal power level, in dB relative to max. input power.
	private double				currentPower	= 0f;

}
