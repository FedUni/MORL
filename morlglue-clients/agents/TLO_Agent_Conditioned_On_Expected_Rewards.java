// Implementation of a TLO_Agent which uses an augmented state based on accumulated expected rewards to get around issues caused by
// non-additivity of rewards in the Bellman equation - this version is specifically designed to find the SER-optimal deterministic policy. 
// VERY IMPORTANT: The set_TLO_Parameters message must be called to supply these parameters before a trial can be run as the sizing of the
// lookup table depends on those values. It also needs to be called before setting the learning parameters.

package agents;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import tools.hypervolume.Point;
import tools.staterep.DummyStateConverter;
import tools.staterep.interfaces.StateConverter;
import tools.traces.StateActionIndexPair;
import tools.valuefunction.TLOConditionedLookupTable;
import tools.valuefunction.TLO_LookupTable;
import tools.valuefunction.interfaces.ActionSelector;
import tools.valuefunction.interfaces.ValueFunction;


public class TLO_Agent_Conditioned_On_Expected_Rewards implements AgentInterface 
{

	// Parameters for the discretisation of the accumulated rewards - all arrays so they can be set independently for each objective that
	// is being thresholded
	double discretisationMin[], discretisationMax[], discretisationGranularity[];
    int numDiscretisations[];
    double thresholds[];
    int numThresholds;
    double accumulatedRewards[];

    // as well as the standard Q-table we also need to learn the expected immediate reward for each state-action-objective - we will
    // calculate this from the sum received over all experineces divided by the number of experiences
    double immediateRewardSum[][][];
    double stateActionVisits[][];
    int lastState, lastAction; // the most recent environmental state and last action - needed for updating the immediate reward estimates
    
	TLOConditionedLookupTable vf = null;
    Stack<StateActionIndexPair> tracingStack = null;

    private boolean policyFrozen = false;
    private boolean debugging = false;
    private Random random;

    private int numActions = 0;
    private int numEnvtStates = 0; // number of states in the environment
    private int numStates; // number of augmented states (agent state = environmental-state U accumulated-primary-reward)
    int numOfObjectives;

    private final double initQValues[]={0,0,0};
    int explorationStrategy; // flag used to indicate which type of exploration strategy is being used
    //if using eGreedy exploration
    double startingEpsilon;
    double epsilonLinearDecay;
    double epsilon;
    // if using softmax selection
    double startingTemperature;
    double temperatureDecayRatio;
    double temperature;
    
    double startingAlpha, alpha, alphaDecay;
    double gamma;
    double lambda;
    final int MAX_STACK_SIZE = 20;

    int numOfSteps;
    int numEpisodes;

    
    //DEBUGGING STUFF
    int saVisits[][];

    StateConverter stateConverter = null;
    
    // Size all of the arrays associated with thresholding etc now that the number of objectives is known
    private void createArrays()
    {
    	discretisationMin = new double[numThresholds];
    	discretisationMax = new double[numThresholds];
        numDiscretisations = new int[numThresholds];
    	discretisationGranularity = new double[numThresholds];
    	thresholds = new double[numThresholds];   
    	accumulatedRewards = new double[numThresholds];
    	immediateRewardSum = new double [numEnvtStates][numActions][numOfObjectives];
    	stateActionVisits = new double[numEnvtStates][numActions];
    }

    @Override
    public void agent_init(String taskSpecification) {
    	System.out.println("SatisficingMOAgent launched");
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numEnvtStates = (theTaskSpec.getDiscreteObservationRange(0).getMax()+1);
        numOfObjectives = theTaskSpec.getNumOfObjectives();
        numThresholds = numOfObjectives-1;
        createArrays();
        // we aren't creating the lookup table at this stage - need to defer that until we get the message telling us how many
        // discretisations levels are being used for each objective, and what the thresholds are - that needs to happen before the 
        // first trial is started

        random = new Random(471);
        tracingStack = new Stack<>();

        //set the model of converting MDP observation to an int state representation
        stateConverter = new DummyStateConverter();
        
        //DEBUGGING STUFF
        saVisits= new int[numStates][numActions];

    }
    
    private void resetForNewTrial()
    {
    	policyFrozen = false;
        numOfSteps = 0;
        numEpisodes = 0;  
        epsilon = startingEpsilon;
        temperature = startingTemperature;
        alpha = startingAlpha;
        // reset Q-values and the accumulated rewards
        vf.resetQValues(initQValues);
        for (int i=0; i<numThresholds; i++)
        {
        	accumulatedRewards[i]=0;
        }
        vf.setConditioningValues(accumulatedRewards);
    	for (int i=0; i<numEnvtStates; i++)
    		for (int j=0; j<numActions; j++)
    		{
    			stateActionVisits[i][j]=0;
    			for (int k=0; k<numOfObjectives; k++)
    				immediateRewardSum[i][j][k] = 0;
    		}
    }
    
