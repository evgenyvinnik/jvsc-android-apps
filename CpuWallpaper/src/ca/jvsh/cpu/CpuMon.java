/*
 * Copyright (C) 2008 Google Inc. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ca.jvsh.cpu;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import ca.jvsh.cpu.CPUStatusLEDActivity;

public class CpuMon
{
	final private String STAT_FILE = "/proc/stat";
	private long mUser;
	private long mSystem;
	private long mTotal;
	
	static final int MAX_SIZE = CPUStatusChart.MAX_POINTS * 2;
	static ArrayList<Integer> userHistory = new ArrayList<Integer>(MAX_SIZE);
	static ArrayList<Integer> systemHistory = new ArrayList<Integer>(MAX_SIZE);
	static ArrayList<Integer> signalHistory = new ArrayList<Integer>(MAX_SIZE);
	
	private CPUStatusLEDActivity mDisplay;
	
	public static TelephonyManager telManager;//static to keep it alive
	
	public CpuMon(TelephonyManager telManager)
	{
		CpuMon.telManager = telManager;
		readStats();
	}
	
	public void linkDisplay(CPUStatusLEDActivity display)
	{
		mDisplay = display;
		readStats();
	}
	
	public void unlinkDisplay()
	{
		mDisplay = null;
	}
	
	public boolean readStats()
	{
		FileReader fstream;
		try
		{
			fstream = new FileReader(STAT_FILE);
		}
		catch (FileNotFoundException e)
		{
			return false;
		}
		BufferedReader in = new BufferedReader(fstream, 500);
		String line;
		try
		{
			while ((line = in.readLine()) != null)
			{
				if (line.startsWith("cpu"))
				{
					updateStats(line.trim().split("[ ]+"));//or expr "[ ]+"
					return true;//one line only
				}
			}
		}
		catch (IOException e)
		{
		}
		return false;
	}
	
	final static int wastepoints = 4;//wait for readings to stabilize on startup.
	
	/*
	 * line: cpu 312480 23494 10874 1529955 51540 0 8 0 0 0 uptime user systm
	 * idle 0 uptime: time since startup 1 user: normal processes executing in
	 * user mode 2 nice: niced processes executing in user mode 3 system:
	 * processes executing in kernel mode 4 idle: twiddling thumbs 5 iowait:
	 * waiting for I/O to complete 6 irq: servicing interrupts 7 softirq:
	 * servicing softirqs
	 */
	private void updateStats(String [] segs)
	{
		// user = user + nice
		long user = Long.parseLong(segs[1]) + Long.parseLong(segs[2]);
		// system = system + intr + soft_irq
		long system = Long.parseLong(segs[3]) + Long.parseLong(segs[6]) + Long.parseLong(segs[7]);
		// total = user + system + idle + io_wait
		long total = user + system + Long.parseLong(segs[4]) + Long.parseLong(segs[5]);
		
		if (mTotal != 0 || total >= mTotal)
		{
			long duser = user - mUser;
			long dsystem = system - mSystem;
			long dtotal = total - mTotal;
			//if the buffer is full
			if (userHistory.size() == MAX_SIZE)
			{
				userHistory.remove(0);
				systemHistory.remove(0);
				signalHistory.remove(0);
			}
			if (userHistory.size() < wastepoints)
			{
				userHistory.add(new Integer(0));
				systemHistory.add(new Integer(0));
				signalHistory.add(new Integer(0));
			}
			else
			{
				userHistory.add(new Integer((int) (duser * 100.0 / dtotal)));
				systemHistory.add(new Integer((int) (dsystem * 100.0 / dtotal)));
				//Log.i("blah%",""+getGSMSignalStrengthPercent());
				signalHistory.add(getGSMSignalStrengthPercent());
			}
			int totalCPUInt = new Double((duser + dsystem) * 100.0 / dtotal).intValue();

			if (mDisplay != null)
			{
				int size = userHistory.size() - 1;
				mDisplay.setCPUValues(totalCPUInt + "% (" + userHistory.get(size) + "/" + systemHistory.get(size) + ")");
				if (totalCPUInt > 75)
				{
					mDisplay.updateGraph(userHistory, systemHistory, signalHistory, getTopN(3));
				}
				else
				{
					mDisplay.updateGraph(userHistory, systemHistory, signalHistory, null);
				}
			}
		}
		mUser = user;
		mSystem = system;
		mTotal = total;
	}
	
	/**
	 * TODO Need a faster way
	 * 
	 * @param n
	 * @return
	 */
	private ArrayList<String> getTopN(int n)
	{
		ArrayList<String> processes = new ArrayList<String>(3);
		Top mTop = new Top();
		Vector<Top.Task> top_list = mTop.getTopN();
		int count = 0;
		for (Iterator<Top.Task> it = top_list.iterator(); it.hasNext();)
		{
			Top.Task task = it.next();
			//if (task.getUsage() == 0) break;
			if (task.getName().indexOf("cpustatusled") > 0) continue;
			processes.add(task.getUsage() / 10 + "% " + task.getName());
			count++;
			if (count == n) break;
		}
		return processes;
	}
	
	myPhoneStateListener signalListener;
	static int max_signal_seen = 16;
	int signal;
	
	public int getGSMSignalStrengthPercent()
	{
		if (signalListener == null) signalListener = new myPhoneStateListener();
		if (telManager != null) telManager.listen(signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		signal = signalListener.getLastGSMStrength();
		if (signal > max_signal_seen) max_signal_seen = signal;
		return (int) ((double) signal / max_signal_seen * 100.0);
	}
	
}
