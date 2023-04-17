package experiments;

import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Reward;

import tools.valuefunction.TLO_LookupTable;
import tools.spreadsheet.*;


public class TLOMOSSTPExperiment 
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
    // Settings for the SpaceTraders task
    private final String ENVIRONMENT_PREFIX = "SpaceTraders0";
    private final int NUM_ONLINE_EPISODES_PER_TRIAL = 20000;
    private final int NUM_DATA_GATHERING_PHASE = 100;
    private final int NUM_LEARNING_PHASE = 1000;
    private final int NUM_OFFLINE_EPISODES_PER_TRIAL = 200;
    private final int MAX_EPISODE_LENGTH = 3;
    private final double[][] TLO_PARAMS = {{-1, 2, 1, 0.88}};    // array of arrays to allow for multiple threshold objectives - min / max / num discretisations / threshold for each
    

    
    
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
        	for (int j=0; j<TLO_PARAMS[i].length; j++)
			{
				agentMessageString += TLO_PARAMS[i][j] + " ";
			}
        }
        RLGlue.RL_agent_message(agentMessageString);
        RLGlue.RL_agent_message("set_learning_parameters" + " " + ALPHA + " " + LAMBDA + " " + GAMMA + " " + EXPLORATION + " " + ALPHA_DECAY + " " + NUM_DATA_GATHERING_PHASE + " " + NUM_LEARNING_PHASE);
        RLGlue.RL_agent_message(PARAM_CHANGE_STRING + " " + EXPLORATION_PARAMETER + " " + NUM_ONLINE_EPISODES_PER_TRIAL + " " +EXPLORATION_END_PARAMETER); 
        
        // set up the output Excel file
    	String agentName = RLGlue.RL_agent_message("get_agent_name");
    	final String fileName = FILENAME_PREFIX+"-"+agentName+"-"+METHOD_PREFIX+EXPLORATION_PARAMETER+"-alpha"+ALPHA+"-lambda"+LAMBDA+"-threshold"+TLO_PARAMS[0][3]+"-phase D"+NUM_DATA_GATHERING_PHASE+"+ L"+NUM_LEARNING_PHASE;
    	excel = new JxlExcelWriter(fileName);
    	String objectivesString="";
        String data ="";
        Reward totalReward;
    	for (int i=0; i<numObjectives; i++)
    	{
    		objectivesString+="&Obj " + (i+1);
    	}

        int phaseIndicater;
        int oneCyclePhase = NUM_DATA_GATHERING_PHASE + NUM_LEARNING_PHASE;
        // run the trials
        for (int trial=0; trial<NUM_TRIALS; trial++)
        {
        	// start new excel sheet and include header row
        	excel.moveToNewSheet("Trial"+trial, trial);
        	excel.writeNextRowText(" &Episode number&Probability&I_A&D_A&T_A&I_B&D_B&T_B&OP"+objectivesString);
        	// run the trial and save the results to the spreadsheet
        	System.out.println("Trial " + trial);
            RLGlue.RL_agent_message("start_new_trial");
    		for (int episodeNum=0; episodeNum<NUM_ONLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
                // 0 ~ (oneCyclePhase-1)
                phaseIndicater = episodeNum%oneCyclePhase;
                
                if(phaseIndicater<NUM_DATA_GATHERING_PHASE){
                    RLGlue.RL_agent_message("data_gathering_phase");
                    totalReward = runEpisode(MAX_EPISODE_LENGTH);
                    data = RLGlue.RL_agent_message("print_data");
                    saveReward("Data-gather phase&"+(1+episodeNum)+"&"+data,totalReward);
                }else{
                    RLGlue.RL_agent_message("learning_phase");
                    totalReward = runEpisode(MAX_EPISODE_LENGTH);
                    data = RLGlue.RL_agent_message("print_data");
                    saveReward("learning phase&"+(1+episodeNum)+"&"+data,totalReward);
                }
                //RLGlue.RL_agent_message("print_data");

                // int batch = (episodeNum-phaseIndicater)/oneCyclePhase + 1;
                // if(phaseIndicater==(NUM_DATA_GATHERING_PHASE-1)){
                //     System.out.println("End of data gathering phase " + batch);
                //     //RLGlue.RL_agent_message("print_lookup_table");
                //     RLGlue.RL_agent_message("print_data");
                // }
                // if(phaseIndicater==(oneCyclePhase-1)){
                //     System.out.println("End of learning phase " + batch);
                //     //RLGlue.RL_agent_message("print_lookup_table");
                //     RLGlue.RL_agent_message("print_data");
                // }
                
    		}
            RLGlue.RL_agent_message("freeze_learning");		// turn off learning and exploration for offline assessment of the final policy    		
            for (int episodeNum=0; episodeNum<NUM_OFFLINE_EPISODES_PER_TRIAL; episodeNum++)
    		{
                totalReward = runEpisode(MAX_EPISODE_LENGTH);
                data = RLGlue.RL_agent_message("print_data");
    			saveReward("Offline&"+(1+episodeNum)+"&"+data,totalReward);
    		}
       
            // // add two rows at the end of the worksheet to summarise the means over all online and offline episodes
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

            //System.out.println("End of learning");
            //RLGlue.RL_agent_message("print_lookup_table");	
        }
        // make summary sheet - the +2 on the number of rows is to capture the online and offline means as well as the individual episode results
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
    	TLOMOSSTPExperiment theExperiment = new TLOMOSSTPExperiment();
        theExperiment.runExperiment();
        System.exit(0); // shut down the experiment + hopefully everything else launched by the Driver program (server, agent, environment)
    }
}