    private void resetForNewEpisode()
    {
  	  	numEpisodes++;
        numOfSteps = 0; 
        for (int i=0; i<numThresholds; i++)
        {
        	accumulatedRewards[i]=0;
        }
        vf.setConditioningValues(accumulatedRewards);
        tracingStack.clear();
        
        //DEBUGGING STUFF
        for (int s=0; s<numStates; s++)
        	for (int a=0; a<numActions; a++)
        		saVisits= new int[numStates][numActions];
    }
    
    // Discretises a double value to an index given min and max range values, a granularity, and specified number of discretisations within 
    // the range. Returns 0 if value < min, numDiscretisations + 1 if value > max, 1..numDiscretisations otherwise
    private int discretise(double value, double min, double max, double granularity, int numDiscretisations)
    {
    	if (value<min)
    		return 0;
    	else if (value>max)
    		return numDiscretisations+1;
    	else
    		return (int)Math.floor((value-min)/granularity)+1;
    }
    
    // combines the observed state info with the discretised primary reward to get the augmented state index
    private int getAugmentedStateIndex(Observation observation)
    {
    	int rewardState;
    	int augmentedState = stateConverter.getStateNumber( observation ); // get the envt state index
    	int totalStatesSoFar = numEnvtStates;
    	// repeat for each thresholded objective - discretise its current value, and apply this
    	for (int i=0; i<numThresholds; i++)
    	{
    		rewardState = discretise(accumulatedRewards[i],discretisationMin[i],discretisationMax[i],discretisationGranularity[i],numDiscretisations[i]);
    		augmentedState += rewardState * totalStatesSoFar;
    		totalStatesSoFar = totalStatesSoFar * (numDiscretisations[i]+2);
    	}
    	return augmentedState;
    }
    

    @Override
    public Action agent_start(Observation observation) {
    	//if (debugging) debugHelper();
    	resetForNewEpisode();
        int state = getAugmentedStateIndex(observation);
        int action = getAction(state);

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        tracingStack.add(new StateActionIndexPair(state, returnAction)); // put executed action on the stack
    	if (debugging)
    	{
        	for (int i=0; i<numActions; i++)
        	{
        		System.out.print("(");
        		double[] q = vf.getQValues(i, state);
        		for (int j=0; j<numOfObjectives; j++)
        		{
        			System.out.print(q[j]+" ");
        		}
        		System.out.print(") ");
        	}
        	System.out.println();
    		int greedyAction = ((ActionSelector)vf).chooseGreedyAction(state);
    		System.out.println("Starting episode " + numEpisodes + " Epsilon = " + epsilon + " Alpha = " + alpha);
    		System.out.println("Step: " + numOfSteps +"\tState: " + state + "\tGreedy action: " + greedyAction + "\tAction: " + action);
    	}
    	// store info needed for updating immediate-rewards on the next time-step
        lastState = stateConverter.getStateNumber( observation ); // get the envt state index
        lastAction = action;
   		//System.out.println("Starting episode " + numEpisodes + " Epsilon = " + epsilon + " Alpha = " + alpha);
        return returnAction;
    }

