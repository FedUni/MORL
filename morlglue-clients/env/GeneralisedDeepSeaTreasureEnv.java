// Implements a generalised form of the Deep Sea Treasure MORL benchmark problem as originally described in Vamplew et al (2011).
// Instances of the benchamrk can be created varying in terms of the size and structure of the state space, the level of stochasticity in
// both the transitions and the rewards, and the shape of the front defined by the rewards.
// To create, call the main method passing in an array of Strings specifying the following in this order:
// - the width of the environment (number of columns) - a positive int
// - the starting depth of the sea-bed - a positive int (may have some effect on different exploration strategies)
// - the minimum depth difference between neighbouring columns - int >= 0
// - the maximum depth difference between neighbouring columns - int >= 0
// - transition noise - the probability that an action will be taken at random rather than as specified by the agent - double between 0 and 1 inclusive
// - reward noise - the standard variation of Gaussian noise to be added to reward values on each time-step
// - front shape - linear, concave, convex or mixed (use the static constants provided by this class)
// Written by Peter Vamplew August 2017

package env;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

import java.util.Random;
import java.awt.*;

import javax.swing.*;

import tools.visualisers.*;
import tools.MDP.*;

public class GeneralisedDeepSeaTreasureEnv implements EnvironmentInterface
{  
	// static ints used in defining the desired shape of the Pareto front defined by the treasures
	public static final int LINEAR = 0;
	public static final int CONCAVE = 1;
	public static final int CONVEX = 2;
	public static final int MIXED = 3;
	
	// define the structure of the environment, and the treasure rewards
    private int depths[];
    private int steps[]; // stores the min number of steps required to reach the treasure in each column
    private int treasure[];
    private int numRows;
    private int numCols = 10;
    private int frontShape;
    private TransitionList transitionFunction[][]; // stores a list of successor states and probabilities for each state, action pair
    private double transitionNoise;
    private double rewardNoise;
    private String taskSpecString; // used to return details of state size and Pareto set
    // define the ordering of the objectives
    private final int TREASURE = 0;
    private final int TIME = 1;
    private final int MIN_TREASURE = 1;
    private final int MAX_TREASURE = 1000;
    // location of the submarine within the environment
    private int agentRow;
    private int agentCol;
    // random number generator
    private Random r;
    // variables and objects related to the visual display of the environment
    private GDST_Frame frame;
	
    private GeneralisedDeepSeaTreasureEnv(int _width, int _minDepth, int _minVerticalStep, int _maxVerticalStep, double _transitionNoise, 
    										double _rewardNoise, int _frontShape, int _seed)
    {
    	constructEnvironment(_width, _minDepth, _minVerticalStep, _maxVerticalStep, _transitionNoise, _rewardNoise, _frontShape, _seed);
    }
    
    // sets up the properties of the environment based on the provided parameters
    private void constructEnvironment(int _width, int _minDepth, int _minVerticalStep, int _maxVerticalStep, double _transitionNoise, 
			double _rewardNoise, int _frontShape, int _seed)
	{
    	r = new Random(_seed);
    	// set up the structure of the environment
    	numCols = _width;
    	depths = new int[numCols];
    	steps = new int[numCols];
    	depths[0] = _minDepth;
    	steps[0] = _minDepth;
    	int stepRange = _maxVerticalStep - _minVerticalStep + 1;
    	for (int col=1; col<numCols; col++)
    	{
    		depths[col] = depths[col-1] + r.nextInt(stepRange) + _minVerticalStep;
    		steps[col] = col + depths[col];
    	}
    	numRows = depths[numCols-1]+1;
    	frontShape = _frontShape;
    	setTreasure();
    	transitionNoise = _transitionNoise;
    	rewardNoise = _rewardNoise;
    	setTransitionFunction();
    	// construct a String to return or include in the TaskSpec to specify the 
    	// number of states and also the true Pareto front, if we can calculate it
    	taskSpecString = (numRows * numCols) + " ";
    	if (transitionNoise==0.0) // can only easily find the front if the envt is not noisy
    	{
    		taskSpecString += numCols; // specify number of Pareto set points
    		for (int col=0; col<numCols; col++)
    		{
    			taskSpecString += " " + treasure[col] + " " + steps[col];
    		}
    	}
    	else // use 0 to indicate Pareto set isn't known
    	{
    		taskSpecString += " " + 0;
    	}
    	// set up the display
    	if (frame==null)
    		frame = new GDST_Frame(numCols, numRows, depths, steps, treasure);
    	else
    		frame.updateEnvironmentSettings(numCols, numRows, depths, steps, treasure);
	}
    
