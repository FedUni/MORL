// A simple agent where all decisions are made by a human user via the keyboard. Designed primarily for testing and debugging of new Environments.
// Written by Peter Vamplew, Nov 2015, based on the SkeletonAgent code.
package agents;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import java.util.Scanner;

public class UserControlledAgent implements AgentInterface 
{
    private int numActions;
    private int numStates;
    private int numOfObjectives;
    private Scanner scanner;
    private double rewardSum[];


    public void agent_init(String taskSpecification) {
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax() + 1;
        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numOfObjectives = theTaskSpec.getNumOfObjectives();
        scanner = new Scanner(System.in);
        rewardSum = new double[numOfObjectives];
    }

    public Action agent_start(Observation observation) 
    {
    	System.out.println("*** Starting new episode");
        displayObservation(observation);
        int theIntAction =  getNextAction(numActions);
        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = theIntAction;
        for (int i=0; i<numOfObjectives; i++)
        {
        	rewardSum[i] = 0.0;
        }
        return returnAction;
    }

    public Action agent_step(Reward reward, Observation observation) 
    {
        displayObservation(observation);
        displayAndAccumulateReward(reward);
        int theIntAction =  getNextAction(numActions);
        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = theIntAction;
        return returnAction;
    }

    public void agent_end(Reward reward) 
    {
        displayAndAccumulateReward(reward);
        displayFinalReward();
        System.out.println("*** This is a terminal state\n\n");
    }

    public void agent_cleanup() 
    {
    }
    
    // Display the current observation to the user
    private void displayObservation(Observation observation)
    {
    	System.out.println("The current state is " + observation.getInt(0));
    }
    
    // Show the current reward vector to the user, and add it on to the accumulated reward
    private void displayAndAccumulateReward(Reward reward)
    {
    	System.out.print("The reward is ");
    	for (int i=0; i<numOfObjectives; i++)
    	{
    		System.out.print(reward.getDouble(i) + "\t");
        	rewardSum[i] += reward.getDouble(i);
    	}
    	System.out.println();
    }
    
    // Show the accumulated reward vector to the user
    private void displayFinalReward()
    {
    	System.out.print("*** The accumulated reward is ");
    	for (int i=0; i<numOfObjectives; i++)
    	{
    		System.out.print(rewardSum[i] + "\t");
    	}
    	System.out.println();    	
    }
    
    // Get a choice of action from the user
    private int getNextAction(int numActions)
    {
    	int choice;
    	do
    	{
    		System.out.print("Select an action in the range 0 .. " + (numActions-1) + ": ");
    		choice = Integer.parseInt(scanner.next());   		
    		
    	} while (choice<0 || choice>numActions-1);
    	System.out.println();
    	return choice;
    }

    public String agent_message(String message) {
        if(message.equals("what is your name?"))
            return "my name is user_controlled_agent, Java edition!";

	return "I don't know how to respond to your message";
    }
    
    /**
     * This is a trick we can use to make the agent easily loadable.
     * @param args
     */
    
    public static void main(String[] args){
     	AgentLoader theLoader=new AgentLoader(new UserControlledAgent());
        theLoader.run();
	}

}
