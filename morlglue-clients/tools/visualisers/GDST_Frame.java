package tools.visualisers;

import javax.swing.JFrame;
import java.awt.*;

public class GDST_Frame extends JFrame 
{
	GDST_Panel panel1;
	Scatter_Plot_Panel panel2;
	
    public GDST_Frame(int numCols, int numRows, int[] depths, int[] steps, int[] treasure) {
    	setLayout(new GridLayout(1,2));
        panel1 = new GDST_Panel(numCols, numRows, depths, treasure);
        add(panel1);
        panel2 = new Scatter_Plot_Panel("Treasure","Time", treasure, steps);
        add(panel2);
        setTitle("Generalised Deep Sea Treasure");
        setSize(700, 500);
        setLocation(620,20);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
    
    public void updateEnvironmentSettings(int numCols, int numRows, int[] depths, int[] steps, int[] treasure) {
        panel1.changeSettings(numCols, numRows, depths, treasure);
        panel2.updateData(treasure, steps);
        repaint();
    }  
    
    public void moveSubmarine(int col, int row)
    {
    	panel1.moveSubmarine(col, row);
    	repaint();
    }
	
}
