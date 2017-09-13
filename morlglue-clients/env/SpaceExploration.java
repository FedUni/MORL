// Written by Peter Vamplew Dec 2015
// Implements the SpaceExploration environment, as used in our experiments in MORL exploration
// It is a 2D grid-world with 2 objectives.
// The first is a measure of mission success - the agent receives a reward when it reaches a planet,
// with the value corresponding to how earthlike the planet is. It also receives a large negative reward
// if a fatal collision with an asteroid occurs. This objective is non-zero only in terminal states.
// The second objective is a measure of radiation exposure - this has a fixed reward of -1 on all time steps,
// except when the agent encounters regions of high radiation when this reward is -11.

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


public class SpaceExploration implements EnvironmentInterface
{  
	// define the structure of the environment
    private final int NUM_ROWS = 5;
    private final int NUM_COLS = 13;
    
    // specify location of special cells in the environment - row, column
    private final int HOME[] = {2, 5}; // starting state for each episode
    private final int ASTEROID[][] = {{0,5},{1,4},{1,6},{2,3},{2,7},{3,4},{3,6},{4,5}}; // asteroids - fatal collision if agent enters these cells
    private final int PLANET[][][] = { 	{{0,9},{1,9},{2,9}}, // the least hospitable planet
										{{0,0},{1,0},{2,0},{3,0},{4,0}}, // the 2nd best planet
    									{{0,12},{1,12},{2,12},{3,12},{4,12}} // the  best planet									
    								 };
    private final int PLANET_REWARD[] = {10, 20, 30};
    private final int RADIATION[] = {1,10,11}; // higher than normal radiation levels in these columns
   
    
    private boolean terminal;
    
    // define the ordering of the objectives
    private final int NUM_OBJECTIVES = 2;
    private final int SUCCESS_REWARD = 0;
    private final int RADIATION_REWARD = 1;
    // state variables
    private int agentRow;
    private int agentCol;
    private Reward rewards = new Reward(0,NUM_OBJECTIVES,0);
	
    public String env_init() 
    {
        //initialize the problem - starting position is always at the home location
        agentRow = HOME[0];
        agentCol = HOME[1];
        terminal = false;
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setEpisodic();
        //Specify that there will be this number of observations
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, NUM_ROWS*NUM_COLS));    
        //Specify that there will be an integer action [0,7] - this environment supports diagonal actions
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 7));
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
    }

    public String env_message(String message) 
    {
        throw new UnsupportedOperationException(message + " is not supported by SpaceExploration environment.");
    }
    
    // convert the agent's current position into a state index
    public int getState() 
    {
        return agentCol + (NUM_COLS * agentRow);
    }
    
    // set rewards
    private void setRewards(int successScore, int radiationScore)
    {
    	rewards.setDouble(SUCCESS_REWARD, successScore);
    	rewards.setDouble(RADIATION_REWARD, radiationScore);
    }
    
    // update the agent's position within the environment based on the specified action
    public void updatePosition(int theAction) 
    {
        // action directions are as shown below
        // 3 2 1
        // 4   0
        // 5 6 7
        // update column - wrap around if edge of the grid is reached
        if (theAction == 0 || theAction == 1 || theAction == 7) // move right
        {
           agentCol = (agentCol+1) % NUM_COLS;
        }
        else if (theAction == 3 || theAction == 4 || theAction == 5) // move left
        {
            agentCol--;
            if (agentCol<0)
            {
            	agentCol = NUM_COLS-1;
            }
        }
        // update row - wrap around if edge of the grid is reached
        if (theAction == 5 || theAction == 6 || theAction == 7) // move down
        {
           agentRow = (agentRow+1) % NUM_ROWS;
        }
        else if (theAction == 1 || theAction == 2 || theAction == 3) // move up
        {
            agentRow--;
            if (agentRow<0)
            {
            	agentRow = NUM_ROWS-1;
            }
        }

        // end episode with a large negative reward if the agent hits an asteroid
        boolean hitAsteroid = false;
        for (int i=0; i<ASTEROID.length; i++)
        {
        	if (agentRow==ASTEROID[i][0] && agentCol==ASTEROID[i][1])
        	{
        		hitAsteroid = true;
        	}
        }
        if (hitAsteroid)
        {
        	terminal = true;
        	setRewards(-100, -1);
        	return;
        }      
        // check if a planet has been reached
        boolean planetfall = false;
        int planet = 0;
        while (!planetfall && planet<PLANET.length)
        {
            for (int i=0; i<PLANET[planet].length; i++)
            {
            	if (agentRow==PLANET[planet][i][0] && agentCol==PLANET[planet][i][1])
            	{
            		planetfall = true;
            	}
            }      	
        	planet++;
        }
        planet--; // we will have incremented one more time after the planet was reached
        if (planetfall) // reached a planet, so set end episode with an appropriate reward
        {
        	terminal = true;
        	setRewards(PLANET_REWARD[planet],-1);
        	return;	
        }
        // see if agent is in a high radiation zone or not 
        boolean highRadiation = false;
        for (int i=0; i<RADIATION.length; i++)
        {
        	if (agentCol==RADIATION[i])
        	{
        		highRadiation = true;
        	}
        }
        if (highRadiation)
        {
        	setRewards(0, -11);
        }  
        else
        {
        	setRewards(0,-1);
        }
    }
    
    public static void main(String[] args) 
    {
        EnvironmentLoader theLoader = new EnvironmentLoader(new SpaceExploration());
        theLoader.run();
    }


}

