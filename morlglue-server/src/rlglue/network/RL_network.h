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
*  $Revision: 925 $
*  $Date: 2008-11-18 15:15:03 -0700 (Tue, 18 Nov 2008) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/rlglue/network/RL_network.h $
* 
*/


#ifndef RL_network_h
#define RL_network_h
#ifdef __cplusplus
extern "C" {
#endif

/* Defines types for RL-Glue */
#include <rlglue/RL_common.h>

/* RL-Glue needs to know what type of object is trying to connect. */
#define kExperimentConnection  1
#define kAgentConnection       2
#define kEnvironmentConnection 3

#define kAgentInit    		4 /* agent_* start by sending one of these values */
#define kAgentStart   		5 /* to the client to let it know what type of    */
#define kAgentStep    		6 /* event to respond to                          */
#define kAgentEnd     		7
#define kAgentCleanup 		8
#define kAgentMessage 		10

#define kEnvInit          	11
#define kEnvStart         	12
#define kEnvStep          	13
#define kEnvCleanup       	14
#define kEnvMessage       	19

#define kRLInit           	20
#define kRLStart          	21
#define kRLStep           	22
#define kRLCleanup        	23
#define kRLReturn         	24
#define kRLNumSteps       	25
#define kRLNumEpisodes    	26
#define kRLEpisode        	27
#define kRLAgentMessage   	33
#define kRLEnvMessage     	34

#define kRLEnvStart     	36
#define kRLEnvStep     		37
#define kRLAgentStart     	38
#define kRLAgentStep     	39
#define kRLAgentEnd     	40

#define kRLTerm           	35

#define kLocalHost "127.0.0.1"
#define kDefaultPort 4096
#define kRetryTimeout 2

/* Data types */
typedef struct rlBuffer_t {
  unsigned int size;
  unsigned int capacity;
  unsigned char *data;
} rlBuffer;

/* Basic network functionality */
int rlOpen(short thePort);
int rlAcceptConnection(int theSocket);

int rlConnect(int theSocket, const char* theAddress, short thePort);
int rlListen(int theSocket, short thePort);
int rlClose(int theSocket);
int rlIsValidSocket(int theSocket);

int rlSendData(int socket, const void* data, int length);
int rlRecvData(int socket, void* data, int length);

/* rlBuffer API */
void rlBufferCreate(rlBuffer *buffer, unsigned int capacity);
void rlBufferDestroy(rlBuffer *buffer);
void rlBufferClear(rlBuffer *buffer);
void rlBufferReserve(rlBuffer *buffer, unsigned int capacity);
unsigned int rlBufferWrite(rlBuffer *buffer, unsigned int offset, const void* sendData, unsigned int count, unsigned int size);
unsigned int rlBufferRead(const rlBuffer *buffer, unsigned int offset, void* recvData, unsigned int count, unsigned int size);

/* Utilities */
unsigned int rlSendBufferData(int theSocket, const rlBuffer* buffer, const int target);
unsigned int rlRecvBufferData(int theSocket, rlBuffer* buffer, int* target);

int rlGetSystemByteOrder();
void rlSwapEndianForDataOfSize(void* out, const void* in, const unsigned int size);
int rlWaitForConnection(const char *address, const short port, const int retryTimeout);
unsigned int rlCopyADTToBuffer(const rl_abstract_type_t* src, rlBuffer* dst, unsigned int offset);
unsigned int rlCopyBufferToADT(const rlBuffer* src, unsigned int offset, rl_abstract_type_t* dst);
#ifdef __cplusplus
}
#endif

#endif
