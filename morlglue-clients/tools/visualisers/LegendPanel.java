// Simple class to display a legend alongside a time-series graph
// Displays the name and colour of each series
// Written by Peter Vamplew Sept 2017 as part of the demonstration of MORL_Glue

package tools.visualisers;

import javax.swing.JPanel;

import java.awt.*;


public class LegendPanel extends JPanel 
{
	private String[] seriesNames;
	private Color[] seriesColours;
	
	public LegendPanel(String[] _seriesNames, Color[] _seriesColours)
	{
		seriesNames = _seriesNames;
		seriesColours = _seriesColours;
		setSize(150,400);
	}
	

    @Override
    public void paintComponent(Graphics g) 
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int height = getHeight();
        int width = getWidth();
        int heightGap = height / (2 + seriesNames.length);
        int y = heightGap;
        for (int i=0; i<seriesNames.length; i++)
        {
        	g2d.setStroke(new BasicStroke(4));
        	g2d.setColor(Color.BLACK);
            g2d.drawString(seriesNames[i], 10, y);
        	g2d.setColor(seriesColours[i]);
            g2d.drawLine(80,y-5, (int)(width*0.9),y-5);
            y += heightGap;
        }
    }

}
