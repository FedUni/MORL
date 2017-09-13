/* 
 * Copyright (C) 2007, Andrew Butcher

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 *
 *  $Revision: 1006 $
 *  $Date: 2009-06-05 22:38:27 -0600 (Fri, 05 Jun 2009) $
 *  $Author: brian@tannerpages.com $
 *  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/RL_glue.c $
 *
 */
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <rlglue/RL_glue.h>
#include <rlglue/Agent_common.h>
#include <rlglue/Environment_common.h>

#include <rlglue/utils/C/RLStruct_util.h>

#include <assert.h>


/*Brian, Sept 8, 2008 :
---------------------
I'm a little worried that some of my changes in this file 
(using stack variables for structs and returning them)
 might go terribly wrong*/

static const action_t *last_action = 0;
//EVAN
//static double total_reward = 0;
//extern int num_rewards; //DEAN CH

//DEAN
static int total_reward_flag=0;
static reward_t *total_reward = 0;
//END DEAN

//END EVAN
static int num_steps = 0;
static int num_episodes = 0;

const char* RL_init() {
    //EVAN
    //fprintf(stdout, "*** EVAN ***\nRL-Glue has %d reward(s)\n", num_rewards); //DEAN CH
    //END EVAN

    const char* task_spec;
    task_spec = env_init();
    agent_init(task_spec);

    //EVAN
    //total_reward=0;
    //total_reward = allocateRLStructPointer(0, num_rewards, 0); //DEAN RM
    //total_reward = allocateRLStructPointer(0, 2, 0); //DEAN CH
    //DEAN
    //Pass through init once - initialise ptr and flag
    total_reward = NULL;
    total_reward_flag=0;
    //END DEAN
    //END EVAN
    num_steps = 0;
    num_episodes = 0;
    /* **WORRYSOME** */
    return task_spec;
}

const observation_action_t *RL_start() {
    const observation_t *last_state = 0;
    /*Make this static so that it is safe to return it*/
    static observation_action_t oa;

    //EVAN
    //total_reward=0;
    //total_reward = allocateRLStructPointer(0, num_rewards, 0);
    //total_reward = allocateRLStructPointer(0, 2, 0); //DEAN CH
    //END EVAN
    
    //DEAN
    //For episodial runs -- come from RL_Start() set flag for resetting total_reward
    if (total_reward_flag==2){ 
      total_reward_flag=1;
    }
    //Ensure struct ptr is null and flag 0 for first pass into RL_Step
    else if (total_reward_flag==0){
      total_reward = NULL;
    }
    else{
      fprintf(stdout,"Unable to reset Total Rewards");
      exit(1);
    }
    //END DEAN

    num_steps = 1;

    last_state = env_start();
    last_action = agent_start(last_state);

    oa.observation = last_state;
    oa.action = last_action;

    /* **WORRYSOME** */
    return &oa;
}

const action_t* RL_agent_start(const observation_t* observation) {
    return agent_start(observation);
}
//EVAN
//const action_t* RL_agent_step(double reward, const observation_t* observation){

const action_t* RL_agent_step(reward_t *reward, const observation_t* observation) {
    //END EVAN
    return agent_step(reward, observation);
}
//EVAN
//void RL_agent_end(double reward){

void RL_agent_end(reward_t *reward) {
    //END EVAN
    agent_end(reward);
}

const observation_t* RL_env_start() {
    const observation_t *thisObservation = 0;

    //EVAN
    //total_reward=0;
    //total_reward = allocateRLStructPointer(0, 2, 0); //DEAN RM
    //DEAN
    fprintf(stdout,"In RL_env_start()\n");
    //END DEAN
    //END EVAN
    num_steps = 1;

    thisObservation = env_start();
    return thisObservation;
}

const reward_observation_terminal_t* RL_env_step(const action_t* action) {
    const reward_observation_terminal_t *ro = 0;
    //EVAN
    //double this_reward=0;
/*
    reward_t *this_reward = allocateRLStructPointer(0, num_rewards, 0);
*/
    //END EVAN

    __RL_CHECK_STRUCT(action)
    ro = env_step(action);
    __RL_CHECK_STRUCT(ro->observation)
/*
    this_reward = ro->reward;
*/

    //EVAN
    //DEAN
    fprintf(stdout,"In RL_env_step()\n");
    //END DEAN
    //total_reward += this_reward;
    for (unsigned int reward_index = 0; reward_index < ro->reward->numDoubles; reward_index++) {
        total_reward->doubleArray[reward_index] += ro->reward->doubleArray[reward_index];
    }
    //END EVAN

    if (ro->terminal == 1) {
        num_episodes += 1;
    } else {
        num_steps += 1;
    }
    return ro;
}

