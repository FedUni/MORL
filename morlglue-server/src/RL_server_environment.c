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
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/RL_server_environment.c $
* 
*/
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif


#include <stdlib.h> /* calloc */
#include <string.h> /* strlen */
#include <assert.h> /* assert */

#include <stdio.h> /* fprintf: debug only */

#include <rlglue/Environment_common.h>
#include <rlglue/network/RL_network.h>

/* Convenience functions for manupulating RL Structs*/
#include <rlglue/utils/C/RLStruct_util.h>

static char* theTaskSpec   = 0;
static rlBuffer theBuffer               = {0};
static observation_t *theObservation = 0;
//EVAN
static reward_t *theReward = 0;
//END EVAN
static char* theOutMessage = 0;

extern void rlSetEnvironmentConnection();
extern int rlGetEnvironmentConnection();

const char* env_init() {
	/* Setup the connection */
	int envState = kEnvInit;
	unsigned int theTaskSpecLength = 0;
	unsigned int offset = 0;

	if (theBuffer.capacity == 0){
		rlBufferCreate(&theBuffer, 65536);
	}

	/* env init-specific data */
	rlBufferClear(&theBuffer);
	rlSendBufferData(rlGetEnvironmentConnection(), &theBuffer, envState);

	rlBufferClear(&theBuffer);
	rlRecvBufferData(rlGetEnvironmentConnection(), &theBuffer, &envState);
	assert(envState == kEnvInit);

	offset = 0;
	offset = rlBufferRead(&theBuffer, offset, &theTaskSpecLength, 1, sizeof(int));  
	if (theTaskSpecLength > 0) {
		if (theTaskSpec != 0) {
			free(theTaskSpec);
			theTaskSpec = 0;
		}

		/*Read the task spec off the wire and then add \0 at the end, to be sure? */
		/*Are we actually stripping the \0 before we send it or is this just for good measure */
		theTaskSpec = (char*)calloc(theTaskSpecLength+1, sizeof(char));
		offset = rlBufferRead(&theBuffer, offset, theTaskSpec, theTaskSpecLength, sizeof(char));
		theTaskSpec[theTaskSpecLength] = '\0';
	}

	return theTaskSpec;
	}

const observation_t *env_start() {
	int envState = kEnvStart;
	unsigned int offset = 0;

	rlBufferClear(&theBuffer);
	rlSendBufferData(rlGetEnvironmentConnection(), &theBuffer, envState);

	rlBufferClear(&theBuffer);
	rlRecvBufferData(rlGetEnvironmentConnection(), &theBuffer, &envState);
	assert(envState == kEnvStart);

	if(theObservation==0)theObservation=allocateRLStructPointer(0,0,0);
	__RL_CHECK_STRUCT(theObservation)
	
	offset = rlCopyBufferToADT(&theBuffer, offset, theObservation);
	return theObservation;
}

const reward_observation_terminal_t *env_step(const action_t *theAction) {
  int envState = kEnvStep;
  static reward_observation_terminal_t ro = {0,0,0};
  unsigned int offset = 0;

  __RL_CHECK_STRUCT(theAction)
  rlBufferClear(&theBuffer);
  offset = 0;
  /* Send theAction to the client environment */
  offset = rlCopyADTToBuffer(theAction, &theBuffer, offset);
  rlSendBufferData(rlGetEnvironmentConnection(), &theBuffer, envState);

  rlBufferClear(&theBuffer);
  rlRecvBufferData(rlGetEnvironmentConnection(), &theBuffer, &envState);
  assert(envState == kEnvStep);

  /* Receive theObservation from the client environment */
  offset = 0;
  offset = rlBufferRead(&theBuffer, offset, &ro.terminal, 1, sizeof(int));
//EVAN  
  //offset = rlBufferRead(&theBuffer, offset, &ro.reward, 1, sizeof(double));
	if(theReward==0)theReward=allocateRLStructPointer(0,0,0);
  offset = rlCopyBufferToADT(&theBuffer, offset, theReward);
  __RL_CHECK_STRUCT(theReward)

  ro.reward = theReward;
//END EVAN

	if(theObservation==0)theObservation=allocateRLStructPointer(0,0,0);
  offset = rlCopyBufferToADT(&theBuffer, offset, theObservation);
  __RL_CHECK_STRUCT(theObservation)

  ro.observation = theObservation;
  return &ro;
}

void env_cleanup() {
	int envState = kEnvCleanup;

	rlBufferClear(&theBuffer);
	rlSendBufferData(rlGetEnvironmentConnection(), &theBuffer, envState);

	rlBufferClear(&theBuffer);
	rlRecvBufferData(rlGetEnvironmentConnection(), &theBuffer, &envState);
	assert(envState == kEnvCleanup);

	rlBufferDestroy(&theBuffer);

	freeRLStructPointer(theObservation);
	theObservation=0;
	
	if (theTaskSpec != 0) {
		free(theTaskSpec);
		theTaskSpec = 0;
	}

	if (theOutMessage != 0) {
		free(theOutMessage);
		theOutMessage = 0;
	}
}


const char* env_message(const char* inMessage) {
  int envState = kEnvMessage;
  unsigned int theInMessageLength = 0;
  unsigned int theOutMessageLength = 0;
  unsigned int offset = 0;

  if (inMessage != NULL) {
    theInMessageLength = strlen(inMessage);
  }

  if (theBuffer.capacity == 0)
    rlBufferCreate(&theBuffer, 65356);

  rlBufferClear(&theBuffer);
  offset = 0;
  offset = rlBufferWrite(&theBuffer, offset, &theInMessageLength, 1, sizeof(int));
  if (theInMessageLength > 0) {
    offset = rlBufferWrite(&theBuffer, offset, inMessage, theInMessageLength, sizeof(char));
  }
  rlSendBufferData(rlGetEnvironmentConnection(), &theBuffer, envState);

  rlBufferClear(&theBuffer);
  rlRecvBufferData(rlGetEnvironmentConnection(), &theBuffer, &envState);
  assert(envState == kEnvMessage);

  offset = 0;
  offset = rlBufferRead(&theBuffer, offset, &theOutMessageLength, 1, sizeof(int));
/*Free and point the old message to null */
    if (theOutMessage != 0) {
      free(theOutMessage);
      theOutMessage = 0;
    }
/* Allocated memory for the new message, maybe just 1 byte for the terminator */
    theOutMessage = (char*)calloc(theOutMessageLength+1, sizeof(char));

/* Fill up the string from the buffer */
if (theOutMessageLength > 0) {
    offset = rlBufferRead(&theBuffer, offset, theOutMessage, theOutMessageLength, sizeof(char));
  }
/* Set the terminator */
    theOutMessage[theOutMessageLength] = '\0';
  return theOutMessage;
}
