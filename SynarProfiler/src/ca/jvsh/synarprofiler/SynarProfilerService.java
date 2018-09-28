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

package ca.jvsh.synarprofiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.jvsh.synarprofiler.R;
import ca.jvsh.synarprofiler.utils.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

/**
 * This is an example of implementing an application service that runs locally
 * in the same process as the application.  The {@link StepServiceController}
 * and {@link StepServiceBinding} classes show how to interact with the
 * service.
 *
 * <p>Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service.  This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
public class SynarProfilerService extends Service
{
	private static final String		TAG							= "ca.jvsh.synarprofiler.SynarProfilerService";
	private SharedPreferences		mSettings;

	private int						mSensorRateMilliseconds;
	private int						mFlushDataMilliseconds;
	private int						mSleepMilliseconds;

	private long					mSensorRateStart;
	private long					mSensorRateStop;
	private long					mFlushDataStart;
	private long					mFlushDataStop;

	private PowerManager.WakeLock	wakeLock;
	private NotificationManager		mNM;

	private BufferedWriter			mOutput;

	private boolean					mActive						= false;
	private Thread					profilingThread;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//battery annd cpu power
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private File					mBatteryElectricCurrentFile;
	private File					mBatteryVoltageFile;

	private TIntList				mBatteryElectricCurrentList;
	private TFloatList				mBatteryVoltageList;

	private boolean					mTrackBatteryCpu;																					//category flag

	private boolean					mTrackBattery;
	private boolean					mTrackCpu0;
	private boolean					mTrackCpu1;

	//CPU power files
	private File					mCpu0Freq					= new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");

	private File					mCpu1Freq					= new File("/sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq");

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//Power Sensors (files and voltages)
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int						mPowerSensorsValues;
	private int						mListValue;
	
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//main power sensors
	//
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private File					mDisplayElvddFile;
	private File					mDisplayMemoryFile;
	private File					mHapticsFile;
	private File					mTouchScreenFile;
	private File					mInternalMemoryFile;
	private File					mVddpx1Lpddr2File;
	private File					mIoPad3File;
	private File					mVregL16aFile;
	private File					mDramVdd2File;
	private File					mDigitalCoreFile;

	private TIntList				mDisplayElvddList;
	private TIntList				mDisplayMemoryList;
	private TIntList				mHapticsList;
	private TIntList				mTouchScreenList;
	private TIntList				mInternalMemoryList;
	private TIntList				mVddpx1Lpddr2List;
	private TIntList				mIoPad3List;
	private TIntList				mVregL16aList;
	private TIntList				mDramVdd2List;
	private TIntList				mDigitalCoreList;

	private final static float		mDisplayElvddVoltage		= 3.8f;
	private final static float		mDisplayMemoryVoltage		= 3.05f;
	private final static float		mHapticsVoltage				= 2.6f;
	private final static float		mTouchScreenVoltage			= 2.85f;
	private final static float		mInternalMemoryVoltage		= 1.1f;
	private final static float		mVddpx1Lpddr2Voltage		= 1.2f;
	private final static float		mIoPad3Voltage				= 1.8f;
	private final static float		mVregL16aVoltage			= 1.8f;
	private final static float		mDramVdd2Voltage			= 1.2f;
	private final static float		mDigitalCoreVoltage			= 1.1f;

	private boolean					mTrackMainPowerSensors;
	private boolean					mTrackDisplayElvdd;
	private boolean					mTrackDisplayMemory;
	private boolean					mTrackHaptics;
	private boolean					mTrackTouchScreen;
	private boolean					mTrackInternalMemory;
	private boolean					mTrackVddpx1Lpddr2;
	private boolean					mTrackIoPad3;
	private boolean					mTrackVregL16a;
	private boolean					mTrackDramVdd2;
	private boolean					mTrackDigitalCore;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//secondary power sensors
	//
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private File					mAmbientLightSensorFile;
	private File					mDisplayIoFile;
	private File					mAudioDspFile;
	private File					mCameraAnalogFile;
	private File					mCameraDigitalFile;
	private File					mCameraIoFile;
	private File					mAudioCodecAnalogFile;
	private File					mAudioCodecIoFile;
	private File					mAudioCodecVddcx1File;
	private File					mDramVdd1File;
	private File					mEmmcFile;
	private File					mEmmcHostInterfaceFile;
	private File					mHdmiFile;
	private File					mIoPad2File;
	private File					mSdCardFile;
	private File					mIsmVdd2File;
	private File					mVregL12aFile;
	private File					mVregL21aFile;

	private TIntList				mAmbientLightSensorList;
	private TIntList				mDisplayIoList;
	private TIntList				mAudioDspList;
	private TIntList				mCameraAnalogList;
	private TIntList				mCameraDigitalList;
	private TIntList				mCameraIoList;
	private TIntList				mAudioCodecAnalogList;
	private TIntList				mAudioCodecIoList;
	private TIntList				mAudioCodecVddcx1List;
	private TIntList				mDramVdd1List;
	private TIntList				mEmmcList;
	private TIntList				mEmmcHostInterfaceList;
	private TIntList				mHdmiList;
	private TIntList				mIoPad2List;
	private TIntList				mSdCardList;
	private TIntList				mIsmVdd2List;
	private TIntList				mVregL12aList;
	private TIntList				mVregL21aList;

	private final static float		mAmbientLightSensorVoltage	= 2.85f;
	private final static float		mDisplayIoVoltage			= 1.8f;
	private final static float		mAudioDspVoltage			= 1.1f;
	private final static float		mCameraAnalogVoltage		= 2.85f;
	private final static float		mCameraDigitalVoltage		= 1.2f;
	private final static float		mCameraIoVoltage			= 1.8f;
	private final static float		mAudioCodecAnalogVoltage	= 2.2f;
	private final static float		mAudioCodecIoVoltage		= 1.8f;
	private final static float		mAudioCodecVddcx1Voltage	= 1.2f;
	private final static float		mDramVdd1Voltage			= 1.8f;
	private final static float		mEmmcVoltage				= 2.85f;
	private final static float		mEmmcHostInterfaceVoltage	= 1.8f;
	private final static float		mHdmiVoltage				= 5.05f;
	private final static float		mIoPad2Voltage				= 2.85f;
	private final static float		mSdCardVoltage				= 2.85f;
	private final static float		mIsmVdd2Voltage				= 1.35f;
	private final static float		mVregL12aVoltage			= 2.05f;
	private final static float		mVregL21aVoltage			= 1.1f;

	private boolean					mTrackSecondaryPowerSensors;
	private boolean					mTrackAmbientLightSensor;
	private boolean					mTrackDisplayIo;
	private boolean					mTrackAudioDsp;
	private boolean					mTrackCameraAnalog;
	private boolean					mTrackCameraDigital;
	private boolean					mTrackCameraIo;
	private boolean					mTrackAudioCodecAnalog;
	private boolean					mTrackAudioCodecIo;
	private boolean					mTrackAudioCodecVddcx1;
	private boolean					mTrackDramVdd1;
	private boolean					mTrackEmmc;
	private boolean					mTrackEmmcHostInterface;
	private boolean					mTrackHdmi;
	private boolean					mTrackIoPad2;
	private boolean					mTrackSdCard;
	private boolean					mTrackIsmVdd2;
	private boolean					mTrackVregL12a;
	private boolean					mTrackVregL21a;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private String					line;
	//time and counter lists
	private TLongList				mTimeList;
	private long					mFirstStart;
	private TIntList				mCountList;
	private int						mCount;
	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class StepBinder extends Binder
	{
		SynarProfilerService getService()
		{
			return SynarProfilerService.this;
		}
	}

	@Override
	public void onCreate()
	{
		Log.i(TAG, "[SERVICE] onCreate");
		super.onCreate();

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();

		// Load settings
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		acquireWakeLock();
		
		// get the data sampling and data flushing periods
		mSensorRateMilliseconds = Integer.valueOf(mSettings.getString("sampling_period", "1000").trim());
		mFlushDataMilliseconds = Integer.valueOf(mSettings.getString("data_flushing_period", "100000").trim());

		//open output file to write header

		mOutput = null;
		Date lm = new Date();
		String fileName = "synar_profiler" + new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss").format(lm) + ".csv";
		try
		{
			File configFile = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
			FileWriter fileWriter = new FileWriter(configFile);
			mOutput = new BufferedWriter(fileWriter);
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.toString());
		}

		mTimeList = new TLongArrayList();
		mCountList = new TIntArrayList();
		
		try
		{

			mOutput.write("Count, Time [ms], ");

			//check profiling flags

			mTrackBatteryCpu = mSettings.getBoolean("battery_and_cpus_power", true);
			if (mTrackBatteryCpu)
			{
				mTrackBattery = mSettings.getBoolean("battery_power", true);
				if (mTrackBattery)
				{
					mBatteryElectricCurrentList = new TIntArrayList();
					mBatteryVoltageList = new TFloatArrayList();
					mOutput.write("Battery Current [uA], Battery Voltage [V], Battery Power [uW], ");
					mBatteryElectricCurrentFile = new File("/sys/devices/platform/msm_adc/curr19_input");
					mBatteryVoltageFile = new File("/sys/class/power_supply/battery/voltage_now");
				}

				mTrackCpu0 = mSettings.getBoolean("cpu0_power", false);
				mTrackCpu1 = mSettings.getBoolean("cpu1_power", false);
			}

			mTrackMainPowerSensors = mSettings.getBoolean("main_power_sensors", false);
			if (mTrackMainPowerSensors)
			{
				mTrackDisplayElvdd = mSettings.getBoolean("display_elvdd_power", false);
				if (mTrackDisplayElvdd)
				{
					mDisplayElvddList = new TIntArrayList();
					mOutput.write("Display Elvdd Current [uA], Display Elvdd Power [uW], ");
					mDisplayElvddFile = new File("/sys/devices/platform/msm_adc/curr38_input");
				}

				mTrackDisplayMemory = mSettings.getBoolean("display_memory_power", false);
				if (mTrackDisplayMemory)
				{
					mDisplayMemoryList = new TIntArrayList();
					mOutput.write("Display Memory Current [uA], Display Memory Power [uW], ");
					mDisplayMemoryFile = new File("/sys/devices/platform/msm_adc/curr40_input");
				}

				mTrackHaptics = mSettings.getBoolean("haptics_power", false);
				if (mTrackHaptics)
				{
					mHapticsList = new TIntArrayList();
					mOutput.write("Haptics Current [uA], Haptics Power [uW], ");
					mHapticsFile = new File("/sys/devices/platform/msm_adc/curr34_input");
				}

				mTrackTouchScreen = mSettings.getBoolean("touch_screen_power", false);
				if (mTrackTouchScreen)
				{
					mTouchScreenList = new TIntArrayList();
					mOutput.write("Touch Screen Current [uA],  Touch Screen Power [uW], ");
					mTouchScreenFile = new File("/sys/devices/platform/msm_adc/curr25_input");
				}

				mTrackInternalMemory = mSettings.getBoolean("internal_memory_power", false);
				if (mTrackInternalMemory)
				{
					mInternalMemoryList = new TIntArrayList();
					mOutput.write("Internal Memory Current [uA], Internal Memory Power [uW], ");
					mInternalMemoryFile = new File("/sys/devices/platform/msm_adc/curr27_input");
				}

				mTrackVddpx1Lpddr2 = mSettings.getBoolean("vddpx1_lpddr2_power", false);
				if (mTrackVddpx1Lpddr2)
				{
					mVddpx1Lpddr2List = new TIntArrayList();
					mOutput.write("VDDPX1 LPDDR2 Current [uA], VDDPX1 LPDDR2 Power [uW], ");
					mVddpx1Lpddr2File = new File("/sys/devices/platform/msm_adc/curr35_input");
				}

				mTrackIoPad3 = mSettings.getBoolean("io_pad3_power", false);
				if (mTrackIoPad3)
				{
					mIoPad3List = new TIntArrayList();
					mOutput.write("IO Pad3 Current [uA], IO Pad3 Power [uW], ");
					mIoPad3File = new File("/sys/devices/platform/msm_adc/curr32_input");
				}

				mTrackVregL16a = mSettings.getBoolean("vreg_l16a_power", false);
				if (mTrackVregL16a)
				{
					mVregL16aList = new TIntArrayList();
					mOutput.write("VREG L16A Current [uA], VREG L16A Power [uW], ");
					mVregL16aFile = new File("/sys/devices/platform/msm_adc/curr20_input");
				}

				mTrackDramVdd2 = mSettings.getBoolean("dram_vdd2_power", false);
				if (mTrackDramVdd2)
				{
					mDramVdd2List = new TIntArrayList();
					mOutput.write("DRAM VDD2 Current [uA], DRAM VDD2 Power, ");
					mDramVdd2File = new File("/sys/devices/platform/msm_adc/curr49_input");
				}
				mTrackDigitalCore = mSettings.getBoolean("digital_core_power", false);
				if (mTrackDigitalCore)
				{
					mDigitalCoreList = new TIntArrayList();
					mOutput.write("Digital Core Current [uA], Digital Core Power [uW], ");
					mDigitalCoreFile = new File("/sys/devices/platform/msm_adc/curr30_input");
				}
			}

			mTrackSecondaryPowerSensors = mSettings.getBoolean("secondary_power_sensors", false);
			if (mTrackSecondaryPowerSensors)
			{
				mTrackAmbientLightSensor = mSettings.getBoolean("ambient_light_sensor_power", false);
				if (mTrackAmbientLightSensor)
				{
					mAmbientLightSensorList = new TIntArrayList();
					mOutput.write("Ambient Light Current [uA], Ambient Light Power [uW], ");
					mAmbientLightSensorFile = new File("/sys/devices/platform/msm_adc/curr37_input");
				}

				mTrackDisplayIo = mSettings.getBoolean("display_io_power", false);
				if (mTrackDisplayIo)
				{
					mDisplayIoList = new TIntArrayList();
					mOutput.write("Display IO Current [uA], Display IO Power [uW], ");
					mDisplayIoFile = new File("/sys/devices/platform/msm_adc/curr39_input");
				}

				mTrackAudioDsp = mSettings.getBoolean("audio_dsp_power", false);
				if (mTrackAudioDsp)
				{
					mAudioDspList = new TIntArrayList();
					mOutput.write("Audio DSP Current [uA], Audio DSP Power [uW], ");
					mAudioDspFile = new File("/sys/devices/platform/msm_adc/curr18_input");
				}

				mTrackCameraAnalog = mSettings.getBoolean("camera_analog_power", false);
				if (mTrackCameraAnalog)
				{
					mCameraAnalogList = new TIntArrayList();
					mOutput.write("Camera Analog Current [uA], Camera Analog Power [uW], ");
					mCameraAnalogFile = new File("/sys/devices/platform/msm_adc/curr48_input");
				}

				mTrackCameraDigital = mSettings.getBoolean("camera_digital_power", false);
				if (mTrackCameraDigital)
				{
					mCameraDigitalList = new TIntArrayList();
					mOutput.write("Camera Digital Current [uA], Camera Digital Power [uW], ");
					mCameraDigitalFile = new File("/sys/devices/platform/msm_adc/curr47_input");
				}

				mTrackCameraIo = mSettings.getBoolean("camera_io_power", false);
				if (mTrackCameraIo)
				{
					mCameraIoList = new TIntArrayList();
					mOutput.write("Camera IO Current [uA], Camera IO Power [uW], ");
					mCameraIoFile = new File("/sys/devices/platform/msm_adc/curr46_input");
				}

				mTrackAudioCodecAnalog = mSettings.getBoolean("audio_codec_analog_power", false);
				if (mTrackAudioCodecAnalog)
				{
					mAudioCodecAnalogList = new TIntArrayList();
					mOutput.write("Audio Codec Current [uA], Audio Codec Power [uW], ");
					mAudioCodecAnalogFile = new File("/sys/devices/platform/msm_adc/curr24_input");
				}

				mTrackAudioCodecIo = mSettings.getBoolean("audio_codec_io_power", false);
				if (mTrackAudioCodecIo)
				{
					mAudioCodecIoList = new TIntArrayList();
					mOutput.write("Audio Codec IO Current [uA], Audio Codec IO Power [uW], ");
					mAudioCodecIoFile = new File("/sys/devices/platform/msm_adc/curr22_input");
				}

				mTrackAudioCodecVddcx1 = mSettings.getBoolean("audio_codec_vddcx_1_power", false);
				if (mTrackAudioCodecVddcx1)
				{
					mAudioCodecVddcx1List = new TIntArrayList();
					mOutput.write("Audio Codec VDDCX1 Current [uA], Audio Codec VDDCX1 Power [uW], ");
					mAudioCodecVddcx1File = new File("/sys/devices/platform/msm_adc/curr23_input");
				}

				mTrackDramVdd1 = mSettings.getBoolean("dram_vdd1_power", false);
				if (mTrackDramVdd1)
				{
					mDramVdd1List = new TIntArrayList();
					mOutput.write("DRAM VDD Current [uA], DRAM VDD Power [uW], ");
					mDramVdd1File = new File("/sys/devices/platform/msm_adc/curr36_input");
				}
				mTrackEmmc = mSettings.getBoolean("emmc_power", false);
				if (mTrackEmmc)
				{
					mEmmcList = new TIntArrayList();
					mOutput.write("Emmc Current [uA], Emmc Power, ");
					mEmmcFile = new File("/sys/devices/platform/msm_adc/curr29_input");
				}
				mTrackEmmcHostInterface = mSettings.getBoolean("emmc_host_interface_power", false);
				if (mTrackEmmcHostInterface)
				{
					mEmmcHostInterfaceList = new TIntArrayList();
					mOutput.write("Emmc Host Interface Current [uA], Emmc Host Interface Power [uW], ");
					mEmmcHostInterfaceFile = new File("/sys/devices/platform/msm_adc/curr41_input");
				}
				mTrackHdmi = mSettings.getBoolean("hdmi_power", false);
				if (mTrackHdmi)
				{
					mHdmiList = new TIntArrayList();
					mOutput.write("HDMI Current [uA], HDMI Power [uW], ");
					mHdmiFile = new File("/sys/devices/platform/msm_adc/curr42_input");
				}
				mTrackIoPad2 = mSettings.getBoolean("io_pad2_power", false);
				if (mTrackIoPad2)
				{
					mIoPad2List = new TIntArrayList();
					mOutput.write("IO Pad2 Current [uA], IO Pad2 Power [uW], ");
					mIoPad2File = new File("/sys/devices/platform/msm_adc/curr33_input");
				}
				mTrackSdCard = mSettings.getBoolean("sd_card_power", false);
				if (mTrackSdCard)
				{
					mSdCardList = new TIntArrayList();
					mOutput.write("SD Card Current [uA], SD Card Power [uW], ");
					mSdCardFile = new File("/sys/devices/platform/msm_adc/curr21_input");
				}
				mTrackIsmVdd2 = mSettings.getBoolean("ism_vdd2_power", false);
				if (mTrackIsmVdd2)
				{
					mIsmVdd2List = new TIntArrayList();
					mOutput.write("ISM VDD2 Current [uA], ISM VDD2 Power [uW], ");
					mIsmVdd2File = new File("/sys/devices/platform/msm_adc/curr31_input");
				}
				mTrackVregL12a = mSettings.getBoolean("vreg_l13a_power", false);
				if (mTrackVregL12a)
				{
					mVregL12aList = new TIntArrayList();
					mOutput.write("VReg L13A Current [uA], VReg L13A Power [uW], ");
					mVregL12aFile = new File("/sys/devices/platform/msm_adc/curr18_input");
				}
				mTrackVregL21a = mSettings.getBoolean("vreg_l21a_power", false);
				if (mTrackVregL21a)
				{
					mVregL21aList = new TIntArrayList();
					mOutput.write("Vreg L21A Current [uA], Vreg L21A Power [uW], ");
					mVregL21aFile = new File("/sys/devices/platform/msm_adc/curr43_input");
				}

			}

			mOutput.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		registerDetector();

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mReceiver, filter);

		// Tell the user we started.
		Toast.makeText(this, getText(R.string.started) 
				+ "\nSampling period " + mSensorRateMilliseconds + " ms"
				+ "\nData flush period " + mFlushDataMilliseconds + " ms"
				+ "\nLog saved to " + fileName,
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		Log.i(TAG, "[SERVICE] onStart");
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy()
	{
		Log.i(TAG, "[SERVICE] onDestroy");

		// Unregister our receiver.
		unregisterReceiver(mReceiver);
		unregisterDetector();

		mNM.cancel(R.string.app_name);

		wakeLock.release();

		super.onDestroy();

		// Stop detecting
		unregisterDetector();

		// Tell the user we stopped.
		Toast.makeText(this, getText(R.string.stopped), Toast.LENGTH_LONG).show();
	}

	private void registerDetector()
	{
		mActive = true;

		profilingThread = new Thread()
		{
			public void run()
			{
				
				mPowerSensorsValues = 0;
				mCount = 0;

				mFirstStart = mFlushDataStart = Utils.currentTimeInMillis();
				while (mActive)
				{
					try
					{
						mSensorRateStart = Utils.currentTimeInMillis();
						mTimeList.add(mSensorRateStart - mFirstStart);

						if (mTrackBatteryCpu)
						{
							if (mTrackBattery)
							{
								{
									RandomAccessFile localRandomAccessFile = new RandomAccessFile(mBatteryElectricCurrentFile, "r");
									line = localRandomAccessFile.readLine();
									mBatteryElectricCurrentList.add(
											line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
									localRandomAccessFile.close();
								}
								{
									RandomAccessFile localRandomAccessFile = new RandomAccessFile(mBatteryVoltageFile, "r");
									line = localRandomAccessFile.readLine();
									mBatteryVoltageList.add(line != null ? Integer.parseInt(line) / 1000.0F : -1);
									localRandomAccessFile.close();
								}
							}

						}

						if (mTrackMainPowerSensors)
						{
							if (mTrackDisplayElvdd)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mDisplayElvddFile, "r");
								line = localRandomAccessFile.readLine();
								mDisplayElvddList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackDisplayMemory)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mDisplayMemoryFile, "r");
								line = localRandomAccessFile.readLine();
								mDisplayMemoryList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackHaptics)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mHapticsFile, "r");
								line = localRandomAccessFile.readLine();
								mHapticsList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackTouchScreen)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mTouchScreenFile, "r");
								line = localRandomAccessFile.readLine();
								mTouchScreenList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackInternalMemory)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mInternalMemoryFile, "r");
								line = localRandomAccessFile.readLine();
								mInternalMemoryList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackVddpx1Lpddr2)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mVddpx1Lpddr2File, "r");
								line = localRandomAccessFile.readLine();
								mVddpx1Lpddr2List.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackIoPad3)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mIoPad3File, "r");
								line = localRandomAccessFile.readLine();
								mIoPad3List.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackVregL16a)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mVregL16aFile, "r");
								line = localRandomAccessFile.readLine();
								mVregL16aList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackDramVdd2)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mDramVdd2File, "r");
								line = localRandomAccessFile.readLine();
								mDramVdd2List.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackDigitalCore)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mDigitalCoreFile, "r");
								line = localRandomAccessFile.readLine();
								mDigitalCoreList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}
						}

						if (mTrackSecondaryPowerSensors)
						{
							if (mTrackAmbientLightSensor)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mAmbientLightSensorFile, "r");
								line = localRandomAccessFile.readLine();
								mAmbientLightSensorList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackDisplayIo)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mDisplayIoFile, "r");
								line = localRandomAccessFile.readLine();
								mDisplayIoList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackAudioDsp)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mAudioDspFile, "r");
								line = localRandomAccessFile.readLine();
								mAudioDspList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackCameraAnalog)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mCameraAnalogFile, "r");
								line = localRandomAccessFile.readLine();
								mCameraAnalogList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackCameraDigital)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mCameraDigitalFile, "r");
								line = localRandomAccessFile.readLine();
								mCameraDigitalList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackCameraIo)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mCameraIoFile, "r");
								line = localRandomAccessFile.readLine();
								mCameraIoList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackAudioCodecAnalog)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mAudioCodecAnalogFile, "r");
								line = localRandomAccessFile.readLine();
								mAudioCodecAnalogList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackAudioCodecIo)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mAudioCodecIoFile, "r");
								line = localRandomAccessFile.readLine();
								mAudioCodecIoList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackAudioCodecVddcx1)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mAudioCodecVddcx1File, "r");
								line = localRandomAccessFile.readLine();
								mAudioCodecVddcx1List.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackDramVdd1)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mDramVdd1File, "r");
								line = localRandomAccessFile.readLine();
								mDramVdd1List.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackEmmc)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mEmmcFile, "r");
								line = localRandomAccessFile.readLine();
								mEmmcList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackEmmcHostInterface)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mEmmcHostInterfaceFile, "r");
								line = localRandomAccessFile.readLine();
								mEmmcHostInterfaceList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackHdmi)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mHdmiFile, "r");
								line = localRandomAccessFile.readLine();
								mHdmiList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackIoPad2)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mIoPad2File, "r");
								line = localRandomAccessFile.readLine();
								mIoPad2List.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackSdCard)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mSdCardFile, "r");
								line = localRandomAccessFile.readLine();
								mSdCardList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackIsmVdd2)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mIsmVdd2File, "r");
								line = localRandomAccessFile.readLine();
								mIsmVdd2List.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackVregL12a)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mVregL12aFile, "r");
								line = localRandomAccessFile.readLine();
								mVregL12aList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

							if (mTrackVregL21a)
							{
								RandomAccessFile localRandomAccessFile = new RandomAccessFile(mVregL21aFile, "r");
								line = localRandomAccessFile.readLine();
								mVregL21aList.add(line != null ? Integer.valueOf(android.text.TextUtils.split(line, "\\s+")[1]).intValue() : -1);
								localRandomAccessFile.close();
							}

						}

						mCountList.add(mCount);
						mCount++;
						mPowerSensorsValues++;

						mFlushDataStop = mSensorRateStop = Utils.currentTimeInMillis();
						mSleepMilliseconds = (int) (mSensorRateMilliseconds - (mSensorRateStop - mSensorRateStart));
						if (mSleepMilliseconds > 0)
						{
							Thread.sleep(mSensorRateMilliseconds, 0);
						}

						if ((mFlushDataStop - mFlushDataStart) > mFlushDataMilliseconds)
						{
							//put to file
							mFlushDataStart = mFlushDataStop;

							write_data();
							mOutput.flush();

							mPowerSensorsValues = 0;

						}
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				try
				{
					write_data();

					mOutput.flush();
					mOutput.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

		};
		profilingThread.start();
	}

	private void write_data()
	{
		try
		{
			for (int i = 0; i < mPowerSensorsValues; i++)
			{
				mOutput.write( mCountList.get(i) + ", " + mTimeList.get(i) + ",");
				
				if (mTrackBatteryCpu)
				{
					if (mTrackBattery)
					{
						mListValue = mBatteryElectricCurrentList.get(i);
						mOutput.write(mListValue + ", " + mBatteryVoltageList.get(i) + ", "
								+ (mListValue * mBatteryVoltageList.get(i)) + ", ");
					}

				}

				if (mTrackMainPowerSensors)
				{
					if (mTrackDisplayElvdd)
					{

						mListValue = mDisplayElvddList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mDisplayElvddVoltage) + ", ");
					}

					if (mTrackDisplayMemory)
					{
						mListValue = mDisplayMemoryList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mDisplayMemoryVoltage) + ", ");
					}

					if (mTrackHaptics)
					{
						mListValue = mHapticsList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mHapticsVoltage) + ", ");
					}

					if (mTrackTouchScreen)
					{
						mListValue = mTouchScreenList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mTouchScreenVoltage) + ", ");
					}

					if (mTrackInternalMemory)
					{
						mListValue = mInternalMemoryList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mInternalMemoryVoltage) + ", ");
					}

					if (mTrackVddpx1Lpddr2)
					{
						mListValue = mVddpx1Lpddr2List.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mVddpx1Lpddr2Voltage) + ", ");
					}

					if (mTrackIoPad3)
					{
						mListValue = mIoPad3List.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mIoPad3Voltage) + ", ");
					}

					if (mTrackVregL16a)
					{
						mListValue = mVregL16aList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mVregL16aVoltage) + ", ");
					}

					if (mTrackDramVdd2)
					{
						mListValue = mDramVdd2List.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mDramVdd2Voltage) + ", ");
					}

					if (mTrackDigitalCore)
					{
						mListValue = mDigitalCoreList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mDigitalCoreVoltage) + ", ");
					}
				}

				if (mTrackSecondaryPowerSensors)
				{
					if (mTrackAmbientLightSensor)
					{
						mListValue = mAmbientLightSensorList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mAmbientLightSensorVoltage) + ", ");
					}

					if (mTrackDisplayIo)
					{
						mListValue = mDisplayIoList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mDisplayIoVoltage) + ", ");
					}

					if (mTrackAudioDsp)
					{
						mListValue = mAudioDspList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mAudioDspVoltage) + ", ");
					}

					if (mTrackCameraAnalog)
					{
						mListValue = mCameraAnalogList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mCameraAnalogVoltage) + ", ");
					}

					if (mTrackCameraDigital)
					{
						mListValue = mCameraDigitalList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mCameraDigitalVoltage) + ", ");
					}

					if (mTrackCameraIo)
					{
						mListValue = mCameraIoList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mCameraIoVoltage) + ", ");
					}

					if (mTrackAudioCodecAnalog)
					{
						mListValue = mAudioCodecAnalogList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mAudioCodecAnalogVoltage) + ", ");
					}

					if (mTrackAudioCodecIo)
					{
						mListValue = mAudioCodecIoList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mAudioCodecIoVoltage) + ", ");
					}

					if (mTrackAudioCodecVddcx1)
					{
						mListValue = mAudioCodecVddcx1List.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mAudioCodecVddcx1Voltage) + ", ");
					}

					if (mTrackDramVdd1)
					{
						mListValue = mDramVdd1List.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mDramVdd1Voltage) + ", ");
					}

					if (mTrackEmmc)
					{
						mListValue = mEmmcList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mEmmcVoltage) + ", ");
					}

					if (mTrackEmmcHostInterface)
					{
						mListValue = mEmmcHostInterfaceList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mEmmcHostInterfaceVoltage) + ", ");
					}

					if (mTrackHdmi)
					{
						mListValue = mHdmiList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mHdmiVoltage) + ", ");
					}

					if (mTrackIoPad2)
					{
						mListValue = mIoPad2List.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mIoPad2Voltage) + ", ");
					}

					if (mTrackSdCard)
					{
						mListValue = mSdCardList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mSdCardVoltage) + ", ");
					}

					if (mTrackIsmVdd2)
					{
						mListValue = mIsmVdd2List.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mIsmVdd2Voltage) + ", ");
					}

					if (mTrackVregL12a)
					{
						mListValue = mVregL12aList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mVregL12aVoltage) + ", ");
					}

					if (mTrackVregL21a)
					{
						mListValue = mVregL21aList.get(i);
						mOutput.write(mListValue + ", " + (mListValue * mVregL21aVoltage) + ", ");
					}

				}
				mOutput.newLine();

			}

			mTimeList.clear();
			mCountList.clear();
			if (mTrackBatteryCpu)
			{
				if (mTrackBattery)
				{
					mBatteryElectricCurrentList.clear();
					mBatteryVoltageList.clear();
				}

			}

			if (mTrackMainPowerSensors)
			{
				if (mTrackDisplayElvdd)
					mDisplayElvddList.clear();

				if (mTrackDisplayMemory)
					mDisplayMemoryList.clear();

				if (mTrackHaptics)
					mHapticsList.clear();

				if (mTrackTouchScreen)
					mTouchScreenList.clear();

				if (mTrackInternalMemory)
					mInternalMemoryList.clear();

				if (mTrackVddpx1Lpddr2)
					mVddpx1Lpddr2List.clear();

				if (mTrackIoPad3)
					mIoPad3List.clear();

				if (mTrackVregL16a)
					mVregL16aList.clear();

				if (mTrackDramVdd2)
					mDramVdd2List.clear();

				if (mTrackDigitalCore)
					mDigitalCoreList.clear();
			}

			if (mTrackSecondaryPowerSensors)
			{
				if (mTrackAmbientLightSensor)
					mAmbientLightSensorList.clear();

				if (mTrackDisplayIo)
					mDisplayIoList.clear();

				if (mTrackAudioDsp)
					mAudioDspList.clear();

				if (mTrackCameraAnalog)
					mCameraAnalogList.clear();

				if (mTrackCameraDigital)
					mCameraDigitalList.clear();

				if (mTrackCameraIo)
					mCameraIoList.clear();

				if (mTrackAudioCodecAnalog)
					mAudioCodecAnalogList.clear();

				if (mTrackAudioCodecIo)
					mAudioCodecIoList.clear();

				if (mTrackAudioCodecVddcx1)
					mAudioCodecVddcx1List.clear();

				if (mTrackDramVdd1)
					mDramVdd1List.clear();

				if (mTrackEmmc)
					mEmmcList.clear();

				if (mTrackEmmcHostInterface)
					mEmmcHostInterfaceList.clear();

				if (mTrackHdmi)
					mHdmiList.clear();

				if (mTrackIoPad2)
					mIoPad2List.clear();

				if (mTrackSdCard)
					mSdCardList.clear();

				if (mTrackIsmVdd2)
					mIsmVdd2List.clear();

				if (mTrackVregL12a)
					mVregL12aList.clear();

				if (mTrackVregL21a)
					mVregL21aList.clear();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private void unregisterDetector()
	{
		mActive = false;
		profilingThread = null;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i(TAG, "[SERVICE] onBind");
		return mBinder;
	}

	/**
	 * Receives messages from activity.
	 */
	private final IBinder	mBinder	= new StepBinder();

	public interface ICallback
	{
		public void stepsChanged(int value);
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification()
	{
		CharSequence text = getText(R.string.app_name);
		Notification notification = new Notification(R.drawable.ic_notification, null,
				System.currentTimeMillis());
		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		Intent fallDetectorIntent = new Intent();
		fallDetectorIntent.setComponent(new ComponentName(this, SynarProfiler.class));
		fallDetectorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				fallDetectorIntent, 0);
		notification.setLatestEventInfo(this, text,
				getText(R.string.notification_subtitle), contentIntent);

		mNM.notify(R.string.app_name, notification);
	}

	// BroadcastReceiver for handling ACTION_SCREEN_OFF.
	private BroadcastReceiver	mReceiver	= new BroadcastReceiver()
											{
												@Override
												public void onReceive(Context context, Intent intent)
												{
													// Check action just to be on the safe side.
													if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
													{
														// Unregisters the listener and registers it again.
														SynarProfilerService.this.unregisterDetector();
														SynarProfilerService.this.registerDetector();
														if (mSettings.getString("operation_level", "run_in_background").equals("wake_up"))
														{
															wakeLock.release();
															acquireWakeLock();
														}
													}
												}
											};

	private void acquireWakeLock()
	{
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		int wakeFlags;
		if (mSettings.getString("operation_level", "run_in_background").equals("wake_up"))
		{
			wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
		}
		else if (mSettings.getString("operation_level", "run_in_background").equals("keep_screen_on"))
		{
			wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK;
		}
		else
		{
			wakeFlags = PowerManager.PARTIAL_WAKE_LOCK;
		}
		wakeLock = pm.newWakeLock(wakeFlags, TAG);
		wakeLock.acquire();
	}

}
