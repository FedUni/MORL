/* Copyright (C) 2007, Andrew Butcher

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
*  $Revission: 914 $
*  $Date: 2009-06-05 22:38:27 -0600 (Fri, 05 Jun 2009) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/RL_network.c $
* 
*/
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif


/* Standard Headers */
#include <assert.h> /* assert */
#include <stdlib.h> /* calloc */
#include <string.h> /* memset */
#include <stdio.h> /* fprintf */

#if defined(_WIN32)
#define _WINSOCK_DEPRECATED_NO_WARNINGS
#include <winsock2.h>
#include <WS2tcpip.h>
#define SLEEPALIAS(n) Sleep(n);
#pragma comment(lib,"ws2_32")
typedef int socklen_t;
#else
/* POSIX Network Headers */
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#define SLEEPALIAS(n) sleep(n);
#define closesocket close
#endif


/* RL_netlib Library Header */
#include <rlglue/network/RL_network.h>

/* Convenience functions for manupulating RL Structs*/
#include <rlglue/utils/C/RLStruct_util.h>

/*When someone passes us a null pointer to send across the network, 
we'll send this instead*/
static rl_abstract_type_t emptyAbstractType={0, 0, 0, 0, 0, 0};

void init_sockets() {
#ifdef _WIN32
	static int initialized = 0;
	
	if (!initialized) {
		WSADATA wsaData;

		// Request version 2.2 of the Winsock API
		int err = WSAStartup(MAKEWORD(2, 2), &wsaData);
		if (err != 0) {
			printf("WSAStartup failed with error: %d\n", err);
			return;
		}
		initialized = 1;
	}
#endif
}

/* Open and configure a socket */
int rlOpen(short thePort) {
  int flag = 1;
  int theSocket = 0;

  init_sockets();

  theSocket = socket(PF_INET, SOCK_STREAM, 0);
  setsockopt(theSocket, IPPROTO_TCP, TCP_NODELAY, (char*)&flag, sizeof(int)); /* Disable Nagleing */

  return theSocket;
}

/* Calls accept on a socket */
int rlAcceptConnection(int theSocket) {
  int theClient = 0;
  struct sockaddr_in theClientAddress = {0};
  socklen_t theSocketSize = sizeof(struct sockaddr_in);
  theClient = accept(theSocket, (struct sockaddr*)&theClientAddress, &theSocketSize);
  return theClient;
}

/* Connect (TCP/IP) to the given address at the given port */
int rlConnect(int theSocket, const char* theAddress, short thePort) {
  int theStatus = 0;
  struct sockaddr_in theDestination;
  theDestination.sin_family = AF_INET;
  theDestination.sin_port = htons(thePort);
  theDestination.sin_addr.s_addr = inet_addr(theAddress);
  memset(&theDestination.sin_zero, '\0', 8);

  theStatus = connect(theSocket, 
		      (struct sockaddr*)&theDestination, 
		      sizeof(struct sockaddr));

  return theStatus;
}

/* Listen for an incoming connection on a given port.
   This function blocks until it receives a new connection */
int rlListen(int theSocket, short thePort) {
  struct sockaddr_in theServer;
  int theStatus = 0;
  int yes = 1;
  
  theServer.sin_family = AF_INET;
  theServer.sin_port = htons(thePort);
  theServer.sin_addr.s_addr = INADDR_ANY;
  memset(&theServer.sin_zero, '\0', 8);
  
  /* We don't really care if this fails, it just lets us reuse the port quickly */
  setsockopt(theSocket, SOL_SOCKET, SO_REUSEADDR, (void *)&yes, sizeof(int));

  theStatus = bind(theSocket, 
		   (struct sockaddr*)&theServer, 
		   sizeof(struct sockaddr));

  if (theStatus == -1)
	{
		fprintf(stderr,"Could not open socket\n");
		perror("bind");
		exit(1);
	}
  
  theStatus = listen(theSocket, 10);
  if (theStatus == -1) return -1;

  return theStatus;
}

int rlClose(int theSocket) {
  return closesocket(theSocket);
}

int rlIsValidSocket(int theSocket) {
  return theSocket != -1;
}

/* send doesn't guarantee that all the data will be sent.
   rlSendData calls send continually until it is all sent, or until an error occurs.
   the amount of data sent is returned */

