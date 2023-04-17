// Implementation of a TLO_Agent_MOSS which uses an augmented state based on accumulated expected rewards 
// and global statistics "total number of episodes experienced", "estimate of the average per-episode return"
// "state visited count", "estimated average return in those episodes", "estimated rewards accumulated prior to reaching this state"
// to get around issues caused by non-additivity of rewards in the Bellman equation 
// this version is specifically designed to find the SER-optimal deterministic policy in stochastic environment. 
// VERY IMPORTANT: The set_TLO_Parameters message must be called to supply these parameters before a trial can be run as the sizing of the
// lookup table depends on those values. It also needs to be called before setting the learning parameters.

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
import tools.valuefunction.TLO_MOSS_LookupTable;
import tools.valuefunction.TLO_LookupTable;
import tools.valuefunction.interfaces.ActionSelector;

public class TLO_Agent_MOSS implements AgentInterface 
{

	// Parameters for the discretisation of the accumulated rewards - all arrays so they can be set independently for each objective that
	// is being thresholded
	double discretisationMin[], discretisationMax[], discretisationGranularity[];
    int numDiscretisations[];
    double thresholds[];
    int numThresholds;
    double accumulatedRewards[]; // accumulated rewards in the current episode P

	// as well as the standard Q-table we also need to learn the global statistics
	double expectedAccumlatedRewards[][]; // [state][objective] //expected cumulative reward when s is reached //P_o(s)
	int stateVisits[]; //[state] //count of visits to s //v(s)
	double estimatedOverallReturn []; // [objective] //estimated return over all episodes //E_\pi
    double estimatedOverallStatedReturn [][]; // [state][objective] // estimated average return in those episodes //E(s)

	//double meanOverallReturn[];// [numThresholds]

	int stateFlag[]; //[state] //binary flag - was s visited in this episode? //b(s)
	int lastState, lastAction; // the most recent environmental state and action
	//double conditioningValues[][]; //[state] [numThresholds]
    
	TLO_MOSS_LookupTable vf = null;
	Stack<StateActionIndexPair> tracingStack = null;

	private boolean policyFrozen = false;

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
    double endingTemperature;
    double temperatureDecayRatio;
    double temperature;
    
    double startingAlpha, alpha, alphaDecay;
    double gamma;
    double lambda;
    final int MAX_STACK_SIZE = 20;

    int numOfSteps;
    int numEpisodes; //count of all episodes v_\pi

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

