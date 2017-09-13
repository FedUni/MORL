// Designed for use in demonstrating the capabilities of MORL_Glue.
// Runs TLO agent on different instances of the Generalised Deep Sea Treasure environment, 
// while producing graphs of the additive-epsilon metric over time, to visualise the
// impact of state-size on the agent's performance.
// Note that this experiment is purely for demonstration purposes, and makes use of
// some features which may not be appropriate for actual research experiments, such as
// assuming knowledge of the actual Pareto-front when setting threshold values for TLO.
// Written by Peter Vamplew, August 2017


package experiments;

import java.awt.Color;
import java.util.StringTokenizer;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Reward;

import tools.valuefunction.TLO_LookupTable;
import agents.TLO_Agent;
import env.GeneralisedDeepSeaTreasureEnv;
import tools.visualisers.*;

public class DemoExperiment 
{
	// array holding parameter settings for various instantiations of the GeneralisedDeepSeaTreasure environment
	// order is _width, _minDepth, _minVerticalStep, _maxVerticalStep, _transitionNoise, _rewardNoise, _frontShape, _seed
	private String envtSettings[] = {
			"3 3 1 3 0.0 0.0 " +GeneralisedDeepSeaTreasureEnv.CONCAVE + " 471",
			"5 3 1 3 0.0 0.0 " +GeneralisedDeepSeaTreasureEnv.CONCAVE + " 471",
			"7 3 1 3 0.0 0.0 " +GeneralisedDeepSeaTreasureEnv.CONCAVE + " 471"
	};
	private String envtNames[] = {"Size = 3","Size = 5","Size = 7"};
	private Color[] seriesColours = {Color.RED,Color.GREEN,Color.BLUE};
	// parameters for the experiment
	private final int MAX_EPISODES = 10000;
	private final int BATCH_SIZE = 5;
	private final int NUM_BATCHES = MAX_EPISODES / BATCH_SIZE;
	private final int MAX_EPISODE_LENGTH = 500;
	private final double INIT_Q_VALUES[] = {0.0,0.0};
    private final double ALPHA = 0.9;
    private final double LAMBDA = 0.9;
    private final double GAMMA = 1.0;
    private final double STARTING_EPSILON = 0.9;
    private final boolean TRACE_TYPE = TLO_Agent.WATKINS; // either .WATKINS (clear traces on non-greedy action) or .PENG (don't clear)
   	private final int EXPLORATION = TLO_LookupTable.EGREEDY;
	
    int numOfObjectives;
    int numStates;
    boolean knownParetoSet;
    private double paretoSet[][]; // stores the Pareto set (if any) returned by the environment via the TaskSpec object
    private double agentSet[][]; // stores the agent's current approximation to the Pareto set
    private double[][] additiveEHistory = new double[envtSettings.length][NUM_BATCHES]; // store the additiveEpsilon over time for each version of the environment, for use in graphing
    private int[] additiveEHistoryLength = new int[envtSettings.length]; // store the number of used elements in each row of the history array

    private Experiment_Frame frame; // for displaying results
    
    // extracts details of the environmental settings from the provided String
    // first entry is the number of states in the environment
    // second is size of the Pareto set - 0 indicates set is unknown
    // any subsequent entries define the actual Pareto set values
    private void processTaskSpecString(String taskSpecString)
    {
    	StringTokenizer t = new StringTokenizer(taskSpecString); // String will consist of space-separated values
    	numStates = Integer.parseInt(t.nextToken());
    	System.out.println("Envt state size = " + numStates);    	
    	int paretoSetSize = Integer.parseInt(t.nextToken());
    	System.out.println("Pareto set size = " + paretoSetSize);
        knownParetoSet = paretoSetSize>0;
        if (knownParetoSet) // there is a known Pareto set
        {

        	paretoSet = new double[paretoSetSize][numOfObjectives];
        	for (int i=0; i<paretoSetSize; i++)
        	{
        		for (int j=0; j<numOfObjectives; j++)
        		{
        			paretoSet[i][j] = Double.parseDouble(t.nextToken());
        		}
            	System.out.println("PS = " + doubleArrayToString(paretoSet[i],' '));
        	}
        } 
        else // clear the paretoSet array
        {
        	paretoSet = null;
        }
    }
    
    // returns a String which concatenates together the values in the provided array, separated by the specified  character (will usually be ' ', '_' or ','), with a separator character at the start of the string
    private String doubleArrayToString(double array[], char separator)
    {
    	String returnString = "";
    	for (int i=0; i<array.length; i++)
    	{
    		returnString = returnString + separator + array[i];
    	}
    	return returnString;
    }
    
    // Runs numBatch episodes of up to stepLimit length while learning.
    // Then freezes the agent and turns off exploration and runs one greedy episode to
    // assess if the agent has converged to the target policy. Returns true if it has,
    // false otherwise. Also stores the reward from the greedy episode in the
    // greedyReward array
    private boolean runBatch(int batchSize, int stepLimit, double[] targetReward, double[] greedyReward) 
    {
    	// run learning episodes
    	RLGlue.RL_agent_message("unfreeze_learning");
    	for (int b=0; b<batchSize; b++)
    	{
	        int terminal = RLGlue.RL_episode(stepLimit);
	        int totalSteps = RLGlue.RL_num_steps();
	        Reward totalReward = RLGlue.RL_return();
	        //System.out.println();
	        //System.out.println(doubleArrayToString(totalReward.doubleArray,','));
    	}
    	// now run one greedy episode for assessment purposes
    	RLGlue.RL_agent_message("freeze_learning");
        int terminal = RLGlue.RL_episode(stepLimit);
        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();  	
        // check if the agent has reached the target reward
        boolean converged = true;
        for (int o=0; o<targetReward.length; o++)
        {
        	greedyReward[o] = totalReward.getDouble(o);
        	if (totalReward.getDouble(o)<targetReward[o])
        		converged = false;
        }
        return converged;
    }
    