int rlSendData(int theSocket, const void* theData, int theLength) {
  int theBytesSent = 0;
  int theMsgError = 0;
  const char* theDataBuffer = (const char*)theData;
  
  while (theBytesSent < theLength) {
    theMsgError = send(theSocket, theDataBuffer + theBytesSent, theLength - theBytesSent, 0);
    if (theMsgError == -1) break;
    else theBytesSent += theMsgError;
  }

  return theBytesSent;
}

/* Calls recv repeatedly until "theLength" data has been received, or an error occurs */
int rlRecvData(int theSocket, void* theData, int theLength) {
  int theBytesRecv = 0;
  int theMsgError = 0;
  char* theDataBuffer = (char*)theData;
  
  while (theBytesRecv < theLength) {
    theMsgError = recv(theSocket, theDataBuffer + theBytesRecv, theLength - theBytesRecv, 0);
    if (theMsgError <= 0) break;
    else theBytesRecv += theMsgError;
  }

  return theBytesRecv;
}

/* rlBuffer API */

/* We use buffers for sending and receiving network data.
   Buffers are written to in network byte order (rlBufferWrite) and read in 
   system byte order (rlBufferRead). */

void rlBufferCreate(rlBuffer *buffer, unsigned int capacity) {
  buffer->size     = 0;
  buffer->capacity = 0;
  buffer->data     = 0;

  if (capacity > 0) {
    rlBufferReserve(buffer, capacity);
  }
}

/* free up the buffer */
void rlBufferDestroy(rlBuffer *buffer) {
  if (buffer->data != 0) {
    free(buffer->data);
  }
  buffer->data = 0;
  buffer->size = 0;
  buffer->capacity = 0;
}

/* clear the buffer */
void rlBufferClear(rlBuffer *buffer) {
  buffer->size = 0;
}

/* Ensure that the buffer contains at least "capacity" memory */
void rlBufferReserve(rlBuffer *buffer, unsigned int capacity) {
  unsigned char* new_data = 0;
  unsigned int new_capacity = 0;
  /* Ensure the buffer can hold the new data */
  if (capacity > buffer->capacity) {
    /* Allocate enough memory for the additional data */
    new_capacity = capacity + (capacity - buffer->capacity) * 2;
    assert(new_capacity > 0);
    new_data = (unsigned char*)calloc(new_capacity, sizeof(unsigned char));
	if(new_data==0){
		fprintf(stderr,"ERROR -- calloc returned null pointer when we asked for %u space in rlBufferReserve.\n",new_capacity);
		fprintf(stderr,"Old buffer capacity was: %u, new was calculated = %u + (%u - %u) * 2\n",capacity,capacity,capacity,buffer->capacity);
	}
    assert(new_data != 0);

    /* Copy the existing data into the the larger memory allocation */
    if (buffer->size > 0) {
      memcpy(new_data, buffer->data, buffer->size);
    }

    /* Free the original data */
    free(buffer->data);

    /* Set the buffers data to the new data pointer */
    buffer->data = new_data;

    /* Set the new capacity */
    buffer->capacity = new_capacity;
  }
}

/* Write to the buffer in network byte order - will resize the buffer to facilitate a write that is too large for the 
   current capacity

   buffer: buffer to write to
   offset: the offset to start writing into the buffer at
   count: the number of items of size "size" to write into the buffer
   size: the size of each individual item
   return: the next sequential byte offset to write to, used as "offset" to the next call of rlBufferWrite
*/
unsigned int rlBufferWrite(rlBuffer *buffer, unsigned int offset, const void* sendData, unsigned int count, unsigned int size) {
  const unsigned char* data = (const unsigned char*)sendData;
  unsigned char* data_ptr = 0;
  unsigned int i = 0;

  if (buffer->capacity < offset + count * size ) {
    rlBufferReserve(buffer, offset + count * size);
  }

  /* Get the offset to the place in the buffer we want to start inserting */
  data_ptr = buffer->data + offset;

  /* For each of the new data items, swap the endianness and add them to the buffer */
  for (i = 0; i < count; ++i) {
    if (rlGetSystemByteOrder() == 1) {
      rlSwapEndianForDataOfSize(&data_ptr[i * size], &data[i * size], size);
    }
    else {
      memcpy(&data_ptr[i * size], &data[i * size], size);
    }
  }

  buffer->size += count * size;
  return offset + count * size;
}

