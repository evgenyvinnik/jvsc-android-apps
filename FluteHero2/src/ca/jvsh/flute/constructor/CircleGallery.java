package ca.jvsh.flute.constructor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import ca.jvsh.flute.R;

public class CircleGallery extends Gallery
{
	// 3 default constructors
	public CircleGallery(Context context)
	{
		super(context);
		initiateAdapter(context);
	}

	public CircleGallery(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initiateAdapter(context);
	}

	public CircleGallery(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initiateAdapter(context);
	}

	View	lastSelectedView	= null;

	private void initiateAdapter(Context context)
	{

		this.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				if (lastSelectedView != null)
					lastSelectedView.setLayoutParams(new Gallery.LayoutParams(300, 300));
				arg1.setLayoutParams(new Gallery.LayoutParams(350, 350));

				lastSelectedView = arg1;

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{

			}
		});

		setAdapter(new ImageAdapterCircleGallery(context));

		//To select the xSelected one -> 0 is the first.
		int xSelected = 0;
		//To make the view go to the middle of our 'endless' array
		setSelection(Integer.MAX_VALUE / 2 + (Integer.MAX_VALUE / 2) % 5 - 1 + xSelected);
	}

	public class ImageAdapterCircleGallery extends BaseAdapter
	{

		private Context		mContext;

		private Integer[]	mImageIds	= { R.drawable.a, R.drawable.b, R.drawable.c, R.drawable.d, R.drawable.e };

		public ImageAdapterCircleGallery(Context c)
		{
			mContext = c;

		}

		public int getCount()
		{
			return Integer.MAX_VALUE;
		}

		public Object getItem(int position)
		{
			return getPosition(position);
		}

		public long getItemId(int position)
		{
			return getPosition(position);
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			ImageView i = new ImageView(mContext);
			int height = i.getHeight();
			Log.d("ImageAdapterCircleGallery", "height = " + height);
			position = getPosition(position);
			i.setImageResource(mImageIds[position]);
			i.setLayoutParams(new Gallery.LayoutParams(300, 300));
			i.setScaleType(ImageView.ScaleType.FIT_XY);
			return i;
		}

		public int checkPosition(int position)
		{
			return getPosition(position);
		}

		int getPosition(int position)
		{
			if (position >= mImageIds.length)
			{
				position = position % mImageIds.length;
			}
			return position;
		}
	}
}