    private void setTreasure()
    {
    	treasure = new int[numCols];
    	treasure[0] = MIN_TREASURE;
    	treasure[numCols-1] = MAX_TREASURE;
    	float stepsRange = steps[numCols-1] - steps[0];
    	float treasureRange = MAX_TREASURE - MIN_TREASURE;
    	for (int col=1; col<numCols-1; col++)
    	{
    		// start by generating a linear set of values, then add/subtract from treasure to get different shapes
    		float ratio = (steps[col]-steps[0])/(float)stepsRange;
    		float adjustmentFactor = ratio * (1-ratio);
			treasure[col] = Math.round(ratio*treasureRange+MIN_TREASURE);
    		if (frontShape==CONVEX) // increase treasure values to give a convex front
    		{
    			treasure[col] += adjustmentFactor * treasureRange;
    		}
    		else if (frontShape==CONCAVE) // decrease treasure values to give a convex front
    		{
    			treasure[col] -= adjustmentFactor * treasureRange;
    		}
    		else if (frontShape==MIXED) // only change some points to get a mixed front, possibly with some dominated points
    		{
    			treasure[col] += adjustmentFactor * treasureRange * (r.nextDouble()*2-1);
    		}
    	}
		// once finished, set steps to -ve value to assist in charting
    	for (int col=0; col<numCols; col++)
    	{
    		steps[col] = -steps[col];
    	}
    }
    
    private void setTransitionFunction()
    {
    	double probOfEachActionUnderNoise = transitionNoise / 4;
    	transitionFunction = new TransitionList[numRows*numCols][4];
    	int nextS[] = new int[4];
    	for (int col=0; col<numCols; col++)
    	{
    		for (int row=0; row<numRows; row++)
    		{
    			int stateNum = getState(col, row);
    			// for each state determine the deterministic successor state
    			for (int action = 0; action<4; action++)
    			{
    				nextS[action] = getDeterministicTransition(col, row, action);
    			}
    			// we know the possible successor states, so can set up the TransitionLists for this state
    			for (int action = 0; action<4; action++)
    			{
    				transitionFunction[stateNum][action] = new TransitionList();
    				transitionFunction[stateNum][action].add(nextS[action], 1 - transitionNoise); 
    				for (int otherAction = 0; otherAction<4; otherAction++)
    				{
    					transitionFunction[stateNum][action].add(nextS[otherAction], probOfEachActionUnderNoise);
    				} 	
    				//System.out.println("\t" + stateNum + "\t" + action + "\t" + transitionFunction[stateNum][action]);
    			}			
    		}
    	}
    }
    
    private int getDeterministicTransition(int col, int row, int theAction)
    {
        int newRow = row;
        int newCol = col;

        if (row>depths[col]) // ie this is an unreachable state
        {
        	// just return the same state we are in
        	return col * numRows + row;
        }
        //  0 = go right
        if (theAction == 0) 
        {
            newCol++;
            if (newCol > numCols-1) {
                newCol--;
            }
        }
        //  1 = go left
        if (theAction == 1) 
        {
            newCol--;
            if (newCol < 0) 
            {
                newCol = 0;
            } else if( !isValid(newRow,newCol) ) // have to ensure the agent doesn't move back into the seabed underneath the treasures
            {
                newCol++;
            }
        }
        //  2 = go down
        if (theAction == 2) 
        {
            newRow++;
            //no need to check if row number is bigger than the number of rows, because if treasure is found the episode will be terminated
        }
        //  3 = go up
        if (theAction == 3) {
            newRow--;
            if (newRow < 0) {
                newRow = 0;
            }
        }
        return newCol * numRows + newRow;  	
    }
    
