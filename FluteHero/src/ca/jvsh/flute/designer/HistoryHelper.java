package ca.jvsh.flute.designer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;

import ca.jvsh.flute.designer.StylesFactory;

import android.graphics.Bitmap;

public class HistoryHelper
{
	private final FluteDesigningSurface	mSurface;

	private State			mUndoState	= new State();
	private State			mRedoState	= new State();

	private boolean			isSwaped	= false;

	public HistoryHelper(FluteDesigningSurface surface)
	{
		mSurface = surface;
	}

	public void undo()
	{
		if (mRedoState.mBuffer == null || mUndoState.mBuffer == null)
		{
			return;
		}
		restoreState(mSurface.getBitmap(), isSwaped ? mRedoState : mUndoState);
		isSwaped = !isSwaped;
	}

	private void restoreState(Bitmap bitmap, State state)
	{
		Buffer byteBuffer = ByteBuffer.wrap(state.mBuffer);
		bitmap.copyPixelsFromBuffer(byteBuffer);
		StylesFactory.restoreState(state.stylesState);
	}

	public void saveState()
	{
		saveState(mSurface.getBitmap(), isSwaped ? mRedoState : mUndoState);
		isSwaped = !isSwaped;
	}

	private void saveState(Bitmap bitmap, State state)
	{
		state.mBuffer = new byte[bitmap.getRowBytes() * bitmap.getHeight()];
		Buffer byteBuffer = ByteBuffer.wrap(state.mBuffer);
		bitmap.copyPixelsToBuffer(byteBuffer);
		StylesFactory.saveState(state.stylesState);
	}

	private static class State
	{
		byte[]							mBuffer		= null;
		final HashMap<Integer, Object>	stylesState	= new HashMap<Integer, Object>();
	}
}
