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
*  $Revision: 923 $
*  $Date: 2008-11-03 23:06:09 -0700 (Mon, 03 Nov 2008) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/rlglue/Environment_common.h $
* 
*/


#ifndef Environment_common_h
#define Environment_common_h

#ifdef __cplusplus
extern "C" {
#endif

/*	* Environment Interface 
	*
	* This should be included by all C/C++ environments as #include <rlglue/Environment_common.h>
	* Environments must implement all of these functions.
*/
#include <rlglue/RL_common.h>

	/* Environment Interface */
	const char* env_init();
	const observation_t* env_start();
	const reward_observation_terminal_t* env_step(const action_t* action);
	void env_cleanup();
	const char* env_message(const char * message);

#ifdef __cplusplus
}
#endif

#endif
