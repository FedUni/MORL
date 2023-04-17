package agents;

import java.util.Stack;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import tools.staterep.DummyStateConverter;
import tools.staterep.interfaces.StateConverter;
import tools.traces.StateActionIndexPair;
import tools.valuefunction.TLO_Option_LookupTable;
import tools.valuefunction.TLO_LookupTable;
import tools.valuefunction.interfaces.ActionSelector;

public class TLO_Agent_Option implements AgentInterface 
{

    double thresholds[];
    int numThresholds;
	int currentOption; // the current option been followed

	TLO_Option_LookupTable vf = null;
	Stack<StateActionIndexPair> tracingStack = null;

	private boolean policyFrozen = false;

    private int numActions = 0;
    private int numStates = 0; 
    private int numOptions = 0;
    private int numOfObjectives;
    private int [][] getAction;

    private final double initQValues[]={0,0,0};
    int explorationStrategy; // flag used to indicate which type of exploration strategy is being used
    //if using eGreedy exploration
    double startingEpsilon;
    double epsilonLinearDecay;
    double epsilon;
    // if using softmax selection
    double startingTemperature;
    double endingTemperature;
    double temperatureDecayRatio;
    double temperature;
    
    double startingAlpha, alpha, alphaDecay;
    double gamma;
    double lambda;
    final int MAX_STACK_SIZE = 20;

    int numOfSteps;
    int numEpisodes; 

    boolean greedyFlag;

    StateConverter stateConverter = null;
    

    @Override
    public void agent_init(String taskSpecification) {
    	System.out.println("TLO_Agent_Option launched");
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numStates = theTaskSpec.getDiscreteObservationRange(0).getMax()+1;
        numOfObjectives = theTaskSpec.getNumOfObjectives();
        numThresholds = numOfObjectives-1;
        thresholds = new double[numThresholds]; 
       
        getAction = createOptionAction(numStates, numActions);
        // we aren't creating the lookup table at this stage - need to defer that until we get the message telling us 
        // what the thresholds are - that needs to happen before the first trial is started

        tracingStack = new Stack<>();
        //set the model of converting MDP observation to an int state representation
        stateConverter = new DummyStateConverter();
    }
    
    private void resetForNewTrial()
    {
    	policyFrozen = false;
        numOfSteps = 0;
        numEpisodes = 0;  
        epsilon = startingEpsilon;
        temperature = startingTemperature;
        alpha = startingAlpha;

        // reset Q-values 
        vf.resetQValues(initQValues);
    }
    
    private void resetForNewEpisode()
    {
        if (!policyFrozen){
            numEpisodes++;
        }
        numOfSteps = 0; 
        // reset the tracingStack
        tracingStack.clear();
    }
    
    @Override
    public Action agent_start(Observation observation) {

    	resetForNewEpisode();
        int state = stateConverter.getStateNumber( observation );
        currentOption = getOption(state);
        greedyFlag = isGreedy(state, currentOption);
        int action = getAction[state][currentOption];

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;

        Action returnOption = new Action(1, 0, 0);
        returnOption.intArray[0] = currentOption;
        tracingStack.add(new StateActionIndexPair(state, returnOption)); // put executed option on the stack

        return returnAction; 
    }

