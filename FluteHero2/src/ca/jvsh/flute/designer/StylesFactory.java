package ca.jvsh.flute.designer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ca.jvsh.flute.designer.Style;

public class StylesFactory
{

	public static final int				SIMPLE			= 0x1011;
	public static final int				ERASER			= 0x1012;

	private static Map<Integer, Style>	cache			= new HashMap<Integer, Style>();
	private static int					currentStyle	= SIMPLE;

	public static Style getStyle(int id)
	{
		if (!cache.containsKey(id))
		{
			cache.put(id, getStyleInstance(id));
		}
		currentStyle = id;
		return cache.get(id);
	}

	public static Style getCurrentStyle()
	{
		return getStyle(currentStyle);
	}

	public static void clearCache()
	{
		cache.clear();
	}

	private static Style getStyleInstance(int id)
	{
		switch (id)
		{

			case SIMPLE:
				return new SimpleStyle();
			case ERASER:
				return new EraserStyle();

			default:
				throw new RuntimeException("Invalid style ID");
		}
	}

	public static void saveState(HashMap<Integer, Object> state)
	{
		Collection<Style> values = cache.values();
		for (Style style : values)
		{
			style.saveState(state);
		}
	}

	public static void restoreState(HashMap<Integer, Object> state)
	{
		Set<Integer> keySet = state.keySet();
		for (int id : keySet)
		{
			Style style = getStyle(id);
			style.restoreState(state);
		}
	}
}
