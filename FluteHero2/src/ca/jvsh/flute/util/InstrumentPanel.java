/**
 * Audalyzer: an audio analyzer for Android.
 * <br>Copyright 2009-2010 Ian Cameron Smith
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

package ca.jvsh.flute.util;

import ca.jvsh.flute.util.AudioAnalyser;
import ca.jvsh.flute.util.InstrumentSurface;
import ca.jvsh.flute.util.PowerGauge;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

/**
 * The main audio analyser view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class InstrumentPanel extends InstrumentSurface
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
	public InstrumentPanel(Activity app)
	{
		super(app, SURFACE_DYNAMIC);

		audioAnalyser = new AudioAnalyser(this);

		addInstrument(audioAnalyser);

		// On-screen debug stats display.
		statsCreate(new String[] { "µs FFT", "Skip/s" });

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
		audioAnalyser.setSampleRate(rate);
	}

	/**
	 * Set the input block size for this instrument.
	 * 
	 * @param   size        The desired block size, in samples.
	 */
	public void setBlockSize(int size)
	{
		audioAnalyser.setBlockSize(size);
	}

	/**
	 * Set the decimation rate for this instrument.
	 * 
	 * @param   rate        The desired decimation.  Only 1 in rate blocks
	 *                      will actually be processed.
	 */
	public void setDecimation(int rate)
	{
		audioAnalyser.setDecimation(rate);
	}

	/**
	 * Enable or disable stats display.
	 * 
	 * @param   enable        True to display performance stats.
	 */
	public void setShowStats(boolean enable)
	{
		setDebugPerf(enable);
	}

	/**
	 * Set the instruments to display
	 * 
	 * @param   InstrumentPanel.Intruments        Choose which ones to display.
	 */
	public void setInstruments()
	{
		loadInstruments();
	}

	/**
	 * Load instruments
	 * 
	 * @param   InstrumentPanel.Intruments        Choose which ones to display.
	 */
	private void loadInstruments()
	{
		Log.i(TAG, "Load instruments");

		//Stop surface update
		onPause();

		//Clear surface events
		clearGauges();

		//Clear analyse events
		audioAnalyser.resetGauge();

		//Destroy last Gauges
		powerGauge = null;

		//Create instruments, update and refresh

		powerGauge = audioAnalyser.getPowerGauge(this);
		addGauge(powerGauge);

		//Load current layout in Gauges if they're already define 
		if ((currentWidth > 0) && (currentHeight > 0))
			refreshLayout();

		//Restart
		onResume();

		Log.i(TAG, "End instruments loading");
	}

	// ******************************************************************** //
	// Layout Processing.
	// ******************************************************************** //

	/**
	 * Lay out the display for a given screen size.
	 * 
	 * @param   width       The new width of the surface.
	 * @param   height      The new height of the surface.
	 */
	@Override
	protected void layout(int width, int height)
	{
		//Save current layout
		currentWidth = width;
		currentHeight = height;
		refreshLayout();
	}

	/**
	 * Lay out the display for the current screen size.
	 */
	protected void refreshLayout()
	{
		// Make up some layout parameters.
		powerRect = new Rect(0, 2* currentHeight/3,currentWidth / 8 , currentHeight);
		
		// Set the gauge geometries.
		if (powerGauge != null)
			powerGauge.setGeometry(powerRect);
	}

	// ******************************************************************** //
	// Save and Restore.
	// ******************************************************************** //

	/**
	 * Save the state of the panel in the provided Bundle.
	 * 
	 * @param   icicle      The Bundle in which we should save our state.
	 */
	protected void saveState(Bundle icicle)
	{
		//      gameTable.saveState(icicle);
	}

	/**
	 * Restore the panel's state from the given Bundle.
	 * 
	 * @param   icicle      The Bundle containing the saved state.
	 */
	protected void restoreState(Bundle icicle)
	{
		//      gameTable.pause();
		//      gameTable.restoreState(icicle);
	}

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	private static final String	TAG				= "Audalyzer";

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	//Current layout
	private int					currentWidth	= 0;
	private int					currentHeight	= 0;

	// Our audio input device.
	private final AudioAnalyser	audioAnalyser;

	// The gauges associated with this instrument.
	private PowerGauge			powerGauge		= null;

	// Bounding rectangles for the waveform, spectrum, sonagram, and VU meter displays.
	private Rect				powerRect		= null;

}
