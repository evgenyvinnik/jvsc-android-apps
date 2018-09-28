package ca.jvsh.guitardemo;

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockFragmentActivity;

//import ca.jvsh.gameinput.GameView;
import ca.jvsh.guitardemo.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.InputDevice.MotionRange;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class GuitarDemo extends SherlockFragmentActivity
{
	private static final String				TAG				= "GameControllerInput";

	private static final int				GUITAR_MENU_ID	= Menu.FIRST;
	GuitarView								guitarView;
	//flag that determine guitar type
	boolean									electric		= true;

	//game controller
	//private SparseArray<InputDeviceState>	mInputDeviceStates;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);

		//mInputDeviceStates = new SparseArray<InputDeviceState>();

		setContentView(R.layout.main);

		//create guitar view
		guitarView = (GuitarView) findViewById(R.id.guitarview);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(Menu.NONE, GUITAR_MENU_ID, 0, "Switch to Acoustic Guitar").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return super.onCreateOptionsMenu(menu);

	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		//final ContentResolver cr = getActivity().getContentResolver();

		switch (item.getItemId())
		{
			case GUITAR_MENU_ID:
				electric = !electric;
				setGuitarType(electric);
				if (electric)
				{
					item.setTitle("Switch to Acoustic Guitar");
				}
				else
				{
					item.setTitle("Switch to Electric Guitar");
				}

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        guitarView.requestFocus();
    }

    
	public void setGuitarType(boolean electric)
	{
		guitarView.setGuitarType(electric);
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		
		// Update device state for visualization and logging.
		/*InputDeviceState state = getInputDeviceState(event);
		final int keyCount = state.getKeyCount();
		if (state != null)
		{
			switch (event.getAction())
			{
				case KeyEvent.ACTION_DOWN:
					if (state.onKeyDown(event))
					{
						  for (int i = 0; i < keyCount; i++) 
						  {
							  if(state.isKeyPressed(i))
							  {
								  guitarView.setFrequency(i);
								  Log.d(TAG, "KeyEvent Action Down " +i);
							  }
						  }
						//TODO add actions here
						//mSummaryAdapter.show(state);
					}
					break;
				case KeyEvent.ACTION_UP:
					if (state.onKeyUp(event))
					{
						//TODO add actions here
						//mSummaryAdapter.show(state);
					}
					break;
			}
		}*/
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event)
	{
		// Check that the event came from a joystick since a generic motion event
		// could be almost anything.
		/*if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
				&& event.getAction() == MotionEvent.ACTION_MOVE)
		{
			// Update device state for visualization and logging.
			InputDeviceState state = getInputDeviceState(event);
			if (state != null && state.onJoystickMotion(event))
			{
				//TODO add actions here
				//mSummaryAdapter.show(state);
			}
		}*/
		return super.dispatchGenericMotionEvent(event);
	}

	/*private InputDeviceState getInputDeviceState(InputEvent event)
	{
		final int deviceId = event.getDeviceId();
		InputDeviceState state = mInputDeviceStates.get(deviceId);
		if (state == null)
		{
			final InputDevice device = event.getDevice();
			if (device == null)
			{
				return null;
			}
			state = new InputDeviceState(device);
			mInputDeviceStates.put(deviceId, state);

			Log.i(TAG, device.toString());
		}
		return state;
	}*/

	/**
	 * Tracks the state of joystick axes and game controller buttons for a particular
	 * input device for diagnostic purposes.
	 */
	/*private static class InputDeviceState
	{
		private final InputDevice		mDevice;
		private final int[]				mAxes;
		private final float[]			mAxisValues;
		private final SparseIntArray	mKeys;

		public InputDeviceState(InputDevice device)
		{
			mDevice = device;

			int numAxes = 0;
			for (MotionRange range : device.getMotionRanges())
			{
				if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0)
				{
					numAxes += 1;
				}
			}

			mAxes = new int[numAxes];
			mAxisValues = new float[numAxes];
			int i = 0;
			for (MotionRange range : device.getMotionRanges())
			{
				if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0)
				{
					numAxes += 1;
				}
				mAxes[i++] = range.getAxis();
			}

			mKeys = new SparseIntArray();
		}

		public InputDevice getDevice()
		{
			return mDevice;
		}

		public int getAxisCount()
		{
			return mAxes.length;
		}

		public int getAxis(int axisIndex)
		{
			return mAxes[axisIndex];
		}

		public float getAxisValue(int axisIndex)
		{
			return mAxisValues[axisIndex];
		}

		public int getKeyCount()
		{
			return mKeys.size();
		}

		public int getKeyCode(int keyIndex)
		{
			return mKeys.keyAt(keyIndex);
		}

		public boolean isKeyPressed(int keyIndex)
		{
			return mKeys.valueAt(keyIndex) != 0;
		}

		public boolean onKeyDown(KeyEvent event)
		{
			final int keyCode = event.getKeyCode();
			if (isGameKey(keyCode))
			{
				if (event.getRepeatCount() == 0)
				{
					final String symbolicName = KeyEvent.keyCodeToString(keyCode);
					mKeys.put(keyCode, 1);
					Log.i(TAG, mDevice.getName() + " - Key Down: " + symbolicName);
				}
				return true;
			}
			return false;
		}

		public boolean onKeyUp(KeyEvent event)
		{
			final int keyCode = event.getKeyCode();
			if (isGameKey(keyCode))
			{
				int index = mKeys.indexOfKey(keyCode);
				if (index >= 0)
				{
					final String symbolicName = KeyEvent.keyCodeToString(keyCode);
					mKeys.put(keyCode, 0);
					Log.i(TAG, mDevice.getName() + " - Key Up: " + symbolicName);
				}
				return true;
			}
			return false;
		}

		public boolean onJoystickMotion(MotionEvent event)
		{
			StringBuilder message = new StringBuilder();
			message.append(mDevice.getName()).append(" - Joystick Motion:\n");

			final int historySize = event.getHistorySize();
			for (int i = 0; i < mAxes.length; i++)
			{
				final int axis = mAxes[i];
				final float value = event.getAxisValue(axis);
				mAxisValues[i] = value;
				message.append("  ").append(MotionEvent.axisToString(axis)).append(": ");

				// Append all historical values in the batch.
				for (int historyPos = 0; historyPos < historySize; historyPos++)
				{
					message.append(event.getHistoricalAxisValue(axis, historyPos));
					message.append(", ");
				}

				// Append the current value.
				message.append(value);
				message.append("\n");
			}
			Log.i(TAG, message.toString());
			return true;
		}

		// Check whether this is a key we care about.
		// In a real game, we would probably let the user configure which keys to use
		// instead of hardcoding the keys like this.
		private static boolean isGameKey(int keyCode)
		{
			switch (keyCode)
			{
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
				case KeyEvent.KEYCODE_DPAD_CENTER:
				case KeyEvent.KEYCODE_SPACE:
					return true;
				default:
					return KeyEvent.isGamepadButton(keyCode);
			}
		}
	}*/

}