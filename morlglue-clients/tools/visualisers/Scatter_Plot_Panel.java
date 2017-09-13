// Simple implementation of a 2D scatter plot
// Used for visualisation of 2D Pareto fronts in MORL_Glue
// Written by Peter Vamplew, August 2017

package tools.visualisers;

import javax.swing.JPanel;
import java.awt.*;

public class Scatter_Plot_Panel extends JPanel 
{
	String xLabel, yLabel;
	int xValues[], yValues[];
	int xMin, xMax, xRange, yMin, yMax, yRange;
	
	public Scatter_Plot_Panel(String _xLabel, String _yLabel, int[] _xValues, int[] _yValues)
	{
		xLabel = _xLabel;
		yLabel = _yLabel;
		updateData(_xValues, _yValues);
	}
	
	public void updateData(int[] _xValues, int[] _yValues)
	{
		xValues = _xValues;
		yValues = _yValues;
		xMin = xValues[0]; xMax = xValues[0];
		for (int i=0; i<xValues.length; i++)
		{
			if (xValues[i]< xMin) xMin = xValues[i];
			if (xValues[i]> xMax) xMax = xValues[i];	
		}
		xRange = xMax - xMin;
		yMin = yValues[0]; yMax = yValues[0];
		for (int i=0; i<yValues.length; i++)
		{
			if (yValues[i]< yMin) yMin = yValues[i];
			if (yValues[i]> yMax) yMax = yValues[i];			
		}
		yRange = yMax - yMin;
	}
	
	
    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        
        int height = getHeight();
        int width = getWidth();
        int xOrigin = width/10;
        int yOrigin = height - height /10;
        int xEnd = width - width / 20;
        int yEnd = height / 10;
        int xScale = xEnd - xOrigin;
        int yScale = yEnd - yOrigin;
        
        // draw axes and labels
        g2d.setColor(Color.black);
        g2d.drawLine(xOrigin,yOrigin,xEnd,yOrigin);
        g2d.drawLine(xOrigin,yOrigin,xOrigin,yEnd);
        g2d.drawString(xLabel, width/2, height - height/20);
        g2d.drawString(""+xMin, xOrigin, height - height/20);   
        g2d.drawString(""+xMax, xEnd-width/20, height - height/20);         
        g2d.drawString(yLabel, width/20, height/20);
        g2d.drawString(""+yMin, width/20, yOrigin); 
        g2d.drawString(""+yMax, width/20, yEnd);  
        // drawPoints
        float dotSize = width/50.0f;
        int intDotSize = Math.round(dotSize);
        int arcSize = Math.round(dotSize/3);
        g2d.setColor(Color.red);
        for (int i=0; i<xValues.length; i++)
        {
        	int x = Math.round(((float)(xValues[i]-xMin))/xRange*xScale+xOrigin-dotSize/2);
        	int y = Math.round(((float)(yValues[i]-yMin))/yRange*yScale+yOrigin-dotSize/2);       	
        	g2d.fillRect(x,y,intDotSize,intDotSize);
        }
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        doDrawing(g);
    }

}
