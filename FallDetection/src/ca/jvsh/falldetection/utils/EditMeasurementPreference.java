package ca.jvsh.falldetection.utils;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

/**
 * An {@link EditTextPreference} that is suitable for entering measurements.
 * It handles metric/imperial setting.
 * @author Levente Bagi
 */
public class EditMeasurementPreference extends EditTextPreference
{

	public EditMeasurementPreference(Context context)
	{
		super(context);
	}

	public EditMeasurementPreference(Context context, AttributeSet attr)
	{
		super(context, attr);
	}

	public EditMeasurementPreference(Context context, AttributeSet attr, int defStyle)
	{
		super(context, attr, defStyle);
	}

	protected void onAddEditTextToDialogView(View dialogView, EditText editText)
	{
		editText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		super.onAddEditTextToDialogView(dialogView, editText);
	}

	public void onDialogClosed(boolean positiveResult)
	{
		if (positiveResult)
		{
			try
			{
				Integer.valueOf(((CharSequence) (getEditText().getText())).toString());
			}
			catch (NumberFormatException e)
			{
				this.showDialog(null);
				return;
			}
		}
		super.onDialogClosed(positiveResult);
	}
}
