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


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import ca.jvsh.cpu.CpuMon;
import ca.jvsh.cpu.myPhoneStateListener;

/**
 * Local service which operates in close cooperation with CPUStatusLED activity.
 * 
 * Execute monitoring through periodic polling, update in-memory history buffers
 * and update display if linkage has been established by the activity after
 * binding to the service.
 * 
 * Whenever running, maintain a persistent notification in the status bar, which
 * sends an intent to (re)start CPUStatusLED activity.
 */
public class CPUStatusLEDService extends Service
{
	final private int SAMPLING_INTERVAL = 3;
	private final IBinder mBinder = new CPUStatusLEDBinder();
	private CpuMon mCpuMon;
	
	/**
	 * 
	 * Binder implementation which passes through a reference to this service.
	 * Since this is a local service, the activity can then call directly
	 * methods on this service instance.
	 */
	public class CPUStatusLEDBinder extends Binder
	{
		CPUStatusLEDService getService()
		{
			return CPUStatusLEDService.this;
		}
	}
	
	public void setGui(CPUStatusLEDActivity gui)
	{
		mCpuMon.linkDisplay(gui);
	}
	
	// All the polling and display updating is driven from this
	// hander which is periodically executed every SAMPLING_INTERVAL seconds.
	private Handler mHandler = new Handler();
	private Runnable mRefresh = new Runnable()
	{
		public void run()
		{
			//this reads the /proc/stats file
			mCpuMon.readStats();//calls updateStats()
			mHandler.postDelayed(mRefresh, SAMPLING_INTERVAL * 1000);
		}
	};
	
	static myPhoneStateListener signalListener;
	
	/**
	 * Framework method called when the service is first created.
	 */
	@Override
	public void onCreate()
	{
		//if(gui==null) stopSelf();
		mCpuMon = new CpuMon((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
		//thread that reads cpu stats
		mHandler.postDelayed(mRefresh, SAMPLING_INTERVAL * 1000);
		//monitor signal strength
		signalListener = new myPhoneStateListener();
	}
	
	/**
	 * Framework method called when the service is stopped/destroyed
	 */
	@Override
	public void onDestroy()
	{
		mHandler.removeCallbacks(mRefresh);
		this.stopSelf();
	}
	
	/**
	 * Framework method called whenever an activity binds to this service.
	 */
	@Override
	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}
	
	/**
	 * Framework method called when an activity binding to the service is
	 * broken.
	 */
	@Override
	public boolean onUnbind(Intent arg)
	{
		mCpuMon.unlinkDisplay();
		return true;
	}
	
}
