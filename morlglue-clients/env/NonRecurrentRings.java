// Written by Peter Vamplew 2015
// Implements the Linked Rings MORL benchmark environment as described in Vamplew et al's paper on
// non-stationary steering
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


public class NonRecurrentRings implements EnvironmentInterface
{  
	private int currentState; 
	private Reward rewards;
	
    public String env_init() 
    {
        //initialize the starting position, and an object to hold the reward
        currentState = 1;
        rewards = new Reward(0,2,0);
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setContinuing();
        //Specify that there will be this number of observations
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, 7));              
        //Specify that there will be two actions 0 and 1
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 1));
        //Specify that there will be this number of objectives
        theTaskSpecObject.setNumOfObjectives(2);
        //Convert specification object to a string
        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);
        return taskSpecString;
    }
    
    // Setup the environment for the start of a new episode
    public Observation env_start() {
        currentState = 1;       
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, currentState-1); // maps states 1-8 to values 0 to 7 for the agent
        return theObservation;
    }
    
    // Execute the specified action, update environmental state and return the reward and new observation
    public Reward_observation_terminal env_step(Action action) 
    {
        updatePositionAndReward( action.getInt(0));
        // set up new Observation
        Reward_observation_terminal RewardObs = new Reward_observation_terminal();
        Observation theObservation = new Observation(1, 0, 0);
        theObservation.setInt(0, currentState-1); // maps states 1-8 to values 0 to 7 for the agent
        RewardObs.setObservation(theObservation);
        RewardObs.setTerminal(false); // this is a continuing task, so it never terminates
        // setup new rewards
        RewardObs.setReward(rewards);
        return RewardObs;
    }

    public void env_cleanup() 
    {
    }

    public String env_message(String message) 
    {
        throw new UnsupportedOperationException(message + " is not supported by the NonRecurrentRings environment.");
    }
    
    
    // update the agent's position within the environment based on the specified action, and stores the associated reward values
    public void updatePositionAndReward(int theAction) 
    {
        //  0 = counter-clockwise
        if (theAction == 0) 
        {
        	if (currentState<=4)
        	{
        		currentState++;
        		if (currentState>4)
        		{
        			currentState = 1;
        		}
        		rewards.setDouble(0, 2);
        		rewards.setDouble(1,-1);
        	}
        	else
        	{
				currentState--;
				if (currentState<5)
				{
					currentState = 1;
				}
				rewards.setDouble(0, 0);
				rewards.setDouble(1,-1);
        	}
        }
        //  1 = clockwise
        else if (theAction == 1) 
        {
        	if (currentState>=5)
        	{
				currentState++;
				if (currentState>8)
				{
					currentState = 5;
				}
				rewards.setDouble(0, -1);
				rewards.setDouble(1,2);
        	}
        	else
        	{
				currentState--;
				if (currentState<=0)
				{
					currentState=5;
				}
				rewards.setDouble(0, -1);
				rewards.setDouble(1,0);       		
        	}
        }     
    }   

    public static void main(String[] args) 
    {
    	//System.out.println("Launching NonRecurrent Rings Environment");
        EnvironmentLoader theLoader = new EnvironmentLoader(new NonRecurrentRings());
        theLoader.run();
    }


}

