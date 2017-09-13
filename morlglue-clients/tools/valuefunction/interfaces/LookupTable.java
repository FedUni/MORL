package tools.valuefunction.interfaces;

import java.util.ArrayList;
import org.rlcommunity.rlglue.codec.types.Reward;

public abstract class LookupTable extends ValueFunction {
    
	private final boolean debugtrace = false;
    protected final int numberOfObjectives;
    protected final int numberOfActions;
    protected final int numberOfStates;
    
    protected ArrayList<double[][]> valueFunction = null;
    protected double[] errors = null;

    public LookupTable( int numberOfObjectives, int numberOfActions, int numberOfStates, int initValue ) {
        this.numberOfObjectives = numberOfObjectives;
        this.numberOfActions = numberOfActions;
        this.numberOfStates = numberOfStates;
        
        valueFunction = new ArrayList<>();
        for(int i=0 ; i<numberOfObjectives ; i++) {
            double[][] array = new double[numberOfActions][numberOfStates];
            valueFunction.add( array );
        }
        
        if( initValue != 0 ) {
            for (int i = 0; i < numberOfObjectives; i++) {
                double[][] array = valueFunction.get(i);
                for (int j = 0; j < numberOfActions; j++) {
                    for (int k = 0; k < numberOfStates; k++) {
                        array[j][k] = initValue;
                    }
                }
            }
        }
        errors = new double[numberOfObjectives];
        
    }
    
    @Override
    public void calculateErrors(int action, int previousState, int greedyAction, int newState, double gamma, Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {
            double[][] qValues = valueFunction.get(i);
            
            double thisQ = qValues[ action ][ previousState ];                        
            double maxQ = qValues[ greedyAction ][ newState ];
            
            double err = getRewardForThisObjective(reward, i) + gamma * maxQ - thisQ;
            
            errors[i] = err;
            
            if (debugtrace && i==0)         System.out.println("\t\tCalc errors - prev state, action " + previousState + ", " + action + " = " + thisQ + " new state, action " + newState + ", " + greedyAction + " = " + maxQ + "-> " + err);
        }    
    }
    @Override
    public void calculateTerminalErrors(int action, int previousState, double gamma, Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {
            double[][] qValues = valueFunction.get(i);
            
            double thisQ = qValues[ action ][ previousState ];                        
            
            errors[i] =  getRewardForThisObjective(reward, i) - thisQ;
            if (debugtrace && i==0)         System.out.println("\t\tCalc terminal errors - prev state, action " + previousState + ", " + action + " = " + thisQ + "-> " + errors[i]);

        }                
    }
    
    /* I must have added these when implementing MOX, but it doesn't make sense for them to be in this class, so I have moved this into the MOX/EOVF specific classes instead
    @Override
    public void calculateErrors(int action, int previousState, int greedyAction, int newState, double gamma[], Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {
            double[][] qValues = valueFunction.get(i);
            
            double thisQ = qValues[ action ][ previousState ];                        
            double maxQ = qValues[ greedyAction ][ newState ];
            
            double err = getRewardForThisObjective(reward, i) + gamma[i] * maxQ - thisQ;
            
            errors[i] = err;
            
            if (debugtrace && i==0)         System.out.println("\t\tCalc errors - prev state, action " + previousState + ", " + action + " = " + thisQ + " new state, action " + newState + ", " + greedyAction + " = " + maxQ + "-> " + err);
        }    
    }
    @Override
    public void calculateTerminalErrors(int action, int previousState, double gamma[], Reward reward) {
        for (int i = 0; i < numberOfObjectives; i++) {
            double[][] qValues = valueFunction.get(i);
            
            double thisQ = qValues[ action ][ previousState ];                        
            
            errors[i] =  getRewardForThisObjective(reward, i) - thisQ;
            if (debugtrace && i==0)         System.out.println("\t\tCalc terminal errors - prev state, action " + previousState + ", " + action + " = " + thisQ + "-> " + errors[i]);

        }                
    }
    */

    protected double getRewardForThisObjective(Reward reward, int i) {
        return reward.doubleArray[i];
    }
    
    @Override
    public void update(int action, int state, double lambda, double alpha) {
        //System.out.println("\t\tUpdate - state,action " + state + ", " + action);       
        for (int i = 0; i < numberOfObjectives; i++) {
            double[][] qValues = valueFunction.get(i);
            double thisQ = qValues[ action ][ state ];

            double newQ = thisQ + alpha * ( lambda * errors[i] );
            qValues[ action ][ state ] = newQ;
            //System.out.println(i + ": " + thisQ + " -> " + newQ);
        }        
    }
    
    @Override
    public double[] getQValues(int action, int state) {
        double[] result = new double[ numberOfObjectives ];
        for (int i = 0; i < numberOfObjectives; i++) {
            double[][] qValues = valueFunction.get(i);
            result[i] = qValues[ action ][ state ];
        }
        return result;
    }
    
    public int getNumberOfObjectives() {
        return numberOfObjectives;
    }

    public int getNumberOfActions() {
        return numberOfActions;
    }

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public ArrayList<double[][]> getValueFunction() {
        return valueFunction;
    }
    
    // resets all table values to 0
    public void resetQValues(double initValue[])
    {
    	//System.out.println("reset q values");
        for (int i = 0; i < numberOfObjectives; i++) {
        	//System.out.println(i + ": " + initValue[i]);
            double[][] array = valueFunction.get(i);
            for (int j = 0; j < numberOfActions; j++) {
                for (int k = 0; k < numberOfStates; k++) {
                    array[j][k] = initValue[i];
                }
            }
        }   	
    }
    
    
    
}