    // Run a trial of a learning agent on the current instance of the environment, with
    // the TLO parameters derived from the current Pareto-set target point 
    private void runTrial(int paretoIndex, int envtNumber)
    {   
    	// set threshold based on specified target values, and reset agent for a new trial
    	double threshold[] = new double[numOfObjectives-1]; // the last objective is never thresholded
    	if (paretoIndex==0)
    	{
    		for (int i=0; i<threshold.length; i++)
    			threshold[i] = paretoSet[paretoIndex][i] - 10; // set threshold below any actual rewards
    	}
    	else // place threshold between the last Pareto point and this one
    	{
    		for (int i=0; i<threshold.length; i++)
    			threshold[i] = 0.5 * (paretoSet[paretoIndex][i] + paretoSet[paretoIndex-1][i]);
    	}
		RLGlue.RL_agent_message("change_thresholds" + doubleArrayToString(threshold, ' '));
		// now start a new trial using those threshold settings
        RLGlue.RL_agent_message("start_new_trial");
        boolean converged = false;
        int batch = 0;
        do
        {
        	System.out.print("Batch " + batch + "\t");
            int cumulativeBatch = additiveEHistoryLength[envtNumber];
        	converged = runBatch(BATCH_SIZE,MAX_EPISODE_LENGTH,paretoSet[paretoIndex],agentSet[paretoIndex]);
        	// calculate and display the additiveEpsilon measure for the front found so far
        	additiveEHistory[envtNumber][cumulativeBatch]=additiveEpsilon(paretoSet,agentSet,paretoIndex+1);
        	additiveEHistoryLength[envtNumber]++;
        	// PRINT OUT FOR NOW _ REPLACE WITH UPDATING GRAPH PANEL ONCE WORKING
        	System.out.print(doubleArrayToString(agentSet[paretoIndex],' '));
        	System.out.print("E+ = " + additiveEHistory[envtNumber][cumulativeBatch]);
        	System.out.println();
        	frame.updateData(additiveEHistory, additiveEHistoryLength);
        	batch++;
        } while (batch < NUM_BATCHES && !converged);
    }
    
    
    // Instantiates a specific instance of the generalised benchmark problem, then runs
    // a set of episodes for each of the known Pareto-set points of that environment.
    // Ends when the agent has found each of those points.
    private void runEnvironment(int envtNumber)
    {
        // switch to next set of parameters for the generalised environment
        String taskSpecString = RLGlue.RL_env_message("change_settings " + envtSettings[envtNumber]);
        processTaskSpecString(taskSpecString);
        // notify the agent of the change in state size, and reset Q-values
        RLGlue.RL_agent_message("set_num_states " + numStates);	
        // init array to store the agent's solutions
        agentSet = new double[paretoSet.length][numOfObjectives];
        // run a separate trial for each of the Pareto-set points
    	for (int i=0; i<paretoSet.length; i++)
    	{
    		runTrial(i, envtNumber);
    	}	
    }

    public void runExperiment() 
    {
    	// do initial set-up
    	System.out.println("Demo experiment!!!!");
        String taskSpec = RLGlue.RL_init();
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpec);
        numOfObjectives = theTaskSpec.getNumOfObjectives();
        // set up the display
        frame = new Experiment_Frame("Additive epsilon",50,1000,additiveEHistory,additiveEHistoryLength,envtNames,seriesColours);
        
        // send details of the fixed parameter settings to the agent
        String agentMessageString = "set_learning_parameters" + " " + ALPHA + " " + LAMBDA + " " + GAMMA + " " + EXPLORATION + " " + TRACE_TYPE + doubleArrayToString(INIT_Q_VALUES, ' ');
        RLGlue.RL_agent_message(agentMessageString);
        RLGlue.RL_agent_message("set_egreedy_parameters " + STARTING_EPSILON + " " + MAX_EPISODES);      
        // iterate through each of the different variations of the generalised environment
        for (int envt = 0; envt < envtSettings.length; envt++)
        {
        	runEnvironment(envt); 
        }
        System.out.println("Finished");
        RLGlue.RL_cleanup();   
    }
    
    // Provides a scalar measure of difference between the agent's reward
    // and a target reward value
    private double additiveEpsilon(double thisReward[], double target[])
    {
    	double score = 0.0;
    	for (int i=0; i<target.length; i++)
    	{
    		double diff = target[i] - thisReward[i];
    		if (diff>score)
    		{
    			score = diff;
    		}
    	}
    	return score;
    }
    
    // Calculates the maximum additive epsilon measure over two sets of solutions (the actual front
    // and the approximation learned by the agent
    private double additiveEpsilon(double paretoSet[][], double agentSet[][], int numAgentSolutions)
    {
    	double maxScore = 0.0;
    	for (int i=0; i<paretoSet.length; i++)
    	{
    		double minScore = Double.MAX_VALUE;
    		for (int j=0; j<numAgentSolutions; j++)
    		{
	    		double score = additiveEpsilon(agentSet[j],paretoSet[i]);
	    		if (score<minScore)
	    		{
	    			minScore = score;
	    		}
    		}
    		if (minScore>maxScore)
    		{
    			maxScore = minScore;
    		}
    	}
    	return maxScore;
    }   

    public static void main(String[] args) {
        DemoExperiment theExperiment = new DemoExperiment();
        theExperiment.runExperiment();
    }
}
