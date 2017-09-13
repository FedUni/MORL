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

public class TLO_LookupTable extends LookupTable implements ActionSelector 
{
	// constants to label the different exploration strategies
	public static final int EGREEDY = 0;
	public static final int SOFTMAX_TOURNAMENT = 1;
	public static final int SOFTMAX_ADDITIVE_EPSILON = 2;
	
    
    Random r = null;
    int explorationStrategy = 0; // default is egreedy
    double thresholds[];
    double thisStateValues[][];

    public TLO_LookupTable( int numberOfObjectives, int numberOfActions, int numberOfStates, int initValue, double thresholds[]) 
    {
        super(numberOfObjectives, numberOfActions, numberOfStates, initValue);
        r = new Random(499);    
        this.thresholds = thresholds;
        thisStateValues = new double[numberOfActions][numberOfObjectives];
    }
    
    // set the exploration strategy
    public void setExplorationStrategy(int ex)
    {
    	explorationStrategy = ex;
    }
    
    // returns a String representing the exploration strategy
    public static String explorationStrategyToString(int ex)
    {
    	switch (ex)
    	{
    		case EGREEDY: return "eGreedy";
    		case SOFTMAX_TOURNAMENT: return "softmax_t";
    		case SOFTMAX_ADDITIVE_EPSILON: return "softmax_+E";
    		default: return "Unknown";
    	}
    }
    
    // for debugging purposes - print out Q- values for all actions for the current state
    public void printCurrentStateValues(int state)
    {
    	getActionValues(state); // copy the action values into the 2D array thisStateValues
    	System.out.print("State " + state + ": ");
		for (int a=0; a<numberOfActions; a++)
		{
			System.out.print("a"+a+" (");
			for (int obj=0; obj<numberOfObjectives; obj++)
			{
				System.out.print(thisStateValues[a][obj]+",");
			}
			System.out.print(") ");
		}  
		System.out.println();
    }
    
    // This is a bit of a hack to get around the fact that the structure of Rustam's lookup table doesn't map nicely
    // on to my TLO library functions. The whole Agent and ValueFunction structure of Rustam's code needs to be refactored at some point
    // Copies the q-values for the current state into the 2 dimensional arraythisStateValues index by [action][objective]
    private void getActionValues(int state)
    {
    	for (int obj=0; obj<numberOfObjectives; obj++)
    	{
    		double[][] thisObjQ = valueFunction.get(obj);
    		for (int a=0; a<numberOfActions; a++)
    		{
    			thisStateValues[a][obj] = thisObjQ[a][state];
    		}
    	}    	
    }

    @Override
    public int chooseGreedyAction(int state) 
    {
    	getActionValues(state);
    	return TLO.greedyAction(thisStateValues, thresholds); 
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
    
    // simple eGreedy selection
    private int eGreedy(double epsilon, int state)
    {
    	if (r.nextDouble()<=epsilon)
    		return r.nextInt(numberOfActions);
    	else
    		return chooseGreedyAction(state);
    }
    
    // softmax selection based on tournament score (i.e. the number of actions which each action TLO-dominates)
    private int softmaxTournament(double temperature, int state)
    {
    	int best = chooseGreedyAction(state); // as a side-effect this will also set up the Q-values array
    	double scores[] = TLO.getDominanceScore(thisStateValues,thresholds);
    	return Softmax.getAction(scores,temperature,best);
    }
    
    // softmax selection based on each action's additive epsilon score
    private int softmaxAdditiveEpsilon(double temperature, int state)
    {
    	int best = chooseGreedyAction(state); // as a side-effect this will also set up the Q-values array
    	double scores[] = TLO.getInverseAdditiveEpsilonScore(thisStateValues,best);
    	return Softmax.getAction(scores,temperature,best);
    }
    
    // This will call one of a variety of different exploration approaches
    public int choosePossiblyExploratoryAction(double parameter, int state)
    {
    	if (explorationStrategy==EGREEDY)
    		return eGreedy(parameter, state);
    	else if (explorationStrategy==SOFTMAX_TOURNAMENT)
    		return softmaxTournament(parameter, state);
    	else if (explorationStrategy==SOFTMAX_ADDITIVE_EPSILON)
    		return softmaxAdditiveEpsilon(parameter, state);
    	else
    	{
    		System.out.println("Error - undefined exploration strategy" + explorationStrategy);
    		return -1; // should cause a crash to halt proceedings
    	}
    	
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