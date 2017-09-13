// Written by Peter Vamplew Feb 2015
// Implements the Resource Gathering task from Barrett and Narayanan (2008), structured as an episodic task. 
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


public class ResourceGatheringEpisodic implements EnvironmentInterface
{  
	// define the structure of the environment
    private final int NUM_ROWS = 5;
    private final int NUM_COLS = 5;
    private final int HOME[] = {4,2};
    private final int GOLD_LOCN[] = {0,2};
    private final int GEMS_LOCN[] = {1, 4};
    private final int ENEMY_LOCN1[] = {1,2};
    private final int ENEMY_LOCN2[] = {0,3};
    private final double ENEMY_CHANCE = 0.1;
    private Random r = new Random(58);
    private boolean terminal;
    
    // define the ordering of the objectives
    private final int ENEMY = 0;
    private final int GOLD = 1;
    private final int GEMS = 2;
    private final int TIME = 3;
    private final int NUM_OBJECTIVES = 4;
    // state variables
    private int agentRow;
    private int agentCol;
    private int hasGold;
    private int hasGems;
    private boolean attacked;
    private Reward rewards = new Reward(0,NUM_OBJECTIVES,0);
	
    public String env_init() 
    {
        //initialize the problem - starting position is always at the home location
        this.agentRow = HOME[0];
        this.agentCol = HOME[1];
        hasGold = hasGems = 0;
        attacked = false;
        terminal = false;
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setEpisodic();
        //Specify that there will be this number of observations
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 99));    // 100 states = 25 cells x 4 possible values of resources gathered          
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
        this.agentRow = HOME[0];
        this.agentCol = HOME[1];
        hasGold = hasGems = 0;    
        attacked = false;
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
        this.agentRow = HOME[0];
        this.agentCol = HOME[1];
        hasGold = hasGems = 0; 
    }

    public String env_message(String message) 
    {
        throw new UnsupportedOperationException(message + " is not supported by ResourceGathering environment.");
    }
    
    // convert the agent's current position into a state index
    public int getState() 
    {
        return agentCol * NUM_ROWS + agentRow + 25 * hasGold + 50 * hasGems;
    }
    
    // set rewards - scale up the 0,1 values for enemy,gold,gems to the range 0..10
    private void setRewards(double enemy, double gold, double gems, double time)
    {
    	rewards.setDouble(ENEMY, enemy*10);
    	rewards.setDouble(GOLD, gold*10);
    	rewards.setDouble(GEMS, gems*10);
    	rewards.setDouble(TIME, time);
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
        agentRow = newRow;
        agentCol = newCol; 
        // set default reward - will be over-ridden if any special conditions hold
        setRewards(0,0,0,-1);
        // check if agent has returned home
        if (agentRow==HOME[0] && agentCol==HOME[1])
        {
        	terminal = true;
        	setRewards(0, hasGold, hasGems, -1);
        }
        // check if the agent has picked up gold
        else if (agentRow==GOLD_LOCN[0] && agentCol==GOLD_LOCN[1])
        {
        	hasGold = 1;
        }
        // check if the agent has picked up gems
        else if (agentRow==GEMS_LOCN[0] && agentCol==GEMS_LOCN[1])
        {
        	hasGems = 1;
        }       
        // check for enemy attack
        else if ((agentRow==ENEMY_LOCN1[0] && agentCol==ENEMY_LOCN1[1]) || (agentRow==ENEMY_LOCN2[0] && agentCol==ENEMY_LOCN2[1]))
        {
        	if (r.nextDouble()<=ENEMY_CHANCE) 
        	{
        		// the agent was attacked, so move to the home position and lose all resources
        		attacked = true;
                this.agentRow = HOME[0];
                this.agentCol = HOME[1];
                hasGold = hasGems = 0;
                terminal = true;
                setRewards(-1,0,0,-1);
        	}
        }

    }
    
    public static void main(String[] args) 
    {
        EnvironmentLoader theLoader = new EnvironmentLoader(new ResourceGatheringEpisodic());
        theLoader.run();
    }


}

