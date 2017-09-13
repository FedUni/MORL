package agents;

import java.util.Random;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

public class SkeletonAgent implements AgentInterface {

    Random randGenerator = new Random();
    Action lastAction;
    Observation lastObservation;

    private int numActions;
    private int numStates;
    private int numOfObjectives;


    public void agent_init(String taskSpecification) {
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax() + 1;
        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numOfObjectives = theTaskSpec.getNumOfObjectives();

		//In more complex agents, you would also check for continuous observations and actions, discount factors, etc.
    }

    public Action agent_start(Observation observation) {
        /**
         * Choose a random action (0 or 1 or 2 or 3)
         */
        int theIntAction = randGenerator.nextInt(4);
        /**
         * Create a structure to hold 1 integer action
         * and set the value
         */
        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = theIntAction;

        lastAction = returnAction.duplicate();
        lastObservation = observation.duplicate();

        return returnAction;
    }

    public Action agent_step(Reward reward, Observation observation) {
        /**
         * Choose a random action (0 or 1 or 2 or 3)
         */
        int theIntAction = randGenerator.nextInt(4);
        /**
         * Create a structure to hold 1 integer action
         * and set the value (alternate method)
         */
        Action returnAction = new Action();
        returnAction.intArray = new int[]{theIntAction};

        lastAction = returnAction.duplicate();
        lastObservation = observation.duplicate();

        return returnAction;
    }

    public void agent_end(Reward reward) {
    }

    public void agent_cleanup() {
        lastAction=null;
        lastObservation=null;
    }

    public String agent_message(String message) {
        if(message.equals("what is your name?"))
            return "my name is skeleton_agent, Java edition!";

	return "I don't know how to respond to your message";
    }
    
    /**
     * This is a trick we can use to make the agent easily loadable.
     * @param args
     */
    
    public static void main(String[] args){
     	AgentLoader theLoader=new AgentLoader(new SkeletonAgent());
        theLoader.run();
	}

}
