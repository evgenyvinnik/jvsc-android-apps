package ca.jvsh.cpu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

class Top
{
	
	class Task implements Comparable<Task>
	{
		final private String mCmd;
		final private long mUsage;
		
		Task(final String command, final long usage)
		{
			mCmd = command;
			mUsage = usage;
		}
		
		Task delta(Task prev, long total)
		{
			return new Task(mCmd, (mUsage - prev.mUsage) * 1000 / total);
		}
		
		String getName()
		{
			return mCmd;
		}
		
		long getUsage()
		{
			return mUsage;
		}
		
		public int compareTo(Task other)
		{
			if (mUsage == other.mUsage) return mCmd.compareTo(other.mCmd);
			else return -(int) (mUsage - other.mUsage);
		}
		
	}
	
	Map<Integer, Task> mPrevState;
	long mPrevCpuTime;
	
	Top()
	{
		mPrevCpuTime = readCpuTime();
		mPrevState = readProcInfo();
	}
	
	public Vector<Task> getTopN()
	{
		Map<Integer, Task> current = readProcInfo();
		long cpu_time = readCpuTime();
		long delta_time = cpu_time - mPrevCpuTime;
		
		Set<Integer> pids = current.keySet();
		pids.retainAll(mPrevState.keySet());
		
		Vector<Task> results = new Vector<Task>();
		for (Iterator<Integer> it = pids.iterator(); it.hasNext();)
		{
			int index = it.next();
			results.add(current.get(index).delta(mPrevState.get(index), delta_time));
		}
		Collections.sort(results);
		
		mPrevState = current;
		mPrevCpuTime = cpu_time;
		return results;
	}
	
	private Map<Integer, Task> readProcInfo()
	{
		Map<Integer, Task> stats = new HashMap<Integer, Task>();
		File proc_dir = new File("/proc/");
		
		String files[] = proc_dir.list();
		for (int i = 0; i < files.length; ++i)
		{
			if (files[i].matches("[0-9]+") == true)
			{
				String stat = readData("/proc/" + files[i] + "/stat");
				if (stat == null) continue;
				
				String [] segs = stat.split("[ ]+");
				long runtime = Long.parseLong(segs[13]) + Long.parseLong(segs[14]);
				
				String cmdline = segs[1];
				if (cmdline.contains("(app_process)"))
				{
					String pkg_name = readData("/proc/" + files[i] + "/cmdline");
					cmdline = cleanCmdline(pkg_name);
				}
				stats.put(Integer.parseInt(files[i]), new Task(cmdline, runtime));
			}
		}
		return stats;
	}
	
	private long readCpuTime()
	{
		String cpustat = readData("proc/stat");
		if (cpustat == null)
		{
			return 0;
		}
		String [] segs = cpustat.split("[ ]+");
		
		return Long.parseLong(segs[1]) + Long.parseLong(segs[2]) + Long.parseLong(segs[3]) + Long.parseLong(segs[4]);
		
	}
	
	private String readData(String filename)
	{
		FileReader fstream;
		try
		{
			fstream = new FileReader(filename);
		}
		catch (FileNotFoundException e)
		{
			return null;
		}
		
		BufferedReader in = new BufferedReader(fstream, 500);
		try
		{
			return in.readLine();
		}
		catch (IOException e)
		{
			return null;
		}
	}
	
	private String cleanCmdline(String raw)
	{
		if (raw == null)
		{
			return "<invalid>";
		}
		for (int i = 0; i < raw.length(); i++)
		{
			if (Character.isIdentifierIgnorable(raw.charAt(i)))
			{
				return raw.substring(0, i);
			}
		}
		return raw;
	}
}
