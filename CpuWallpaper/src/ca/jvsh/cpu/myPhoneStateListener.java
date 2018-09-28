package ca.jvsh.cpu;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

public class myPhoneStateListener extends PhoneStateListener
{
	static int lastGSMStrength = 0;
	
	public int getLastGSMStrength()
	{
		return lastGSMStrength;
	}
	
	public void onSignalStrengthsChanged(SignalStrength signalStrength)
	{
		if (!signalStrength.isGsm())
		{//TODO: getCDMAStrength instead here
			lastGSMStrength = 0;
			return;
		}
		if (signalStrength.getGsmSignalStrength() != lastGSMStrength)
		{
			lastGSMStrength = signalStrength.getGsmSignalStrength();
		}
	}
	
}
