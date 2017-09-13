/* 
* Copyright (C) 2007, Adam White

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
*  $Revision: 925 $
*  $Date: 2008-11-18 15:15:03 -0700 (Tue, 18 Nov 2008) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/rlglue/RL_glue.h $
* 
*/


#ifndef RL_interface_h
#define RL_interface_h

#ifdef __cplusplus
extern "C" {
#endif


#include <rlglue/RL_common.h>

/*	* RL-Glue Interface 
	*
	* This should be included by all C/C++ experiments as #include <rlglue/RL_glue.h>
	* Experiments should not implement these functions, rather, these are the functions
	* that experiments should call.
*/
const char* RL_init();
const observation_action_t *RL_start();
const reward_observation_action_terminal_t *RL_step();
void RL_cleanup();

const char* RL_agent_message(const char* message);
const char* RL_env_message(const char* message);

//EVAN
//double RL_return();
reward_t *RL_return();
//END EVAN
int RL_num_steps();
int RL_num_episodes();
int RL_episode(unsigned int num_steps);

/**
	New Experimental Methods, not part of the public API
**/
const action_t* RL_agent_start(const observation_t* observation);
//EVAN
//const action_t* RL_agent_step(double reward, const observation_t* observation);
const action_t* RL_agent_step(reward_t *reward, const observation_t* observation);
//void RL_agent_end(double reward);  
void RL_agent_end(reward_t *reward);  
//END EVAN
const observation_t* RL_env_start();
const reward_observation_terminal_t* RL_env_step(const action_t* action);


#ifdef __cplusplus
}
#endif

#endif
