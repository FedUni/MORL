package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Reward;

import tools.valuefunction.TLO_LookupTable;
import tools.spreadsheet.*;


public class TLOOptionExperiment 
{

    private int whichEpisode = 0;
    private int numObjectives;



    // enable this group of declarations for egreedy exploration
   	//private final int EXPLORATION = TLO_LookupTable.EGREEDY;
    //private final String METHOD_PREFIX = "EGREEDY";
    //private final String PARAM_CHANGE_STRING = "set_egreedy_parameters";
    //private double EXPLORATION_PARAMETER = 0.9;
    
    // enable this group of declarations for softmax-epsilon exploration		    
    //private int EXPLORATION = TLO_LookupTable.SOFTMAX_ADDITIVE_EPSILON;
    //private final String METHOD_PREFIX = "SOFTMAX_E";
    //private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
    //private double EXPLORATION_PARAMETER = 10;
    
    // enable this group of declarations for softmax-epsilon exploration		    
    private int EXPLORATION = TLO_LookupTable.SOFTMAX_TOURNAMENT;
    private final String METHOD_PREFIX = "SOFTMAX_T";
    private final String PARAM_CHANGE_STRING = "set_softmax_parameters";
    private double EXPLORATION_PARAMETER = 10; // usually 10
    private double EXPLORATION_END_PARAMETER = 2; // EXPLORATION_PARAMETER will graduatelly donw to EXPLORATION_END_PARAMETER
    

	// alter these declarations to match the Environment being used	
    
    // Settings for the DST task
    // private final String ENVIRONMENT_PREFIX = "DST";
    // private final int NUM_ONLINE_EPISODES_PER_TRIAL = 10000;
    // private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 1;
    // private final int MAX_EPISODE_LENGTH = 200;
    // private final double[][] TLO_PARAMS = {{-500, 0, 1, -16}};    // array of arrays to allow for multiple threshold objectives - min / max / num discretisations / threshold for each
    
    // Settings for the Stochastic_MOMDP task
    /*private final String ENVIRONMENT_PREFIX = "Stochastic";
    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 1000;
    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 200;
    private final int MAX_EPISODE_LENGTH = 3;
    private final double[][] TLO_PARAMS = {{1.2, 2.8, 1, 4.4}};    // array of arrays to allow for multiple threshold objectives - min / max / num discretisations / threshold for each
    */
    // Settings for the SpaceTraders task (Just for Option Agent)
    private final String ENVIRONMENT_PREFIX = "SpaceTraders-1";
    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 20000;
    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 200;
    private final int MAX_EPISODE_LENGTH = 3;
    private final double [] TLO_PARAMS = {0.88};    // Not need for discretising state only threshold
    
    
    // alter these declarations to determine which form of learning is being used, learning parameters etc
    private final double ALPHA = 0.01;
    private final double ALPHA_DECAY_TO = 0; 
    private final double ALPHA_DECAY = (ALPHA-ALPHA_DECAY_TO)/NUM_ONLINE_EPISODES_PER_TRIAL; //$\lambda$ALPHA / NUM_ONLINE_EPISODES_PER_TRIAL; // set to 0 for no decay
    private final double LAMBDA = 0.95;
    private final double GAMMA = 1.0;
    private final int NUM_TRIALS = 20;
	    
	private final String FILENAME_PREFIX = ENVIRONMENT_PREFIX + "-";
	private ExcelWriter excel;
  
    // store the data for the most recent Reward. The strings indicates a value or label to be written in the first two columns
    private void saveReward(String labels, Reward r)
    {
    	excel.writeNextRowTextAndNumbers(labels, r.doubleArray);
    }    
	    
