package tools.visualisers;

import javax.swing.JFrame;

import java.awt.*;

public class Experiment_Frame extends JFrame 
{
	TimeSeriesGraphPanel graph;
	LegendPanel legend;
	
    public Experiment_Frame(String _yAxisLabel, int _maxX, int _maxY, double[][] _seriesData, 
    					int[] _seriesLength, String[] _seriesNames, Color[] _seriesColours) 
    {
    	setLayout(new GridLayout(1,2));
        graph = new TimeSeriesGraphPanel(_yAxisLabel, _maxX, _maxY, _seriesData, _seriesLength, _seriesColours);
        add(graph);
        legend = new LegendPanel(_seriesNames, _seriesColours);
        add(legend);
        setTitle("Experiment Summary");
        setSize(600, 500);
        setLocation(0,20);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
    
    public void updateData(double[][] _seriesData, int[] _seriesLength) 
    {
        graph.updateData(_seriesData, _seriesLength);
        repaint();
    }  

	
}
