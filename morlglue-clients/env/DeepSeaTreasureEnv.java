// Written by Rustam Issabekov 2014
// Refactored by Peter Vamplew Jan 2015
// Implements the Deep Sea Treasure MORL benchmark problem as described in Vamplew et al (2011)
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


public class DeepSeaTreasureEnv implements EnvironmentInterface
{  
	// define the structure of the environment, and the treasure rewards
    private int depths[] = {1, 2, 3, 4, 4, 4, 7, 7, 9, 10};
    private float treasure[] = {1, 2, 3, 5, 8, 16, 24, 50, 74, 124};
    private int numRows = 11;
    private int numCols = 10;
    // define the ordering of the objectives
    private static int TREASURE = 0;
    private static int TIME = 1;
    // location of the submarine within the environment
    private int agentRow;
    private int agentCol;
	
    public String env_init() 
    {
        //initialize the problem - starting position is always fixed, top-left corner of the world
        this.agentRow = 0;
        this.agentCol = 0;
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setEpisodic();
        //Specify that there will be this number of observations
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 11*10 - 1));              
        //Specify that there will be an integer action [0,3]
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 3));
        //Specify that there will this number of objectives
        theTaskSpecObject.setNumOfObjectives(2);
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
        theObservation.setInt(0, getState());
        return theObservation;
    }
    
    // Execute the specified action, update environmental state and return the reward and new observation
    public Reward_observation_terminal env_step(Action action) 
    {
        updatePosition( action.getInt(0) );
        // set up new Observation
        Reward_observation_terminal RewardObs = new Reward_observation_terminal();
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, getState());
        RewardObs.setObservation(theObservation);
        RewardObs.setTerminal(isTerminal());
        // setup new rewards
        Reward rewards = new Reward(0,2,0);
        double[] rewardsArray = getRewards();
        rewards.setDouble(0, rewardsArray[0]);
        rewards.setDouble(1, rewardsArray[1]);
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
        throw new UnsupportedOperationException(message + " is not supported by DST environment.");
    }
    
    // convert the agent's current position into a state index
    public int getState() 
    {
        return agentCol * numRows + agentRow;
    }
    
    // update the agent's position within the environment based on the specified action
    public void updatePosition(int theAction) 
    {
        int newRow = agentRow;
        int newCol = agentCol;

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
        agentRow = newRow;
        agentCol = newCol;      
    }
    
    public double[] getRewards() 
    {
        double objs[] = new double[2];
        objs[TIME] = -1; // time reward is -1 on all time-steps
        objs[TREASURE] = 0;
        //if found treasure
        if (agentRow == depths[agentCol]) {
            objs[TREASURE] = treasure[agentCol];
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
        EnvironmentLoader theLoader = new EnvironmentLoader(new DeepSeaTreasureEnv());
        theLoader.run();
    }


}