const reward_observation_action_terminal_t *RL_step() {
    static reward_observation_action_terminal_t roa = {0, 0, 0, 0};
    const reward_observation_terminal_t *ro;
    const observation_t *last_state;

    __RL_CHECK_STRUCT(last_action)
    ro = env_step(last_action);
    __RL_CHECK_STRUCT(ro->observation)
    last_state = ro->observation;

    roa.reward = ro->reward;
    roa.observation = ro->observation;
    roa.terminal = ro->terminal;
     
    //Allocate memory for the total reward struct -- from RL_init() only
    if(total_reward_flag==0) {
      //fprintf(stdout,"Flag =0 : Total_reward:%p\n",total_reward);
      //fprintf(fp,"Flag =0 : Total_reward:%p\n",total_reward);
      total_reward = allocateRLStructPointer(0,ro->reward->numDoubles, 0);
      total_reward_flag=1;
    }

    //Reset total reward only after mem allocation or from RL_Start()
    if(total_reward_flag==1) {
      for (unsigned int reward_index = 0; reward_index < ro->reward->numDoubles; reward_index++) {
        //fprintf(stdout,"Flag=1\n"); 
        //fprintf(fp,"Flag=1\n"); 
        total_reward->doubleArray[reward_index] = 0;
      }
      //Set flag to prevent initialisation when start called from RL_episode()
      total_reward_flag=2;
    }

    //EVAN
    //Accumlate total reward from rewards in each episode
    //total_reward += this_reward;
    for (unsigned int reward_index = 0; reward_index < ro->reward->numDoubles; reward_index++) {
        total_reward->doubleArray[reward_index] += ro->reward->doubleArray[reward_index];
    }
    //END EVAN

    /* Sept 28/08, The reason that we only increment stepcount if we're not terminal is that if an episode ends on
    its first env_step, num_step will be 1 from env_start, but we don't want to go to num_step=2.*/
    if (ro->terminal == 1) {
        num_episodes += 1;
        agent_end(ro->reward);
    } else {
        num_steps += 1;
        last_action = agent_step(ro->reward, last_state);
        __RL_CHECK_STRUCT(last_action)
        roa.action = last_action;
    }
    /* **WORRYSOME** */
    return &roa;
}

void RL_cleanup() {
  //DEAN TODO Need to remove the struct pointer allocation to totalRewards
  //Testing it!!!
    freeRLStructPointer(total_reward);
    env_cleanup();
    agent_cleanup();
}

const char* RL_agent_message(const char* message) {
    const char *theAgentResponse = 0;
    const char *messageToSend = 0;

    if (message != 0) {
        messageToSend = message;
    } else {
        messageToSend = "";
    }

    theAgentResponse = agent_message(messageToSend);
    if (theAgentResponse == 0) {
        return "";
    }

    return theAgentResponse;
}

const char* RL_env_message(const char* message) {
    const char *theEnvResponse = 0;
    const char *messageToSend = 0;

    if (message != 0) {
        messageToSend = message;
    } else {
        messageToSend = "";
    }

    theEnvResponse = env_message(messageToSend);
    if (theEnvResponse == 0) {
        return "";
    }

    return theEnvResponse;
}

int RL_episode(const unsigned int maxStepsThisEpisode) {
    const reward_observation_action_terminal_t *rl_step_result = 0;
    int isTerminal = 0;


    RL_start();
    /*RL_start sets steps to 1*/
    for (; !isTerminal && (maxStepsThisEpisode == 0 ? 1 : num_steps < maxStepsThisEpisode);) {
        rl_step_result = RL_step();
        isTerminal = rl_step_result->terminal;
    }

    /*Return the value of terminal to tell the caller whether the episode ended naturally or was cut off*/
    return isTerminal;
}

//EVAN
//double RL_return() {

reward_t *RL_return() {
    //END EVAN
    return total_reward;
}

int RL_num_steps() {
    /* number of steps of the current or just completed episodes of run */
    return num_steps;
}

int RL_num_episodes() {
    return num_episodes;
}

