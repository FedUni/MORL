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
*  $Revision: 832 $
*  $Date: 2008-09-15 10:20:19 -0600 (Mon, 15 Sep 2008) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/RL_network_glue.c $
* 
*/
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif


#include <stdio.h>
#include <stdlib.h>
#include <rlglue/network/RL_network.h>

#include "RL_network_glue.h"

extern int rlDidAgentConnect();
extern void rlSetAgentConnection(int);
extern void rlCloseAgentConnection();

extern int rlDidEnvironmentConnect();
extern void rlSetEnvironmentConnection(int);
extern void rlCloseEnvironmentConnection();

extern int rlDidExperimentConnect();
extern void rlSetExperimentConnection(int);
extern void rlCloseExperimentConnection();

int rlOpenServer()
{
  int server = 0;
  char* envptr = 0;
  short port = kDefaultPort;


  envptr = getenv("RLGLUE_PORT");  
  if (envptr != 0) {
    port = strtol(envptr, 0, 10);
    if (port == 0) {
      port = kDefaultPort;
    }
  }
  //fprintf(stdout, "RL-Glue is listening for connections on port=%d\n", port);
  fprintf(stdout, "MORL-Glue is listening for connections on port=%d\n", port); //DEAN CH
  
  server = rlOpen(port);
  rlListen(server, port);

  return server;
}

int rlConnectSystems() {

  int theServer = 0;
  int theClient = 0;
  int theClientType = 0;
  int theExperimentConnection = 0;

  rlBuffer theBuffer = {0};

  /* if there are things that are not yet connected we need to open a socket and listen for them */

  if (!rlDidAgentConnect() || !rlDidEnvironmentConnect() || !rlDidExperimentConnect())
  {
    rlBufferCreate(&theBuffer, sizeof(int) * 2);

    theServer = rlOpenServer();

    while(!rlDidAgentConnect() || !rlDidEnvironmentConnect() || !rlDidExperimentConnect()) {
      theClient = rlAcceptConnection(theServer);

      rlRecvBufferData(theClient, &theBuffer, &theClientType);
      
      switch(theClientType) {
      case kAgentConnection:
	//fprintf(stdout, "\tRL-Glue :: Agent connected.\n"); 
	fprintf(stdout, "\tMORL-Glue :: Agent connected.\n"); //DEAN CH
	rlSetAgentConnection(theClient);
	break;
	
      case kEnvironmentConnection:
	//fprintf(stdout, "\tRL-Glue :: Environment connected.\n");
	fprintf(stdout, "\tMORL-Glue :: Environment connected.\n"); //DEAN CH
	rlSetEnvironmentConnection(theClient);
	break;
	
      case kExperimentConnection:
	//fprintf(stdout, "\tRL-Glue :: Experiment connected.\n");
	fprintf(stdout, "\tMORL-Glue :: Experiment connected.\n"); //DEAN CH
	rlSetExperimentConnection(theClient);
	theExperimentConnection = theClient;
	break;
	
      default:
	fprintf(stderr, "RL_network.c: Unknown Connection Type: %d\n", theClientType);
	break;
      };
    }
    rlClose(theServer);
    rlBufferDestroy(&theBuffer);
  }

  return theExperimentConnection;
}

void rlDisconnectSystems()
{
  rlCloseAgentConnection();
  rlCloseEnvironmentConnection();
  rlCloseExperimentConnection();
}
