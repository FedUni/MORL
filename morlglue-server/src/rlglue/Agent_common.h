/* 
* Copyright (C) 2007, Matt Radkie

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
*  $Revision: 914 $
*  $Date: 2008-10-11 12:09:33 -0600 (Sat, 11 Oct 2008) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/rlglue/Agent_common.h $
* 
*/


#ifndef Agent_common_h
#define Agent_common_h

#include <rlglue/RL_common.h>

#ifdef __cplusplus
extern "C" {
#endif

/*	* Agent Interface 
	*
	* This should be included by all C/C++ agents as #include <rlglue/Agent_common.h>
	* Agents must implement all of these functions.
*/
void agent_init(const char* task_spec);
const action_t* agent_start(const observation_t* observation);
//EVAN
//const action_t* agent_step(double reward, const observation_t* observation);
const action_t* agent_step(const reward_t *reward, const observation_t* observation);
//void agent_end(double reward);  
void agent_end(const reward_t *reward);  
//END EVAN
void agent_cleanup();
const char* agent_message(const char* message);

#ifdef __cplusplus
}
#endif
#endif
