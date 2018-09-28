/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.jvsh.falldetection;

import java.io.BufferedWriter;
import java.io.IOException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Detects steps and notifies all listeners (that implement StepListener).
 * @author Levente Bagi
 * @todo REFACTOR: SensorListener is deprecated
 */
public class GyroscopeDataWriter implements SensorEventListener
{
	//private final static String	TAG	= "SensorDataWriter";

	int							mLinesWritten;
	BufferedWriter				mOutput;

	public GyroscopeDataWriter(BufferedWriter output)
	{
		mLinesWritten = 0;
		mOutput = output;
	}

	//public void onSensorChanged(int sensor, float[] values) {
	public void onSensorChanged(SensorEvent event)
	{
		Sensor sensor = event.sensor;
		synchronized (this)
		{

			if (sensor.getType() == Sensor.TYPE_GYROSCOPE)
			{
				if (mOutput != null)
				{
					try
					{

						if (mLinesWritten == 0)
						{
							mOutput.write("Metadata," +
									"Time," +
									"Sample Count," +
									"Angular Velocity X (rad/s),Angular Velocity Y (rad/s),Angular Velocity Z (rad/s),");
							mOutput.newLine();
						}
						if (mLinesWritten == 0)
						{
							mOutput.write("File Format Version=3,");
						}
						else if (mLinesWritten == 1)
						{
							mOutput.write("Monitor Case IDs= :SI-000178,");
						}
						else if (mLinesWritten == 2)
						{
							mOutput.write("Monitor Labels= :l.thigh,");
						}
						else if (mLinesWritten == 3)
						{
							mOutput.write("Version String1=Qualcomm MDP,");
						}
						else if (mLinesWritten == 4)
						{
							mOutput.write("Version String2=,");
						}
						else if (mLinesWritten == 5)
						{
							mOutput.write("Version String3=,");
						}
						else if (mLinesWritten == 6)
						{
							mOutput.write("Calibration Version=,");
						}
						else
						{
							mOutput.write(" ,");
						}

						mOutput.write(
										//Time
										event.timestamp + "," +
										//Sample Count
										mLinesWritten + "," +
										//Angular Velocity X (rad/s)
										event.values[0] + "," +
										//Angular Velocity Y (rad/s)
										event.values[1] + "," +
										//Angular Velocity Z (rad/s)
										event.values[2] + ","
										
								);
						mOutput.newLine();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				mLinesWritten++;
			}
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub
	}

}