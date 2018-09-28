package ca.jvsh.networkbenchmark;

import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;

public class IPAddressKeyListener extends NumberKeyListener
{

	private char[]						mAccepted;
	private static IPAddressKeyListener	sInstance;

	@Override
	protected char[] getAcceptedChars()
	{
		return mAccepted;
	}

	/**
	 * The characters that are used.
	 * 
	 * @see KeyEvent#getMatch
	 * @see #getAcceptedChars
	 */
	private static final char[]	CHARACTERS	= new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' };

	private IPAddressKeyListener()
	{
		mAccepted = CHARACTERS;
	}

	/**
	 * Returns a IPAddressKeyListener that accepts the digits 0 through 9, plus the dot
	 * character, subject to IP address rules: the first character has to be a digit, and
	 * no more than 3 dots are allowed.
	 */
	public static IPAddressKeyListener getInstance()
	{
		if (sInstance != null)
			return sInstance;

		sInstance = new IPAddressKeyListener();
		return sInstance;
	}

	/**
	 * Display a number-only soft keyboard.
	 */
	public int getInputType()
	{
		return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
	}

	/**
	 * Filter out unacceptable dot characters.
	 */
	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
			int dend)
	{
		if (end > start)
		{
			String destTxt = dest.toString();
			String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
			if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?"))
			{
				return "";
			}
			else
			{
				String[] splits = resultingTxt.split("\\.");
				for (int i = 0; i < splits.length; i++)
				{
					if (Integer.valueOf(splits[i]) > 255)
					{
						return "";
					}
				}
			}
		}
		return null;

	}
}
