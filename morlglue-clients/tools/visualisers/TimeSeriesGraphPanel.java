// Simple implementation of multiple single-variate time-series graphs
// Written by Peter Vamplew, Sept 2017

package tools.visualisers;

import javax.swing.JPanel;

import java.awt.*;

public class TimeSeriesGraphPanel extends JPanel 
{
	String yLabel;
	int xMax, yMax;
	int xMin, yMin = 0;
	double[][] seriesData;
	int[] seriesLength;
	Color[] seriesColours;
	
	public TimeSeriesGraphPanel(String _yAxisLabel, int _xMax, int _yMax, double[][] _seriesData, int[] _seriesLength, Color[] _seriesColours)
	{
		yLabel = _yAxisLabel;
		xMax = _xMax;
		yMax = _yMax;
		seriesColours = _seriesColours;
		updateData(_seriesData, _seriesLength);
	}
	
	public void updateData(double[][] _seriesData, int[] _seriesLength)
	{
		seriesData = _seriesData;
		seriesLength = _seriesLength;
        for (int i=0; i<seriesData.length; i++)
        {
        	if (seriesLength[i]>xMax)
        	{
        		xMax = seriesLength[i];
        	}
        }
	}
	
	
    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        
        int height = getHeight();
        int width = getWidth();
        int xOrigin = width/10;
        int yOrigin = height - height /10;
        int xEnd = width - width / 20;
        int yEnd = height / 10;
        int xRange = xMax - xMin;
        int yRange = yMax - yMin;
        int xScale = xEnd - xOrigin;
        int yScale = yEnd - yOrigin;
        
        // draw axes and labels
        g2d.setColor(Color.black);
        g2d.drawLine(xOrigin,yOrigin,xEnd,yOrigin);
        g2d.drawLine(xOrigin,yOrigin,xOrigin,yEnd);
        g2d.drawString("Batches", width/2, height - height/20);
        g2d.drawString(""+"0", xOrigin, height - height/20);   
        g2d.drawString(""+xMax, xEnd-width/20, height - height/20);         
        g2d.drawString(yLabel, width/20, height/20);
        g2d.drawString(""+"0", width/20, yOrigin); 
        g2d.drawString(""+yMax, width/20, yEnd-5);  
        // draw series
        for (int i=0; i<seriesData.length; i++)
        {
           	g2d.setStroke(new BasicStroke(4));
        	g2d.setColor(seriesColours[i]);
        	for (int j=0; j<seriesLength[i]-1; j++)
        	{
            	int x = Math.round(((float)(j-xMin))/xRange*xScale+xOrigin);
            	int y = Math.round(((float)(seriesData[i][j]-yMin))/yRange*yScale+yOrigin);
            	int x2 = Math.round(((float)(j+1-xMin))/xRange*xScale+xOrigin);
            	int y2 = Math.round(((float)(seriesData[i][j+1]-yMin))/yRange*yScale+yOrigin); 
            	g2d.drawLine(x, y, x2, y2);
        	}
        }
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        doDrawing(g);
    }

}