    @Override
    public Action agent_step(Reward reward, Observation observation) 
    {
        numOfSteps++;

        int state = stateConverter.getStateNumber( observation );
        int action = getAction[state][currentOption];

        if (!policyFrozen) {
            double currentLambda = lambda;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionIndexPair pair = tracingStack.get(i);

                int prevOption = pair.getAction().getInt(0);
                int prevState = pair.getState();

                if (i + 1 == tracingStack.size()) // this is the most recent action
                {
                    vf.calculateErrors(prevOption, prevState, currentOption, state, gamma, reward);
                    vf.update(prevOption, prevState, 1.0, alpha);
                } 
                else {
                    // if there is no more recent entry for this state-action pair then update it
                    // this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                        vf.update(prevOption, prevState, currentLambda, alpha);
                    }
                    currentLambda *= lambda;
                }

            }
        }

        Action returnOption = new Action(1, 0, 0);
        returnOption.intArray[0] = currentOption;
        
        // clear trace if this Option is not greedy, otherwise trim stack if neccesary
        // if (greedyFlag)
        // {
	        if( tracingStack.size() == MAX_STACK_SIZE ) 
	        {
	            tracingStack.remove(0);
	        }
        // }
        // else
        // {
        // 	tracingStack.clear();
        // }
        // in either case, can now add this state-action to the trace stack
        tracingStack.add( new StateActionIndexPair(state, returnOption) );

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;

        return returnAction;
    }

    @Override
    public void agent_end(Reward reward) 
    {
  	  	numOfSteps++;

  	  	epsilon -= epsilonLinearDecay;
  	  	temperature *= temperatureDecayRatio;
  	  	alpha -= alphaDecay;
        if (!policyFrozen) {
            double currentLambda = lambda;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionIndexPair pair = tracingStack.get(i);

                int prevOption = pair.getAction().getInt(0);
                int prevState = pair.getState();

                if (i + 1 == tracingStack.size()) 
                {
                    vf.calculateTerminalErrors(prevOption, prevState, gamma, reward);
                    vf.update(prevOption, prevState, 1.0, alpha);
                } 
                else 
                {
                	// if there is no more recent entry for this state-action pair then update it
                	// this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                        vf.update(prevOption, prevState, currentLambda, alpha);
                    }
                    currentLambda *= lambda;
                }
            }
        }
        
    }

    @Override
    public void agent_cleanup() {
        vf = null;
        policyFrozen = false;
    }

    private int [] [] createOptionAction(int numStates, int numActions) {

        // base, exponent
        numOptions = (int) Math.pow(numActions, (numStates-1)); // don't count the terminal state
        int[][] getAction = new int [numStates][numOptions];

        for (int s=0; s<numStates-1; s++){
            for (int o=0; o<numOptions; o++){
                if(s==0){
                    getAction[s][o] = o/numActions;
                }
                if(s==1){
                    getAction[s][o] = o%numActions;
                }
            }
        }
        return getAction;
    }
    
    private int getOption(int state) {

        int option;
        if (!policyFrozen)
        {
        	switch (explorationStrategy)
        	{
	        	case TLO_LookupTable.EGREEDY: 
                    option = vf.choosePossiblyExploratoryAction(epsilon, state); 
	        		break;
	        	case TLO_LookupTable.SOFTMAX_TOURNAMENT: 
                    //action = valueFunction.choosePossiblyExploratoryAction(temperature, state);
                    //break;
	        	case TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON : 
                    option = vf.choosePossiblyExploratoryAction(temperature, state);
	        		break;
	        	default:
                    option = -1; // this should never happen - if it does we'll return an invalid value to force the program to halt
        	}
        } 
        else 
        {
        	option = vf.chooseGreedyAction(state);
        }
        
        return option;
    }
    
    // returns true if the specified option is amongst the greedy options for the 
    // specified state, false otherwise
    private boolean isGreedy(int state, int option){
        ActionSelector valueFunction = (ActionSelector) vf;
        return valueFunction.isGreedy(state, option);  	
    }

    @Override
    public String agent_message(String message) {
    	if (message.equals("get_agent_name")){
    		return "TLO_Agent_Option";
    	}
        if (message.equals("freeze_learning")) {
            policyFrozen = true;
            System.out.println("Learning has been freezed");
            return "message understood, policy frozen";
        }
        if (message.startsWith("change_weights")){
            System.out.print("TLO_Agent_Option: Weights can not be changed");
            return "TLO_Agent_Option: Weights can not be changed";
        }
        // This is a really important message as its sets the TLO parameters and allows for the creation of the LookUp Table at the
        // right size for the augmented state - this must happen before a trial can be run
        if (message.startsWith("set_TLO_parameters")){
        	System.out.println(message);
        	String[] parts = message.split(" ");
        	// iterate through the thresholded objectives, loading the discretisation and thresholding settings into the arrays
        	for (int i=0; i<numThresholds; i++)
        	{
	        	thresholds[i] = Double.valueOf(parts[i+1]).doubleValue();
	        	System.out.println("Objective " + i + " / " + thresholds[i]);
        	}


            vf = new TLO_Option_LookupTable(numOfObjectives, numOptions, numStates, 0, thresholds);
        	System.out.println();
            return "TLO parameters set";
        }
        if (message.startsWith("set_learning_parameters")){
        	System.out.println(message);
        	String[] parts = message.split(" ");
        	startingAlpha = Double.valueOf(parts[1]).doubleValue();
        	lambda = Double.valueOf(parts[2]).doubleValue(); 
        	gamma = Double.valueOf(parts[3]).doubleValue();
        	explorationStrategy = Integer.valueOf(parts[4]).intValue();
        	alphaDecay = Double.valueOf(parts[5]).doubleValue();
        	vf.setExplorationStrategy(explorationStrategy);
        	System.out.print("Alpha = " + startingAlpha + " Lambda = " + lambda + " Gamma = " + gamma + " exploration = " + TLO_LookupTable.explorationStrategyToString(explorationStrategy) + " alpha decay = " +alphaDecay);
            System.out.println();
            return "Learning parameters set";
        }
        //////
        if (message.startsWith("print_lookup_table")){
            for(int i=0;i<numStates;i++){
                vf.printQValues(i);
            }
            return "Print the lookup table";
        }
        if(message.equals("print_policy")){
            return "&"+ vf.chooseGreedyAction(0);
        }
        if(message.equals("get_q_value")){
            String q_value_string = "";
            for(int i=0; i< numOptions; i++){
                q_value_string += "&" + vf.getQValues(i, 0)[0] + "&" + vf.getQValues(i, 0)[1];
            }
            return q_value_string;
        }
        if (message.startsWith("set_egreedy_parameters")){
        	String[] parts = message.split(" ");
        	startingEpsilon = Double.valueOf(parts[1]).doubleValue();
        	epsilonLinearDecay = startingEpsilon / Double.valueOf(parts[2]).doubleValue(); // 2nd param is number of online episodes over which e should decay to 0
            System.out.println("Starting epsilon changed to " + startingEpsilon);
            return "egreedy parameters changed";
        }
        if (message.startsWith("set_softmax_parameters")){
        	String[] parts = message.split(" ");
        	startingTemperature = Double.valueOf(parts[1]).doubleValue();
        	int numEpisodes =  Integer.valueOf(parts[2]).intValue(); // 2nd param is number of online episodes over which temperature should decay to 0.01
            endingTemperature = Double.valueOf(parts[3]).doubleValue();
        	temperatureDecayRatio = Math.pow(endingTemperature/startingTemperature,1.0/numEpisodes);
            System.out.println("Starting temperature changed to " + startingTemperature + " Decay ratio = " + temperatureDecayRatio);
            return "softmax parameters changed";
        } 
        else if (message.equals("start_new_trial")){
        	resetForNewTrial();
            System.out.println("New trial started: Q-values and other variables reset");
            return "New trial started: Q-values and other variables reset";
        }
        else if (message.equals("start-debugging")){
    		return "Debugging enabled in agent";
    	}
        else if (message.equals("stop-debugging")){
    		return "Debugging disabled in agent";
    	}
        System.out.println("TLO_Agent_Option - unknown message: " + message);
        return "TLO_Agent_Option does not understand your message.";
    }
    
    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new TLO_Agent_Option() );
        theLoader.run();
    }
}
