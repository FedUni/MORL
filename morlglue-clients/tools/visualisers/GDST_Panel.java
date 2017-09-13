package tools.visualisers;

import javax.swing.JPanel;
import java.awt.*;


public class GDST_Panel extends JPanel 
{
	private int numCols, numRows;
	private int[] depths;
	private int[] treasure;
	private int submarineColumn, submarineRow = 0;
	
	public GDST_Panel(int _numCols, int _numRows, int[] _depths, int[] _treasure)
	{
		numCols = _numCols;
		numRows = _numRows;
		depths = _depths;
		treasure = _treasure;
	}
	
	public void changeSettings(int _numCols, int _numRows, int[] _depths, int[] _treasure)
	{
		numCols = _numCols;
		numRows = _numRows;
		depths = _depths;
		treasure = _treasure;
	}
	
    public void moveSubmarine(int _col, int _row)
    {
    	submarineColumn = _col;
    	submarineRow = _row;
    }
	
	private void drawSubmarine(Graphics2D g2d, int cellWidth, int cellHeight)
	{
		int xOffset = cellWidth/6;
		int yOffset = Math.round(cellHeight*0.4f);
		int x = submarineColumn * cellWidth + xOffset;
		int y = submarineRow * cellHeight + yOffset;
		g2d.setColor(Color.DARK_GRAY);
		//g2d.fillRect(x,y,cellWidth-2*xOffset,cellHeight-2*yOffset);
		g2d.fillOval(x,y,cellWidth-2*xOffset,Math.round(cellHeight*0.4f));
		g2d.fillRect(x+Math.round(cellWidth*0.35f), y-Math.round(cellHeight*0.1f), Math.round(cellWidth*0.15f), Math.round(cellHeight*0.4f));
		g2d.fillRect(x+Math.round(cellWidth*0.4f), y-Math.round(cellHeight*0.25f), Math.round(cellWidth*0.03f), Math.round(cellHeight*0.4f));
		g2d.fillRect(x+Math.round(cellWidth*0.4f), y-Math.round(cellHeight*0.25f), Math.round(cellWidth*0.1f), Math.round(cellHeight*0.05f));
	}
	
    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        int cellWidth = getWidth()/numCols;
        int cellHeight = getHeight()/numRows;
        int x=0;
        for (int col=0; col<numCols; col++)
        {
        	int y = 0;
        	for (int row=0; row<numRows; row++)
        	{
        		// fill cell with appropriate colour
        		if (row<depths[col])
        			g2d.setColor(Color.cyan);
        		else if (row==depths[col])
        			g2d.setColor(Color.yellow); 
        		else
        			g2d.setColor(Color.black);
        		g2d.fillRect(x,y,cellWidth,cellHeight);
        		// draw border in black
        		g2d.setColor(Color.black); 
        		g2d.drawRect(x,y,cellWidth,cellHeight);
        		if (row==depths[col])
        		{
        			g2d.drawString(""+treasure[col], x+cellWidth/5, Math.round(y+cellHeight*0.7));
        		}
        		y += cellHeight;
        	}
        	x += cellWidth;
        }
        drawSubmarine(g2d,cellWidth,cellHeight);
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        doDrawing(g);
    }

}
