package tools.valuefunction.interfaces;

import java.util.ArrayList;
import java.util.Random;
import org.rlcommunity.rlglue.codec.types.Reward;
import tools.valuefunction.Softmax;
import tools.valuefunction.TLO;

public abstract class LookupTable extends ValueFunction {
    
	private final boolean debugtrace = false;
    protected final int numberOfObjectives;
    protected final int numberOfActions;
    protected final int numberOfStates;
    protected Random r = null;

    // constants to label the different exploration strategies
	public static final int EGREEDY = 0;
	public static final int SOFTMAX_TOURNAMENT = 1;
	public static final int SOFTMAX_ADDITIVE_EPSILON = 2;
    protected int explorationStrategy = 0; // default is egreedy
    
    protected ArrayList<double[][]> valueFunction = null;
    protected double[] errors = null;

    public LookupTable( int numberOfObjectives, int numberOfActions, int numberOfStates, int initValue ) {
        this.numberOfObjectives = numberOfObjectives;
        this.numberOfActions = numberOfActions;
        this.numberOfStates = numberOfStates;
        r = new Random(499);
        
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
        
        abstract public int chooseGreedyAction(int state);
        
        // simple eGreedy selection
        private int eGreedy(double epsilon, int state)
        {
            if (r.nextDouble()<=epsilon)
                return r.nextInt(numberOfActions);
            else
                return chooseGreedyAction(state);
        }
        
        // softmax selection based on tournament score (i.e. the number of actions which each action TLO-dominates)
        abstract protected int softmaxTournament(double temperature, int state);
        
        // softmax selection based on each action's additive epsilon score
        abstract protected int softmaxAdditiveEpsilon(double temperature, int state);
        
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

        // Print the Q-values for all actions in the specified state, in CSV format
        public void printQValues(int state)
        {
            System.out.print("State " + state + ", ");
            for (int action=0; action<numberOfActions; action++)
            {
                System.out.print("[");
                for (int i = 0; i < numberOfObjectives; i++) 
                {
                    double[][] qValues = valueFunction.get(i);
                    System.out.print(qValues[ action ][ state ]+" ");
                } 
                System.out.print("], ");
            }
            System.out.println();
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