        expectedAccumlatedRewards = new double [numEnvtStates][numThresholds];
    	stateVisits = new int[numEnvtStates];
        estimatedOverallReturn = new double [numThresholds];
        estimatedOverallStatedReturn = new double [numEnvtStates][numThresholds];
        stateFlag = new int [numEnvtStates];
    }

    @Override
    public void agent_init(String taskSpecification) {
    	System.out.println("MOSS Agent launched");
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpecification);

        numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;
        numEnvtStates = (theTaskSpec.getDiscreteObservationRange(0).getMax()+1);
        numOfObjectives = theTaskSpec.getNumOfObjectives();
        numThresholds = numOfObjectives-1;
        createArrays();
        // we aren't creating the lookup table at this stage - need to defer that until we get the message telling us how many
        // discretisations levels are being used for each objective, and what the thresholds are - that needs to happen before the 
        // first trial is started

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

        for (int i=0; i<numThresholds; i++){
            accumulatedRewards[i]=0;
            estimatedOverallReturn[i] = 0;
            for (int s=0; s<numEnvtStates; s++){
                expectedAccumlatedRewards[s][i] = 0;
                estimatedOverallStatedReturn[s][i] = 0;
            }
        }
        for (int s=0; s<numEnvtStates; s++)
        {
        	stateVisits[s] = 0;
        }

        vf.setConditioningValues(accumulatedRewards,1,accumulatedRewards);
        // reset Q-values 
        vf.resetQValues(initQValues);
    }
    
    private void resetForNewEpisode()
    {
        if (!policyFrozen){
            numEpisodes++;
        }
        numOfSteps = 0; 
        for (int i=0; i<numThresholds; i++)
        {
            // sums of prior rewards
        	accumulatedRewards[i]=0;
        }
        for (int s=0; s<numEnvtStates; s++)
        {
        	stateFlag[s]=0;
        }
        vf.setConditioningValues(accumulatedRewards,1,accumulatedRewards);
        // reset the tracingStack
        tracingStack.clear();
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
    private int getAugmentedStateIndex(int envStateS, double[] accumulatedRewardsP)
    {
    	int rewardState;
    	int augmentedState = envStateS; // get the envt state index
    	int totalStatesSoFar = numEnvtStates;
    	// repeat for each thresholded objective - discretise its current value, and apply this
    	for (int i=0; i<numThresholds; i++)
    	{
    		rewardState = discretise(accumulatedRewardsP[i],discretisationMin[i],discretisationMax[i],discretisationGranularity[i],numDiscretisations[i]);
    		augmentedState += rewardState * totalStatesSoFar;
    		totalStatesSoFar = totalStatesSoFar * (numDiscretisations[i]+2);
    	}
    	return augmentedState;
    }

    // input: state s, accumulated rewards in the current episode P
    private int updateStatisticsHelper(int envStateS, double[] accumulatedRewardsP){

        if (!policyFrozen){
            if(stateFlag[envStateS]==0){
                stateVisits[envStateS]++;
                stateFlag[envStateS] = 1;
            }
            for (int i=0; i<numThresholds; i++){
                expectedAccumlatedRewards[envStateS][i] = expectedAccumlatedRewards[envStateS][i] + alpha*(accumulatedRewardsP[i] - expectedAccumlatedRewards[envStateS][i]);
                //expectedAccumlatedRewards[envStateS][i] = expectedAccumlatedRewards[envStateS][i] + ((double)1/stateVisits[envStateS])*(accumulatedRewardsP[i] - expectedAccumlatedRewards[envStateS][i]);
            }
        }
        // estimated probability of visiting s in any episode
        double estimatedProbability = (double)stateVisits[envStateS]/numEpisodes;
        // estimated return in episodes where s is not visited
        double estimatedReturnNotVisited[] = new double [numOfObjectives];  
        for (int i=0; i<numThresholds; i++){
            if(estimatedProbability!=1){
                estimatedReturnNotVisited[i] = (estimatedOverallReturn[i] - estimatedProbability*estimatedOverallStatedReturn[envStateS][i])/(1-estimatedProbability);
            }else{
                estimatedReturnNotVisited[i]=0;
            }
            
        }

        int augmentedState = getAugmentedStateIndex(envStateS, expectedAccumlatedRewards[envStateS]);

        vf.setConditioningValues(expectedAccumlatedRewards[envStateS], estimatedProbability, estimatedReturnNotVisited);

        return augmentedState;
    }
    
    @Override
    public Action agent_start(Observation observation) {

    	resetForNewEpisode();
        int envState = stateConverter.getStateNumber( observation );
        int state = updateStatisticsHelper(envState, accumulatedRewards);
        int action = getAction(state);

        Action returnAction = new Action(1, 0, 0);
        returnAction.intArray[0] = action;
        tracingStack.add(new StateActionIndexPair(state, returnAction)); // put executed action on the stack

        lastAction = action;
        return returnAction;
    }

    @Override
    public Action agent_step(Reward reward, Observation observation) 
    {
        numOfSteps++;
        for (int i=0; i<numThresholds; i++)
        {
            accumulatedRewards[i] += reward.getDouble(i);
        }
        lastState = stateConverter.getStateNumber( observation ); // get the envt state index
        int state = updateStatisticsHelper(lastState, accumulatedRewards);
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

    	// store info needed for updating immediate-rewards on the next time-step
        lastState = stateConverter.getStateNumber( observation ); // get the envt state index
        lastAction = action;
        //System.out.println("\tAction\t" + action);
        return returnAction;
    }

    @Override
    public void agent_end(Reward reward) 
    {
  	  	numOfSteps++;
        // update estimate of immediate reward for the previous state-action 
        for (int i=0; i<numThresholds; i++)
        {
            accumulatedRewards[i] += reward.getDouble(i);
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

            for(int o=0; o<numThresholds; o++){
                estimatedOverallReturn[o] += alpha*(accumulatedRewards[o]-estimatedOverallReturn[o]);
                //estimatedOverallReturn[o] += ((double)1/numEpisodes)*(accumulatedRewards[o]-estimatedOverallReturn[o]);
                for(int s=0; s<numEnvtStates; s++){
                    if(stateFlag[s]!=0){
                        estimatedOverallStatedReturn [s][o] += alpha*(accumulatedRewards[o]-estimatedOverallStatedReturn [s][o]);
                        //estimatedOverallStatedReturn [s][o] += ((double)1/stateVisits[s])*(accumulatedRewards[o]-estimatedOverallStatedReturn [s][o]);
                    }
                }
            }
        }
        
    }

    @Override
    public void agent_cleanup() {
        vf = null;
        policyFrozen = false;
    }
    
    private int getAction(int state) {
        //ActionSelector valueFunction = (ActionSelector) vf;
        int action;
        if (!policyFrozen)
        {
        	switch (explorationStrategy)
        	{
	        	case TLO_LookupTable.EGREEDY: 
	        		action = vf.choosePossiblyExploratoryAction(epsilon, state); 
	        		break;
	        	case TLO_LookupTable.SOFTMAX_TOURNAMENT: 
                    //action = valueFunction.choosePossiblyExploratoryAction(temperature, state);
                    //break;
	        	case TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON : 
	        		action = vf.choosePossiblyExploratoryAction(temperature, state);
	        		break;
	        	default:
	        		action = -1; // this should never happen - if it does we'll return an invalid value to force the program to halt
        	}
        } 
        else 
        {
        	action = vf.chooseGreedyAction(state);
        }
        
        return action;
    }
    
    private int getGreedyAction(int envStateS){
        // estimated probability of visiting s in any episode
        double estimatedProbability = (double)stateVisits[envStateS]/numEpisodes;
        // estimated return in episodes where s is not visited
        double estimatedReturnNotVisited[] = new double [numOfObjectives];  
        for (int i=0; i<numThresholds; i++){
            if(estimatedProbability!=1){
                estimatedReturnNotVisited[i] = (estimatedOverallReturn[i] - estimatedProbability*estimatedOverallStatedReturn[envStateS][i])/(1-estimatedProbability);
            }else{
                estimatedReturnNotVisited[i]=0;
            }
            
        }

        int augmentedState = getAugmentedStateIndex(envStateS, expectedAccumlatedRewards[envStateS]);

        vf.setConditioningValues(expectedAccumlatedRewards[envStateS], estimatedProbability, estimatedReturnNotVisited);

        return ((ActionSelector)vf).chooseGreedyAction(augmentedState);
    }

    // returns true if the specified action is amongst the greedy actions for the 
    // specified state, false otherwise
    private boolean isGreedy(int state, int action){
        ActionSelector valueFunction = (ActionSelector) vf;
        return valueFunction.isGreedy(state,action);  	
    }

    @Override
    public String agent_message(String message) {
    	if (message.equals("get_agent_name")){
    		return "TLO_MOSS";
    	}
        if (message.equals("freeze_learning")) {
            policyFrozen = true;
            System.out.println("Learning has been freezed");
            return "message understood, policy frozen";
        }
        if (message.startsWith("change_weights")){
            System.out.print("TLO_Agent_MOSS: Weights can not be changed");
            return "TLO_Agent_MOSS: Weights can not be changed";
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
                ///////////////// should be numStates * (numDiscretisations[i]+2)
            	numStates = numEnvtStates * (numDiscretisations[i]+2); // agent state = environmental-state U accumulated-actual-reward
            }	
            vf = new TLO_MOSS_LookupTable(numOfObjectives, numActions, numStates, 0, thresholds);
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

            System.out.println("Print estimatedOverallStatedReturn");
            for(int s=0; s<numEnvtStates; s++)
            {
                System.out.println("numEnvtStates "+s);
                for(int i=0; i<numThresholds; i++){
                    System.out.print(" "+estimatedOverallStatedReturn[s][i]+" ");
                }
                System.out.println();
            }
            System.out.println("Print expectedAccumlatedRewards");
            for(int s=0; s<numEnvtStates; s++)
            {
                System.out.println("numEnvtStates "+s);
                for(int i=0; i<numThresholds; i++){
                    System.out.print(" "+expectedAccumlatedRewards[s][i]+" ");
                }
                System.out.println();
            }
            System.out.println("Print stateVisits");
            for(int s=0; s<numEnvtStates; s++)
            {
                System.out.println("numEnvtStates "+s+" "+ stateVisits[s]);
            }
            
            System.out.println("Augmented Q value");
            for(int s=0; s<numEnvtStates; s++){
                System.out.println("State " + s + ", ");
                for(int a=0; a<numActions; a++){
                    System.out.println("Action " + a + ", ");
                    System.out.print("[");
                    double qValues[] = vf.getQValues(a, s+3);
                    for (int i = 0; i < numThresholds; i++) 
                    {
                        double probability = (double)stateVisits[s]/numEpisodes;
                        double estimatedReturnNotVisited; 
                        if(probability==1){
                            estimatedReturnNotVisited = 0;
                        }else{
                            estimatedReturnNotVisited = (estimatedOverallReturn[i] - probability*estimatedOverallStatedReturn[s][i])/(1-probability);
                        }
                        double augmentedQValue = probability*(qValues[i] + 0) + (1-probability)*estimatedReturnNotVisited;
                        System.out.print("probability: "+probability+" ");
                        System.out.print("qValues: "+qValues[i]+" ");
                        System.out.print("value: "+(estimatedOverallReturn[i] - probability*estimatedOverallStatedReturn[s][i])+" ");
                        System.out.print("estimatedReturnNotVisited: "+estimatedReturnNotVisited+" ");
                        System.out.print(augmentedQValue+" ");
                    } 
                    System.out.print("], ");
                    System.out.println("");
                }
            }

            return "Print the lookup table";
        }
        if(message.equals("print_policy")){

            int action1 = getGreedyAction(0);

            int action2 = getGreedyAction(1);

            int policy = action1*3 + action2;

            return "&"+String.valueOf(policy)+"&"+String.valueOf(action1)+"&"+String.valueOf(action2);
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
        System.out.println("TLO_Agent_MOSS - unknown message: " + message);
        return "TLO_Agent_MOSS does not understand your message.";
    }
    

    public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader( new TLO_Agent_MOSS() );
        theLoader.run();
    }
}