    public String env_init() 
    {
        //initialize the problem - starting position is always fixed, top-left corner of the world
        this.agentRow = 0;
        this.agentCol = 0;
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setEpisodic();
        //Specify that there will be this number of observations
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, numRows * numCols - 1));              
        //Specify that there will be an integer action [0,3]
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 3));
        //Specify that there will this number of objectives
        theTaskSpecObject.setNumOfObjectives(2);
        // return the Pareto set, if it is known (empty String otherwise)
        theTaskSpecObject.setExtra(taskSpecString);
        //Convert specification object to a string
        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);
        return taskSpecString;
    }
    
    // Setup the environment for the start of a new episode
    public Observation env_start() {
        this.agentRow = 0;
        this.agentCol = 0;       
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, getState(agentCol, agentRow));
        // update display
        frame.moveSubmarine(agentCol, agentRow);
        return theObservation;
    }
    
    // Execute the specified action, update environmental state and return the reward and new observation
    public Reward_observation_terminal env_step(Action action) 
    {
        updatePosition( action.getInt(0) );
        // set up new Observation
        Reward_observation_terminal RewardObs = new Reward_observation_terminal();
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, getState(agentCol,agentRow));
        RewardObs.setObservation(theObservation);
        RewardObs.setTerminal(isTerminal());
        // setup new rewards, add noise as required
        Reward rewards = new Reward(0,2,0);
        double[] rewardsArray = getRewards(agentCol,agentRow);
        rewards.setDouble(0, rewardsArray[0] * (1+r.nextGaussian()*rewardNoise));
        rewards.setDouble(1, rewardsArray[1] * (1+r.nextGaussian()*rewardNoise));
        RewardObs.setReward(rewards);
        return RewardObs;
    }

    public void env_cleanup() 
    {
    	//starting position is always fixed, top-left corner of the world
    	this.agentRow = 0;
    	this.agentCol = 0;
    }

    public String env_message(String message) 
    {
    	System.out.println("GDST received message: " + message);
        if (message.startsWith("change_settings"))
        {
            System.out.print("DSTG settings changed.");
        	String[] args = message.split(" ");
	    	// parse generalised constructor arguments
	    	int _width = Integer.parseInt(args[1]);
	    	int _minDepth = Integer.parseInt(args[2]);
	    	int _minVerticalStep = Integer.parseInt(args[3]);
	    	int _maxVerticalStep = Integer.parseInt(args[4]);
	    	double _transitionNoise = Double.parseDouble(args[5]);
	    	double _rewardNoise = Double.parseDouble(args[6]);
	    	int _frontShape = Integer.parseInt(args[7]);
	    	int _seed = Integer.parseInt(args[8]);
	    	constructEnvironment(_width, _minDepth, _minVerticalStep, _maxVerticalStep, _transitionNoise, _rewardNoise, _frontShape, _seed);
	    	return taskSpecString;
        }
        else
        {
        	throw new UnsupportedOperationException(message + " is not supported by DST environment.");
        }
    }
    
    // convert the agent's current position into a state index
    public int getState(int col, int row) 
    {
        return col * numRows + row;
    }
    
    
    // update the agent's position within the environment based on the specified action
    public void updatePosition(int theAction) 
    {
        int state = getState(agentCol,agentRow);
        int nextState = transitionFunction[state][theAction].getNextState(r);
        agentRow = nextState % numRows;
        agentCol = nextState / numRows;  
        // update display
        frame.moveSubmarine(agentCol, agentRow);
    }
    
    
    // Returns the non-noisy reward for the specified column and row
    public double[] getRewards(int col, int row) 
    {
        double objs[] = new double[2];
        objs[TIME] = -1; // time reward is -1 on all time-steps
        objs[TREASURE] = 0;
        //if found treasure
        if (row == depths[col]) {
            objs[TREASURE] = treasure[col];
        }
        return objs;
    }


    public boolean isTerminal() {
        //if found treasure
        if (agentRow == depths[agentCol]) {
            return true;
        } else return false;

    }

    // the agent can not be legally positioned below the depth of the treasure for its current column - return false if this occurs
    private boolean isValid(int row, int col) 
    {
        boolean valid = true;
        if (row > depths[col]) 
        {
            valid = false;
        }
        return valid;
    }

    public static void main(String[] args) 
    {
    	if (args!=null)
    	{
	    	// parse generalised constructor arguments
	    	int width = Integer.parseInt(args[0]);
	    	int minDepth = Integer.parseInt(args[1]);
	    	int minVerticalStep = Integer.parseInt(args[2]);
	    	int maxVerticalStep = Integer.parseInt(args[3]);
	    	double transitionNoise = Double.parseDouble(args[4]);
	    	double rewardNoise = Double.parseDouble(args[5]);
	    	int frontShape = Integer.parseInt(args[6]);
	    	int seed = Integer.parseInt(args[7]);
	    	// instantiate the environment based on the generalised parameters
	        EnvironmentLoader theLoader = new EnvironmentLoader(new GeneralisedDeepSeaTreasureEnv(width, minDepth, minVerticalStep, 
	        																maxVerticalStep, transitionNoise, rewardNoise, frontShape, seed));
	        theLoader.run();
    	}
    	else
    	{
	    	// instantiate with default settings
	        EnvironmentLoader theLoader = new EnvironmentLoader(new GeneralisedDeepSeaTreasureEnv(3, 3, 1,  3, 0.0, 0.0, GeneralisedDeepSeaTreasureEnv.CONCAVE, 471));
	        theLoader.run();   		
    	}
    }


}