/* Read data in network byte order
   buffer: buffer to read from
   offset: the offset to start reading from the buffer at
   count: the number of items of size "size" to read from the buffer
   size: the size of each individual item
   return: the next sequential byte offset to read from, used as offset in the next call to rlBufferRead
*/
unsigned int rlBufferRead(const rlBuffer *buffer, unsigned int offset, void* recvData, unsigned int count, unsigned int size) {
  unsigned char* data = (unsigned char*)recvData;
  unsigned int i = 0;

  /* For each of the new data items, swap the endianness and read them from the buffer */
  for (i = 0; i < count; ++i) {
    if (rlGetSystemByteOrder() == 1) {
      rlSwapEndianForDataOfSize(&data[i * size], &buffer->data[(i * size) + offset], size);
    }
    else {
      memcpy(&data[i * size], &buffer->data[(i * size) + offset], size);
    }
  }

  return offset + (count * size);
}

/* Send the buffer across the network. Sends target and the size of the buffer across the network
   before sending the data in the buffer itself.  This could be refactored so that it just sends the
   buffer and the caller is responsible for writing any header data into the buffer before hand.
   This was more convenient at the time */

unsigned int rlSendBufferData(int theSocket, const rlBuffer* buffer, const int target) {
  int sendTarget = target;
  unsigned int sendSize = buffer->size;
  unsigned int header[2] = {0};
  
  /* sendSize needs to go across in network byte order, swap it if we're little endian */
  if (rlGetSystemByteOrder() == 1) {
    rlSwapEndianForDataOfSize(&sendTarget, &target, sizeof(int));
    rlSwapEndianForDataOfSize(&sendSize, &buffer->size, sizeof(unsigned int));
  }
  
  header[0] = (unsigned int)sendTarget;
  header[1] = (unsigned int)sendSize;

  /*  rlSendData(theSocket, &sendTarget, sizeof(int));
      rlSendData(theSocket, &sendSize, sizeof(unsigned int));*/

  rlSendData(theSocket, header, sizeof(unsigned int) * 2);

  if (buffer->size > 0) {
    rlSendData(theSocket, buffer->data, buffer->size);
  }

  return (sizeof(unsigned int) * 2) + buffer->size; /* Returns payload size, not actual data sent ! */
}


/* Corresponds to calls made by rlSendBufferData.  Reads a "target" and a size from the network,
   then reads "size" data into rlBuffer.  See rlSendBufferData for more */

unsigned int rlRecvBufferData(int theSocket, rlBuffer* buffer, int *target) {
  unsigned int recvTarget = 0;
  unsigned int recvSize = 0;
  unsigned int header[2] = {0};
  unsigned int totalSize = 0;

  if (rlRecvData(theSocket, header, sizeof(unsigned int) * 2) > 0)
  {
    if (rlGetSystemByteOrder() == 1)  /* Little Endian */
    {
      rlSwapEndianForDataOfSize(&recvTarget, &header[0], sizeof(unsigned int));
      rlSwapEndianForDataOfSize(&recvSize, &header[1], sizeof(unsigned int));
    }
    else
    {
      recvTarget = header[0];
      recvSize = header[1];
    }

    *target = (int)recvTarget;
    rlBufferReserve(buffer, recvSize);

    if (recvSize > 0) {
      rlRecvData(theSocket, buffer->data, recvSize);
    }

    totalSize = (sizeof(unsigned int) * 2) + buffer->size;
  }

  return totalSize;
}

/* Utilities */
int rlGetSystemByteOrder() {
  /*
    Endian will be 1 when we are on a little endian machine,
    and not 1 on a big endian machine.
  */

  const int one = 1;
  const char endian = *(char*)&one;

  return endian;
}

/**
   Brian Tanner Sept 8/2008 :: This function/explanation is Andrew Butcher madness ;)

  Notice that the pointers "in" and "out" are not allowed to be the same.
   When dealing with IEEE floating point numbers, you still need to swap endianness.
   For sanity, we disallow swaping back into the same memory space to discourage 
   swapping a double/float back into its own memory.  Once these have been swapped, they should
   not be treated as doubles/floats again until they are back into their native endianness.
*/

void rlSwapEndianForDataOfSize(void* out, const void* in, const unsigned int size) {
  const unsigned char *src = (const unsigned char *)in;
  unsigned char *dst = (unsigned char *)out;
  unsigned int i = 0;

  assert(out != in);

  for (i = 0; i < size; ++i) {
    dst[i] = src[size-i-1];
  }
}


