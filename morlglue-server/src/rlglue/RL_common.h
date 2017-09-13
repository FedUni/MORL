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
*  $Revision: 957 $
*  $Date: 2009-02-03 09:07:49 -0700 (Tue, 03 Feb 2009) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/rlglue/RL_common.h $
* 
*/


#ifndef RLcommon_h
#define RLcommon_h

#ifdef __cplusplus
extern "C" {
#endif

#include <stdio.h>
#include <stdlib.h>

/* Strings are not guaranteed to be null terminated I think */
typedef struct
{
	unsigned int numInts;
	unsigned int numDoubles;
	unsigned int numChars;
	int* intArray;
	double* doubleArray;
	char* charArray;
} rl_abstract_type_t;

typedef rl_abstract_type_t observation_t;
typedef rl_abstract_type_t action_t;
//EVAN
typedef rl_abstract_type_t reward_t;
//END EVAN

typedef struct{
  const observation_t *observation;
  const action_t *action;
} observation_action_t;

typedef struct
{ 
//EVAN
  //double reward;
  const reward_t *reward;
//END EVAN  
  const observation_t *observation;
  int terminal;
} reward_observation_terminal_t;

typedef struct {
//EVAN
  //double reward;
  const reward_t *reward;
//END EVAN  
  const observation_t *observation;
  const action_t *action;
  int terminal;
} reward_observation_action_terminal_t;

typedef reward_observation_action_terminal_t roat_t;
typedef reward_observation_terminal_t rot_t;
typedef observation_action_t oa_t;


#ifdef __cplusplus
}
#endif

#endif
