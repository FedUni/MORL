/*
Copyright 2007 Brian Tanner
http://rl-library.googlecode.com/
brian@tannerpages.com
http://brian.tannerpages.com
 * July 2007
 * This is the Java Version MountainCar Domain from the RL-Library.  
 * Brian Tanner ported it from the Existing RL-Library to Java.
 * I found it here: http://rlai.cs.ualberta.ca/RLR/environment.html

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

/* Modified to a multiobjective context by Peter Vamplew, April 2015. I also stripped out the RLViz code /*
package org.rlcommunity.environments.mountaincar;*/

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



public class MOMountainCarDiscretised implements EnvironmentInterface
{

    static final int numActions = 3;
    static final int NUM_CELLS = 6; //specifies the degree of quantisation in each dimension of state space
    private MountainCarState theState;
    //Problem parameters have been moved to MountainCarState
    private Random randomGenerator = new Random();

   /* public static TaskSpecPayload getTaskSpecPayload(ParameterHolder P) {
        MountainCar theMC = new MountainCar(P);
        String taskSpecString = theMC.makeTaskSpec().getStringRepresentation();
        return new TaskSpecPayload(taskSpecString, false, "");
    }*/

    public String env_init() 
    {
        //initialize the problem
        theState = new MountainCarState(false, 0.0, 0L); // no random starts, no noise, 0L as default random seed
        //Task specification object
        TaskSpecVRLGLUE3 theTaskSpecObject = new TaskSpecVRLGLUE3();
        theTaskSpecObject.setEpisodic();
        //Specify that there will be this number of observations
        theTaskSpecObject.addDiscreteObservation(new IntRange(0, NUM_CELLS*NUM_CELLS - 1));              
        //Specify that there will be an integer action [0,2]
        theTaskSpecObject.addDiscreteAction(new IntRange(0, 2));
        //Specify that there will this number of objectives
        theTaskSpecObject.setNumOfObjectives(3);
        //Convert specification object to a string
        String taskSpecString = theTaskSpecObject.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);
        return taskSpecString;
    }  
    
/*    public MountainCarState getState() {
        return theState;
    }*/

    /**
     * Restart the car on the mountain.  Pick a random position and velocity if
     * randomStarts is set.
     * @return
     */
    public Observation env_start() {
        theState.reset();
        return makeObservation();
    }

    /**
     * Takes a step.  If an invalid action is selected, choose a random action.
     * @param theAction
     * @return
     */
    public Reward_observation_terminal env_step(Action theAction) {

        int a = theAction.intArray[0];

        if (a > 2 || a < 0) {
            System.err.println("Invalid action selected in mountainCar: " + a);
            a = randomGenerator.nextInt(3);
        }
        theState.update(a);
        // set up new Observation
        Reward_observation_terminal RewardObs = new Reward_observation_terminal();
        RewardObs.setObservation(makeObservation());
        RewardObs.setTerminal(theState.inGoalRegion());
        // setup new rewards
        Reward rewards = new Reward(0,3,0);
        rewards.setDouble(0, theState.inGoalRegion() ? 0 : -1); // -1 penalty for time unless we escaped on this turn
        rewards.setDouble(1, a==0 ? -1 : 0); // -1 penalty for second objective if we braked this turn
        rewards.setDouble(2, a==2 ? -1 : 0); // -1 penalty for third objective if we accelerated this turn
        RewardObs.setReward(rewards);
        return RewardObs;
    }
    
    // discretises the continuous state of the MountainCar, and stores the index of the active cell in a new Observation object
    private Observation makeObservation()
    {
        Observation theObservation = new Observation(1, 0, 0);
        // get state from theState object and discretise it into an index
        int posIndex = (int)Math.floor((theState.getPosition()-theState.minPosition)*NUM_CELLS/(theState.maxPosition-theState.minPosition));
        int velocityIndex = (int)Math.floor((theState.getVelocity()-theState.minVelocity)*NUM_CELLS/(theState.maxVelocity-theState.minVelocity));
        theObservation.setInt(0, posIndex*NUM_CELLS+velocityIndex);
        return theObservation;
    }


    public String env_message(String message) 
    {
        throw new UnsupportedOperationException(message + " is not supported by MOMountainCarDiscretised environment.");
    }

    public void env_cleanup() {
        theState.reset();
    }
    
    public static void main(String[] args) {
        EnvironmentLoader L = new EnvironmentLoader(new MOMountainCarDiscretised());
        L.run();
    }

}