int rlWaitForConnection(const char *address, const short port, const int retryTimeout) {
  int theConnection = 0;
  int isConnected = -1;

  while(isConnected == -1) {
    theConnection = rlOpen(port);
    assert(rlIsValidSocket(theConnection));
    isConnected = rlConnect(theConnection, address, port);
    if (isConnected == -1) { 
      rlClose(theConnection);
      SLEEPALIAS(retryTimeout);
    }
  }

  return theConnection;
}

/**
* Added by Brian Tanner, Sept 8/2008
* This function write a rl_abstract_type_t from dist to a buffer *dist.
* It uses offset to know where to start writing, and returns a new offset after 
* the read.
**/
unsigned int rlCopyADTToBuffer(const rl_abstract_type_t* src, rlBuffer* dst, unsigned int offset) {
	/* The header is made up of the counts: numInts, numDoubles, and numChars */
	const int headerSize = sizeof(unsigned int) * 3;
	/* The body is all of the ints, doubles, and chars */
	int dataSize   = 0;
	
	if(src==0){
		src=&emptyAbstractType;
	}
	
	dataSize=src->numInts * sizeof(int) + src->numDoubles * sizeof(double) + src->numChars * sizeof(char);
	
	__RL_CHECK_STRUCT(src);

	rlBufferReserve(dst, dst->size + headerSize + dataSize);

	offset = rlBufferWrite(dst, offset, &src->numInts, 1, sizeof(unsigned int));
	offset = rlBufferWrite(dst, offset, &src->numDoubles, 1, sizeof(unsigned int));
	offset = rlBufferWrite(dst, offset, &src->numChars, 1, sizeof(unsigned int));

	if (src->numInts > 0) {
		offset = rlBufferWrite(dst, offset, src->intArray, src->numInts, sizeof(int));
	}

	if (src->numDoubles > 0) {
		offset = rlBufferWrite(dst, offset, src->doubleArray, src->numDoubles, sizeof(double));  
	}

	if (src->numChars > 0) {
		offset = rlBufferWrite(dst, offset, src->charArray, src->numChars, sizeof(char));  
	}


	return offset;
}

/**
* Added by Brian Tanner, Sept 8/2008
* This function reads a rl_abstract_type_t from the buffer and puts it into *dst.
* It uses offset to know where to start reading, and returns a new offset after 
* the read.
**/
unsigned int rlCopyBufferToADT(const rlBuffer* src, unsigned int offset, rl_abstract_type_t* dst) {
	unsigned int numIntsInBuffer    = 0;
	unsigned int numDoublesInBuffer = 0;
	unsigned int numCharsInBuffer   = 0;
	
	assert(dst!=0);

	offset = rlBufferRead(src, offset, &numIntsInBuffer, 1, sizeof(unsigned int));
	offset = rlBufferRead(src, offset, &numDoublesInBuffer, 1, sizeof(unsigned int));
	offset = rlBufferRead(src, offset, &numCharsInBuffer, 1, sizeof(unsigned int));


	if(numIntsInBuffer>1000000 || numDoublesInBuffer>1000000 || numCharsInBuffer > 1000000){
		fprintf(stderr,"ERROR -- more than a million of ints, doubles, or chars in the buffer (%d %d %d), probably corrupt datastream, exiting\n",numIntsInBuffer,numDoublesInBuffer,numCharsInBuffer);
		exit(1);
	}

/* 	Brian Tanner: October 8, 2008 

	This code used to be clever and would only clear and re-allocate memory when it was absolutely necessary.
	However, that code was a little bit buggy, and to make RL-Glue 3.0 really solid we've replaced it with some 
	very simple code.
	
	This memory de-allocation/allocation should not be a disastrous bottleneck, I think the sockets are introducing
	enough latency that optimizing this might have a negligible impact.  However, we should one day come back here
	and optimize this code to avoid it some time in the future. 
*/


	/*Just clear out DST for now, one day we will make an optimization pass and make this more efficient */
	reallocateRLStruct(dst,numIntsInBuffer,numDoublesInBuffer,numCharsInBuffer);	

	if(numIntsInBuffer>0){
		offset = rlBufferRead(src, offset, dst->intArray, dst->numInts, sizeof(int));
	}

	if(numDoublesInBuffer>0){
		offset = rlBufferRead(src, offset, dst->doubleArray, dst->numDoubles, sizeof(double));
	}

	if(numCharsInBuffer>0){
		offset = rlBufferRead(src, offset, dst->charArray, dst->numChars, sizeof(char));
	}

	return offset;
}
