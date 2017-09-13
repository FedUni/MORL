// Written by Peter Vamplew Dec 2015
// Implements the BonusWorld environment, as used in our experiments in MORL exploration
// It is a 2D grid-world with 3 objectives - 2 are terminal-only, and the other is the time objective.

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


public class BonusWorld implements EnvironmentInterface
{  
	// define the structure of the environment
    private final int NUM_ROWS = 9;
    private final int NUM_COLS = 9;
    private final int HOME[] = {0,0};
    private final int WALL[][] = {{2,2},{2,3},{3,2}}; // walls block movement
    private final int PIT[][] = {{7,1},{7,3},{7,5},{1,7},{3,7},{5,7}}; // falling in a pit returns to the starting state, but doesn't end the episode
    private final int BONUS_LOCN[] = {3,3};
    private boolean terminal;
    
    // define the ordering of the objectives
    private final int ROW_REWARD = 0;
    private final int COL_REWARD = 1;
    private final int TIME_REWARD = 2;
    private final int NUM_OBJECTIVES = 3;
    // state variables
    private int agentRow;
    private int agentCol;
    private int hasBonus;
    private Reward rewards = new Reward(0,NUM_OBJECTIVES,0);
	
    public String env_init() 
    {
        //initialize the problem - starting position is always at the home location
        agentRow = HOME[0];
        agentCol = HOME[1];
        hasBonus = 0;
        terminal = false;
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setEpisodic();
        //Specify that there will be this number of observations
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 161));    // 162 states = 81 cells x 2 possible values of having the bonus (true/false)         
        //Specify that there will be an integer action [0,3]
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 3));
        //Specify that there will this number of objectives
        theTaskSpecObject.setNumOfObjectives(NUM_OBJECTIVES);
        //Convert specification object to a string
        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);
        return taskSpecString;
    }
    
    // Setup the environment for the start of a new episode
    public Observation env_start() {
        agentRow = HOME[0];
        agentCol = HOME[1];
        hasBonus = 0;    
        terminal = false;
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
        RewardObs.setTerminal(terminal);
        // setup new rewards
        RewardObs.setReward(rewards);
        return RewardObs;
    }

    public void env_cleanup() 
    {
    	//starting position is always the home location
        agentRow = HOME[0];
        agentCol = HOME[1];
        hasBonus = 0; 
    }

    public String env_message(String message) 
    {
        throw new UnsupportedOperationException(message + " is not supported by BonusWorld environment.");
    }
    
    // convert the agent's current position into a state index
    public int getState() 
    {
        return agentCol + (NUM_COLS * agentRow) + (NUM_COLS * NUM_ROWS) * hasBonus;
    }
    
    // set rewards - scale up the 0,1 values for enemy,gold,gems to the range 0..10
    private void setRewards(int rowScore, int colScore, int timeScore)
    {
    	rewards.setDouble(ROW_REWARD, rowScore);
    	rewards.setDouble(COL_REWARD, colScore);
    	rewards.setDouble(TIME_REWARD, timeScore);
    }
    
    // update the agent's position within the environment based on the specified action
    public void updatePosition(int theAction) 
    {
        int newRow = agentRow;
        int newCol = agentCol;

        // update position - staying in the bounds of the grid
        //  0 = go right
        if (theAction == 0) 
        {
            newCol++;
            if (newCol > NUM_COLS-1) {
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
            }
        }
        //  2 = go down
        if (theAction == 2) 
        {
            newRow++;
            if (newRow > NUM_ROWS-1) 
            {
                newRow--;
            }          
        }
        //  3 = go up
        if (theAction == 3) {
            newRow--;
            if (newRow < 0) {
                newRow = 0;
            }
        }
        // don't move if we would hit the wall cells; also check if the bonus has been obtained
        boolean hitWall = false;
        for (int i=0; i<WALL.length; i++)
        {
        	if (newRow==WALL[i][0] && newCol==WALL[i][1])
        	{
        		hitWall = true;
        	}
        }
        if (!hitWall)
        {
        	agentRow = newRow;
        	agentCol = newCol; 
        	if (agentRow==BONUS_LOCN[0] && agentCol==BONUS_LOCN[1])
        	{
        		hasBonus = 1;
        	}
        }
        // check if we fell in a pit - if so, lose the bonus and return to the start state
        boolean hitPit = false;
        for (int i=0; i<PIT.length; i++)
        {
        	if (newRow==PIT[i][0] && newCol==PIT[i][1])
        	{
        		hitPit = true;
        	}
        }
        if (hitPit)
        {
        	agentRow = HOME[0];
        	agentCol = HOME[1]; 
        	hasBonus = 0;
        }      
        // check if terminal location has been reached and set reward accordingly
        if ((agentRow==NUM_ROWS-1 && agentCol%2==0) || (agentCol==NUM_COLS-1 && agentRow%2==0))
        {
        	terminal = true;
        	setRewards((agentRow+1)*(hasBonus+1), (agentCol+1)*(hasBonus+1), -1);
        } 
        else
        {
            setRewards(0,0,-1);
        }
    }
    
    public static void main(String[] args) 
    {
        EnvironmentLoader theLoader = new EnvironmentLoader(new BonusWorld());
        theLoader.run();
    }


}

