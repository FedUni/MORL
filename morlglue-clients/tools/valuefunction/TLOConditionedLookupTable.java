// A modified version of the LookupTable to allow for conditioning of decisions based on the augmented state (ie including
// information about actual or expected rewards accumulated during the current episode prior to applying the thresholding operator).

package tools.valuefunction;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.LookupTable;

public class TLOConditionedLookupTable extends LookupTable implements ActionSelector 
{
    double thisStateValues[][];
    double conditioningValues[]; // array of dimensionality one less than number of objectives
    double thresholds[];
    int numThresholds; // will be numObjectives minus 1

    public TLOConditionedLookupTable( int numberOfObjectives, int numberOfActions, int numberOfStates, int initValue, 
    								double theseThresholds[]) 
    {
        super(numberOfObjectives, numberOfActions, numberOfStates, initValue); 
        thresholds = theseThresholds.clone();
        numThresholds = numberOfObjectives -1;
        thisStateValues = new double[numberOfActions][numberOfObjectives]; 
        conditioningValues = new double[numThresholds];
        for (int i=0; i<numThresholds; i++)
        {
        	conditioningValues[i] = 0.0;
        }
    }
    
       
    // for debugging purposes - print out conditioning and Q- values for all actions for the current state
    public void printCurrentStateValues(int state)
    {
    	// compile conditioning values and thresholds into a string
    	String conditioningString = "Conditioning values \t";
    	String thresholdingString = "Thresholds \t";
        for (int i=0; i<numThresholds; i++)
        {
        	conditioningString += conditioningValues[i] + "\t";
        	thresholdingString += thresholds[i] + "\t";
        }    	
    	getActionValues(state); // copy the action values into the 2D array thisStateValues
    	System.out.print("State\t" + state + "\t" + conditioningString +"\t"+ thresholdingString + "\tActions\t");
		for (int a=0; a<numberOfActions; a++)
		{
			System.out.print(a+"\t");
			for (int obj=0; obj<super.numberOfObjectives; obj++)
			{
				System.out.print(thisStateValues[a][obj]+"\t");
			}
		}  
    }
    
    // This is a bit of a hack to get around the fact that the structure of Rustam's lookup table doesn't map nicely
    // on to my TLO library functions. The whole Agent and ValueFunction structure of Rustam's code needs to be refactored at some point
    // Combine the state-action values and the conditioning Values together into thisStateValues
    private void getActionValues(int state)
    {
		for (int a=0; a<numberOfActions; a++)
		{
			for (int i=0; i<numThresholds; i++)
			{
				thisStateValues[a][i] = valueFunction.get(0)[a][state] + conditioningValues[i];
			}
			// the final objective doesn't need to be conditioned as no thresholding is applied to it
			thisStateValues[a][numThresholds] = valueFunction.get(numThresholds)[a][state];
		}
    }
    
    public void setConditioningValues(double values[])
    {
    	for (int i=0; i<numThresholds; i++)
    	{
    		conditioningValues[i] = values[i];
    	}
    }
    

    @Override
    public int chooseGreedyAction(int state) 
    {
    	getActionValues(state);
    	int greedy = TLO.greedyAction(thisStateValues, thresholds); 
    	return greedy;
    }
    
    // returns true if action is amongst the greedy actions for the specified
    // state, otherwise false
    public boolean isGreedy(int state, int action)
    {  
    	getActionValues(state);
    	int best = TLO.greedyAction(thisStateValues, thresholds); 
    	// this action is greedy if it is TLO-equal to the greedily selected action
    	return (TLO.compare(thisStateValues[action], thisStateValues[best], thresholds)==0);
    }
       
    // softmax selection based on tournament score (i.e. the number of actions which each action TLO-dominates)
    protected int softmaxTournament(double temperature, int state)
    {
    	int best = chooseGreedyAction(state); // as a side-effect this will also set up the Q-values array
    	double scores[] = TLO.getDominanceScore(thisStateValues,thresholds);
    	return Softmax.getAction(scores,temperature,best);
    }
    
    // softmax selection based on each action's additive epsilon score
    protected int softmaxAdditiveEpsilon(double temperature, int state)
    {
    	int best = chooseGreedyAction(state); // as a side-effect this will also set up the Q-values array
    	double scores[] = TLO.getInverseAdditiveEpsilonScore(thisStateValues,best);
    	return Softmax.getAction(scores,temperature,best);
    }    

    public double[] getThresholds() {
        return thresholds;
    }

    public void setThresholds(double[] thresholds) {
        this.thresholds = thresholds;
    }
    
     public void saveValueFunction(String theFileName) {
    	System.out.println(theFileName);
        for (int s = 0; s < numberOfStates; s++) {
            for (int a = 0; a < numberOfActions; a++) {
            	System.out.print("State "+s+"\tAction "+a+"\t");         	
            	for (int i = 0; i < numberOfObjectives; i++) {
                    System.out.print(valueFunction.get(i)[a][s] +"\t");
                }
            	System.out.println();
            }                
        }  	
        try {
            DataOutputStream DO = new DataOutputStream(new FileOutputStream(new File(theFileName)));
            for (int i = 0; i < numberOfObjectives; i++) {
                for (int a = 0; a < numberOfActions; a++) {
                    for (int s = 0; s < numberOfStates; s++) {
                        DO.writeDouble( valueFunction.get(i)[a][s] );
                    }
                }                
            }
            DO.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Problem saving value function to file: " + theFileName + " :: " + ex);
        } catch (IOException ex) {
            System.err.println("Problem writing value function to file:: " + ex);
        }
    }
    
    public void loadValueFunction(String theFileName) {
        try {
            DataInputStream DI = new DataInputStream(new FileInputStream(new File(theFileName)));
            for (int i = 0; i < numberOfObjectives; i++) {
                for (int a = 0; a < numberOfActions; a++) {
                    for (int s = 0; s < numberOfStates; s++) {
                        valueFunction.get(i)[a][s] = DI.readDouble();
                    }
                }                
            }
            DI.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Problem loading value function from file: " + theFileName + " :: " + ex);
        } catch (IOException ex) {
            System.err.println("Problem reading value function from file:: " + ex);
        }
    }
   
}