    @Override
    public Action agent_step(Reward reward, Observation observation) 
    {
        numOfSteps++;
        // update estimate of immediate reward for the previous state-action 
        if (!policyFrozen)
        {
        	for (int i=0; i<numOfObjectives; i++)
	        {
	        	immediateRewardSum[lastState][lastAction][i] += reward.getDouble(i);
	        }
        	stateActionVisits[lastState][lastAction]++;
        }
        // update the accumulated expected rewards
        for (int i=0; i<numThresholds; i++)
        {
        	accumulatedRewards[i] += immediateRewardSum[lastState][lastAction][i] / stateActionVisits[lastState][lastAction];
        }
        System.out.print("Reward\t" + reward.getDouble(0) + "\tLast state\t" + lastState + "\tLast action\t" + lastAction + "\tI(ls,la)\t" + immediateRewardSum[lastState][lastAction][0] + "\tAcc\t" + accumulatedRewards[0]);
        vf.setConditioningValues(accumulatedRewards);

        int state = getAugmentedStateIndex(observation);
        int action;
        int greedyAction = ((ActionSelector)vf).chooseGreedyAction(state);

        if (!policyFrozen) {
            double currentLambda = lambda;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionIndexPair pair = tracingStack.get(i);

                int prevAction = pair.getAction().getInt(0);
                int prevState = pair.getState();

                if (i + 1 == tracingStack.size()) // this is the most recent action
                {
                    vf.calculateErrors(prevAction, prevState, greedyAction, state, gamma, reward);
                    vf.update(prevAction, prevState, 1.0, alpha);
                } 
                else {
                	// if there is no more recent entry for this state-action pair then update it
                	// this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                        vf.update(prevAction, prevState, currentLambda, alpha);
                    }
                    currentLambda *= lambda;
                }
            }
            action = getAction(state);
        } else {// if frozen, don't learn and follow greedy policy
            action = greedyAction;
        }

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        // clear trace if this action is not greedy, otherwise trim stack if neccesary
        if (isGreedy(state,action))
        {
	        if( tracingStack.size() == MAX_STACK_SIZE ) 
	        {
	            tracingStack.remove(0);
	        }
        }
        else
        {
        	tracingStack.clear();
        }
        // in either case, can now add this state-action to the trace stack
        tracingStack.add( new StateActionIndexPair(state, returnAction ) );
        if (debugging)
        {
        	for (int i=0; i<numActions; i++)
        	{
        		System.out.print("(");
        		double[] q = vf.getQValues(i, state);
        		for (int j=0; j<numOfObjectives; j++)
        		{
        			System.out.print(q[j]+" ");
        		}
        		System.out.print(") ");
        	}
        	System.out.println();
        	greedyAction = ((ActionSelector)vf).chooseGreedyAction(state);
        	System.out.println("Step: " + numOfSteps +"\tState: " + state + "\tGreedy action: " + greedyAction + "\tAction: " + action + "\tImpact: " + reward.getDouble(1) + "\tReward: " + reward.getDouble(0));
        	System.out.println();
        }
    	// store info needed for updating immediate-rewards on the next time-step
        lastState = stateConverter.getStateNumber( observation ); // get the envt state index
        lastAction = action;
        System.out.println("\tAction\t" + action);
        return returnAction;
    }

    @Override
    public void agent_end(Reward reward) 
    {
  	  	numOfSteps++;
        // update estimate of immediate reward for the previous state-action 
        if (!policyFrozen)
        {
        	for (int i=0; i<numOfObjectives; i++)
	        {
	        	immediateRewardSum[lastState][lastAction][i] += reward.getDouble(i);
	        }
        	stateActionVisits[lastState][lastAction]++;
        }
  	  	epsilon -= epsilonLinearDecay;
  	  	temperature *= temperatureDecayRatio;
  	  	alpha -= alphaDecay;
        if (!policyFrozen) {
            double currentLambda = lambda;
            for (int i = tracingStack.size() - 1; i >= 0; i--) {
                StateActionIndexPair pair = tracingStack.get(i);

                int prevAction = pair.getAction().getInt(0);
                int prevState = pair.getState();

                if (i + 1 == tracingStack.size()) 
                {
                    vf.calculateTerminalErrors(prevAction, prevState, gamma, reward);
                    vf.update(prevAction, prevState, 1.0, alpha);
                } 
                else 
                {
                	// if there is no more recent entry for this state-action pair then update it
                	// this is to implement replacing rather than accumulating traces
                    int index = tracingStack.indexOf(pair, i + 1);
                    if (index == -1) {
                        vf.update(prevAction, prevState, currentLambda, alpha);
                    }
                    currentLambda *= lambda;
                }
            }
        }
       
        if (debugging)
        {
        	System.out.println("Step: " + numOfSteps + "\tImpact: " + reward.getDouble(1) + "\tReward: " + reward.getDouble(0));
        	System.out.println("---------------------------------------------");
        }
    }

    @Override
    public void agent_cleanup() {
        vf = null;
        policyFrozen = false;
    }
    
    private int getAction(int state) {
        ActionSelector valueFunction = (ActionSelector) vf;
        int action;
        if (!policyFrozen)
        {
        	switch (explorationStrategy)
        	{
	        	case TLO_LookupTable.EGREEDY: 
	        		action = valueFunction.choosePossiblyExploratoryAction(epsilon, state); 
	        		break;
	        	case TLO_LookupTable.SOFTMAX_TOURNAMENT: 
	        	case TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON : 
	        		action = valueFunction.choosePossiblyExploratoryAction(temperature, state);
	        		break;
	        	default:
	        		action = -1; // this should never happen - if it does we'll return an invalid value to force the program to halt
        	}
        } 
        else 
        {
        	action = valueFunction.chooseGreedyAction(state);
        }
        
        //DEBUGGING STUFF
        saVisits[state][action]++;
        
        return action;
    }
    
    // returns true if the specified action is amongst the greedy actions for the 
    // specified state, false otherwise
    private boolean isGreedy(int state, int action)
    {
        ActionSelector valueFunction = (ActionSelector) vf;
        return valueFunction.isGreedy(state,action);  	
    }

    @Override
    public String agent_message(String message) {
    	if (message.equals("get_agent_name"))
    	{
    		return "TLO_Expected";
    	}
        if (message.equals("freeze_learning")) {
            policyFrozen = true;
            System.out.println("Learning has been freezed");
            return "message understood, policy frozen";
        }
        if (message.startsWith("change_weights")){
            System.out.print("TLO_Agent_Conditioned_On_Expected_Rewards: Weights can not be changed");
            return "TLO_Agent_Conditioned_On_Expected_Rewards: Weights can not be changed";
        }
        // This is a really important message as its sets the TLO parameters and allows for the creation of the LookUp Table at the
        // right size for the augmented state - this must happen before a trial can be run
        if (message.startsWith("set_TLO_parameters")){
        	System.out.println(message);
        	String[] parts = message.split(" ");
        	// iterate through the thresholded objectives, loading the discretisation and thresholding settings into the arrays
        	for (int i=0; i<numThresholds; i++)
        	{
	        	discretisationMin[i] = Double.valueOf(parts[1]).doubleValue();
	        	discretisationMax[i] = Double.valueOf(parts[2]).doubleValue(); 
	        	numDiscretisations[i] = (int)Double.valueOf(parts[3]).doubleValue();
	        	discretisationGranularity[i] = (discretisationMax[i]-discretisationMin[i])/numDiscretisations[i];
	        	thresholds[i] = Double.valueOf(parts[4]).doubleValue();
	        	System.out.println("Objective " + i +" (min/max/num discretisations/threshold): " + discretisationMin[i] + " / " + discretisationMax[i] + " / " + numDiscretisations[i] + " / " + thresholds[i]);
        	}
            // size the lookup table to account for the additional cells needed for the augmented state
            // each conditioning variable increases the # of states by (n+2) (the extra 2 are required to account for values outside of the clamping range)
            for (int i=0; i<numThresholds; i++)
            {
            	numStates = numEnvtStates * (numDiscretisations[i]+2); // agent state = environmental-state U accumulated-actual-reward
            }	
            vf = new TLOConditionedLookupTable(numOfObjectives, numActions, numStates, 0, thresholds);
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
        	temperatureDecayRatio = Math.pow(0.01/startingTemperature,1.0/numEpisodes);
            System.out.println("Starting temperature changed to " + startingTemperature + " Decay ratio = " + temperatureDecayRatio);
            return "softmax parameters changed";
        } 
        else if (message.equals("start_new_trial")){
        	resetForNewTrial();
            System.out.println("New trial started: Q-values and other variables reset");
            return "New trial started: Q-values and other variables reset";
        }
        else if (message.equals("start-debugging"))
    	{
    		debugging = true;
    		return "Debugging enabled in agent";
    	}
        else if (message.equals("stop-debugging"))
    	{
    		debugging = false;
    		return "Debugging disabled in agent";
    	}
        System.out.println("TLO_Agent_Conditioned_On_Expected_Rewards - unknown message: " + message);
        return "TLO_Agent_Conditioned_On_Expected_Rewards does not understand your message.";
    }
    
    // used for debugging with the ComparisonAgentForDebugging
    // dumps Q-values and feedback on action-selection for the current Observation
    public void dumpInfo(Observation observation, Action thisAction, Action otherAgentAction)
    {
    	int state = getAugmentedStateIndex(observation);
        int action = thisAction.getInt(0);
        int otherAction = otherAgentAction.getInt(0);
        System.out.println("TLO_Agent_Conditioned_On_Expected_Rewards");
		System.out.println("\tEpisode" + numEpisodes + "Step: " + numOfSteps +"\tState: " + "\tAction: " + action);
		System.out.println("\tIs other agent's action greedy for me? " + ((ActionSelector)vf).isGreedy(state,otherAction));
    	for (int i=0; i<numActions; i++)
    	{
    		System.out.print("\t(");
    		double[] q = vf.getQValues(i, state);
    		for (int j=0; j<numOfObjectives; j++)
    		{
    			System.out.print(q[j]+" ");
    		}
    		System.out.print(") ");
    	}
        System.out.println();    	
    }

    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new TLO_Agent_Conditioned_On_Expected_Rewards() );
        theLoader.run();

    }


}
