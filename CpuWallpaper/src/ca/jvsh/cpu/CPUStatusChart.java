/**
 * Copyright (C) 2009 SC 4ViewSoft SRL
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ca.jvsh.cpu;

import java.util.ArrayList;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

/**
 * Project status demo chart.
 */
public class CPUStatusChart
{
	
	final static String [] titles = new String[]
	{
			"User",
			"System",
			"Signal",
			"CPU & Signal"
	};
	static XYSeries userSeries = new XYSeries(titles[0]);
	static XYSeries systemSeries = new XYSeries(titles[1]);
	static XYSeries signalSeries = new XYSeries(titles[2]);
	
	static final int MAX_POINTS = 60; // 5 mins
	static XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
	static XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	static XYSeriesRenderer renderer1 = new XYSeriesRenderer();
	static XYSeriesRenderer renderer2 = new XYSeriesRenderer();
	static XYSeriesRenderer renderer3 = new XYSeriesRenderer();
	static boolean initialized = false;
	
	/**
	 * Returns the chart name.
	 * 
	 * @return the chart name
	 */
	public String getName()
	{
		return titles[3];
	}
	
	/**
	 * Returns the chart description.
	 * 
	 * @return the chart description
	 */
	public String getDesc()
	{
		return titles[3];
	}
	
	public void setLineDataToSeries(ArrayList<Integer> data, XYSeries series)
	{
		series.clear();
		int index = 0;
		for (int i = (data.size() - 1); i >= 0 && index < MAX_POINTS; i--)
		{
			series.add(index++, data.get(i).doubleValue());
		}
	}
	
	/**
	 * Create a chart view
	 * 
	 * @param context
	 *            the context
	 * @return the built intent
	 */
	public View createView(Context context, ArrayList<Integer> userdata, ArrayList<Integer> systemdata, ArrayList<Integer> signaldata)
	{
		while (dataset.getSeriesCount() > 0)
			dataset.removeSeries(0);
		
		setLineDataToSeries(userdata, userSeries);
		dataset.addSeries(userSeries);
		
		setLineDataToSeries(systemdata, systemSeries);
		dataset.addSeries(systemSeries);
		
		setLineDataToSeries(signaldata, signalSeries);
		dataset.addSeries(signalSeries);
		
		if (!initialized) initialize();
		
		// StringBuffer s= new StringBuffer();
		// for(int h=0;h<userSeries.getItemCount();h++)
		// s.append(userSeries.getY(h)).append(",");
		// Log.i("user data points", s.toString());
		return ChartFactory.getLineChartView(context, dataset, renderer);
	}
	
	private void initialize()
	{
		renderer1.setColor(Color.RED);
		renderer1.setFillBelowLine(true);
		renderer1.setFillBelowLineColor(0x44ff0000);
		renderer.addSeriesRenderer(renderer1);
		
		renderer2.setColor(Color.CYAN);
		renderer2.setFillBelowLine(true);
		renderer2.setFillBelowLineColor(0x4400ffff);
		renderer.addSeriesRenderer(renderer2);
		
		renderer3.setColor(Color.LTGRAY);
		renderer3.setLineWidth(0.6f);
		renderer.addSeriesRenderer(renderer3);
		
		// no point styles
		renderer.setChartTitle(titles[3]);
		renderer.setXTitle("Time (s) \u00BB");
		renderer.setYTitle("CPU / Signal (%) \u00BB");
		renderer.setXAxisMin(0);// 3x100= 300/ 5 mins
		renderer.setXAxisMax(MAX_POINTS);
		renderer.setYAxisMin(0);
		renderer.setYAxisMax(100);
		renderer.setAxesColor(Color.GRAY);
		renderer.setLabelsColor(Color.WHITE);
		
		renderer.setXLabels(6);
		renderer.setYLabels(10); // every 10%
		renderer.setShowGrid(true);
		// renderer.setAntialiasing(false);
		// renderer.setDisplayChartValues(true);
		initialized = true;
		
	}
	
}
