// Written by Peter Vamplew Sept 2020
// Implements the Space Traders benchmark environment as described in Vamplew et al's paper on
// stochastic MOMDPs and value-based MORL
package env;

import java.util.Random;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;


public class SpaceTraders implements EnvironmentInterface
{  
	private int currentState; 
	private boolean terminal;
	private Reward rewards;
    private Random r = new Random(471);
    // action names
    private final int INDIRECT = 0;
    private final int DIRECT = 1;
    private final int TELEPORT = 2;
    // arrays hold the reward for each action for each state - elements are success/failure rate, time penalty on success, time penalty on failure
    private double[][] STATE_A_DATA = {{1.0, -12, 9999},{0.9, -6, -1},{0.85, 0, 0}};
    private double[][] STATE_B_DATA = {{1.0, -10, 9999},{0.9, -8, -7},{0.85, 0, 0}};
	
    public String env_init() 
    {
        //initialize the starting position, and an object to hold the reward
        currentState = 0;
        rewards = new Reward(0,2,0);
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setContinuing();
        //Specify that there will be this number of observations (ie 3 states - with the 3rd being the terminal state)
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 2));              
        //Specify that there will be three actions
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 2));
        //Specify that there will be this number of objectives
        theTaskSpecObject.setNumOfObjectives(2);
        //Convert specification object to a string
        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);
        return taskSpecString;
    }
    
    // Setup the environment for the start of a new episode
    public Observation env_start() {
        currentState = 0;       
        terminal = false;
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, currentState);
        return theObservation;
    }
    
    // Execute the specified action, update environmental state and return the reward and new observation
    public Reward_observation_terminal env_step(Action action) 
    {
        updatePositionAndReward( action.getInt(0));
        // set up new Observation
        Reward_observation_terminal RewardObs = new Reward_observation_terminal();
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, currentState);
        RewardObs.setObservation(theObservation);
        RewardObs.setTerminal(terminal); // end once we have taken an action in state 1
        // setup new rewards
        RewardObs.setReward(rewards);
        return RewardObs;
    }

    public void env_cleanup() 
    {
    }

    public String env_message(String message) 
    {
        throw new UnsupportedOperationException(message + " is not supported by the LinkedRings environment.");
    }
    
    
    // update the agent's position within the environment based on the specified action, and stores the associated reward values
    public void updatePositionAndReward(int theAction) 
    {
    	if (currentState==0) // Planet A
    	{
    		if (r.nextDouble()<STATE_A_DATA[theAction][0]) // success! we reach planet B
    		{
    			rewards.setDouble(0, 0); // no penalty or reward for mission success until ended
    			rewards.setDouble(1,STATE_A_DATA[theAction][1]); // use the success time penalty
    			currentState = 1;
        	}
    		else // death
    		{
    			rewards.setDouble(0, 0); // no penalty for failure
    			rewards.setDouble(1,STATE_A_DATA[theAction][2]); // use the success time penalty
    			currentState = 2;
    			terminal = true;
        	}
        }
        //  Planet  B
        else if (currentState==1)
        {
    		if (r.nextDouble()<STATE_B_DATA[theAction][0]) // success! we got back to planet A
    		{
    			rewards.setDouble(0, 1); // reward for succesful mission completion
    			rewards.setDouble(1,STATE_B_DATA[theAction][1]); // use the success time penalty
        	}
    		else // death
    		{
    			rewards.setDouble(0, 0); // no penalty for failure
    			rewards.setDouble(1,STATE_B_DATA[theAction][2]); // use the success time penalty
        	}
			currentState = 2;
			terminal = true;
        }     
    }   

    public static void main(String[] args) 
    {
        EnvironmentLoader theLoader = new EnvironmentLoader(new SpaceTraders());
        theLoader.run();
    }


}