    // Run One Episode of length maximum cutOff
    private Reward runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);
        int totalSteps = RLGlue.RL_num_steps();
        Reward totalReward = RLGlue.RL_return();
        return totalReward;
    }

    public void runExperiment() {
    	// set up data structures to store reward history
        String taskSpec = RLGlue.RL_init();
        TaskSpecVRLGLUE3 theTaskSpec = new TaskSpecVRLGLUE3(taskSpec);
        numObjectives = theTaskSpec.getNumOfObjectives();
        // configure agents exploration and TLO settings
        String agentMessageString = "set_TLO_parameters ";
        for (int i=0; i<TLO_PARAMS.length; i++)
        {				
            agentMessageString += TLO_PARAMS[i] + " ";
        }
        RLGlue.RL_agent_message(agentMessageString);
        RLGlue.RL_agent_message("set_learning_parameters" + " " + ALPHA + " " + LAMBDA + " " + GAMMA + " " + EXPLORATION + " " + ALPHA_DECAY);
        RLGlue.RL_agent_message(PARAM_CHANGE_STRING + " " + EXPLORATION_PARAMETER + " " + NUM_ONLINE_EPISODES_PER_TRIAL + " " +EXPLORATION_END_PARAMETER); 
        // set up the output Excel file
    	String agentName = RLGlue.RL_agent_message("get_agent_name");
    	final String fileName = FILENAME_PREFIX+"-"+agentName+"-"+METHOD_PREFIX+EXPLORATION_PARAMETER+"-alpha"+ALPHA+"-lambda"+LAMBDA+"-threshold"+TLO_PARAMS[0];
    	excel = new JxlExcelWriter(fileName);
    	String objectivesString="";
    	for (int i=0; i<numObjectives; i++)
    	{
    		objectivesString+="&Obj " + (i+1);
    	}
        // run the trials
        for (int trial=0; trial<NUM_TRIALS; trial++)
        {
        	// start new excel sheet and include header row
        	excel.moveToNewSheet("Trial"+trial, trial);
        	excel.writeNextRowText(" &Episode number &Policy &II_0&II_1 &ID_0&ID_1 &IT_0&IT_1 &DI_0&DI_1 &DD_0&DD_1 &DT_0&DT_1 &TI_0&TI_1 &TD_0&TD_1 &TT_0&TT_1"+objectivesString);
        	// run the trial and save the results to the spreadsheet
        	System.out.println("Trial " + trial);
            RLGlue.RL_agent_message("start_new_trial");
    		for (int episodeNum=0; episodeNum<NUM_ONLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
                Reward data = runEpisode(MAX_EPISODE_LENGTH);
                String policy = RLGlue.RL_agent_message("print_policy");
                String qValue = RLGlue.RL_agent_message("get_q_value");
                saveReward("Online&"+(1+episodeNum)+policy+qValue,data);
    		}
            RLGlue.RL_agent_message("freeze_learning");		// turn off learning and exploration for offline assessment of the final policy    		
            for (int episodeNum=0; episodeNum<NUM_OFFLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
    			// turn on debugging for the final offline run
            	// if (episodeNum==NUM_OFFLINE_EPISODES_PER_TRIAL-1)
            	// {
	            // 	//RLGlue.RL_env_message("start-debugging");
	    		// 	//RLGlue.RL_agent_message("start-debugging");
            	// }
                Reward data = runEpisode(MAX_EPISODE_LENGTH);
                String policy = RLGlue.RL_agent_message("print_policy");
                String qValue = RLGlue.RL_agent_message("get_q_value");
    			saveReward("Offline&"+(1+episodeNum)+policy+qValue,data);
    		}
        	//RLGlue.RL_env_message("stop-debugging");
			//RLGlue.RL_agent_message("stop-debugging");           
            // add two rows at the end of the worksheet to summarise the means over all online and offline episodes
            // String formulas = "";
            // for (int i=0; i<numObjectives; i++)
            // {
            // 	formulas +="AVERAGE(" + excel.getAddress(i+2,1) + ":" + excel.getAddress(i+2,NUM_ONLINE_EPISODES_PER_TRIAL) + ")&";
            // }
            // excel.writeNextRowTextAndFormula("Mean over all online episodes& ", formulas);
            // formulas = "";
            // for (int i=0; i<numObjectives; i++)
            // {
            // 	formulas +="AVERAGE(" + excel.getAddress(i+2,NUM_ONLINE_EPISODES_PER_TRIAL+1) + ":" + excel.getAddress(i+2,NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL) + ")&";
            // }
            // excel.writeNextRowTextAndFormula("Mean over all offline episodes& ", formulas);

            ////
            //RLGlue.RL_agent_message("print_lookup_table");	
        }
        // // make summary sheet - the +2 on the number of rows is to capture the online and offline means as well as the individual episode results
        // excel.makeSummarySheet(NUM_TRIALS, objectivesString, 2, 1, numObjectives, NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL+2);           
        // // make another sheet which collates the online and off-line per episode means across all trials, for later use in doing t-tests
        // excel.moveToNewSheet("Collated", NUM_TRIALS+1); // put this after the summary sheet
        // String onlineHeader="";
        // String offlineHeader="";
        // for (int i=0; i<numObjectives; i++)
        // {
        // 	onlineHeader+= "Online mean Obj " + (i+1) +"&";
        // 	offlineHeader+= "Offline mean Obj " + (i+1) +"&";
        // }
        // excel.writeNextRowText("Trial&" + onlineHeader + offlineHeader);
        // final int ONLINE_ROW = NUM_ONLINE_EPISODES_PER_TRIAL+NUM_OFFLINE_EPISODES_PER_TRIAL+1;
        // final int OFFLINE_ROW = ONLINE_ROW+1;
        // for (int i=0; i<NUM_TRIALS; i++)
        // {
        // 	String text = Integer.toString(i);
        // 	String onlineLookups = "";
        // 	String offlineLookups = "";
        // 	for (int j=0; j<numObjectives; j++)
        // 	{
        // 		onlineLookups+= excel.getAddress(i,j+2,ONLINE_ROW) + "&";
        // 		offlineLookups+= excel.getAddress(i,j+2,OFFLINE_ROW) + "&";
        // 	}
        // 	excel.writeNextRowTextAndFormula(text, onlineLookups + offlineLookups);
        // }
        excel.closeFile();
        RLGlue.RL_cleanup();
        System.out.println("********************************************** Experiment finished");
    }

    public static void main(String[] args) {
    	TLOOptionExperiment theExperiment = new TLOOptionExperiment();
        theExperiment.runExperiment();
        System.exit(0); // shut down the experiment + hopefully everything else launched by the Driver program (server, agent, environment)
    }
}

