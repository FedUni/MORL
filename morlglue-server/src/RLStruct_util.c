/* 
* Copyright (C) 2008, Brian Tanner

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
*  $Revision: 211 $
*  $Date: 2008-09-28 03:21:30 -0600 (Sun, 28 Sep 2008) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: https://rl-glue-ext.googlecode.com/svn/trunk/projects/codecs/C/src/RLStruct_util.c $
* 
*/
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <assert.h>
#include <stdlib.h> /* free, calloc */
#include <string.h> /* memcpy */
#include <rlglue/utils/C/RLStruct_util.h>

/**	
*	Sept 8 2008, Brian Tanner is creating replace function
*	This one takes a src and dst, and puts all the data from the src into the dst
*	Freeing and reallocating if necessary
**/
void replaceRLStruct(const rl_abstract_type_t *src, rl_abstract_type_t *dst){
	assert(src!=0);
	assert(dst!=0);
	
	if(dst->numInts!=src->numInts){
		if(dst->numInts>0 && dst->intArray!=0){
			free(dst->intArray);
			dst->intArray=0;
		}
		dst->numInts=src->numInts;
		if(dst->numInts>0){
			dst->intArray=(int *)calloc(dst->numInts,sizeof(int));
		}
	}
	if(src->numInts>0){
		memcpy(dst->intArray, src->intArray,dst->numInts*sizeof(int));
	}

	if(dst->numDoubles!=src->numDoubles){
		if(dst->numDoubles>0 && dst->doubleArray!=0){
			free(dst->doubleArray);
			dst->doubleArray=0;
		}
		dst->numDoubles=src->numDoubles;
		if(dst->numDoubles>0){
			dst->doubleArray=(double *)calloc(dst->numDoubles,sizeof(double));
		}
	}
	if(src->numDoubles>0){
		memcpy(dst->doubleArray, src->doubleArray,dst->numDoubles*sizeof(double));
	}

	if(dst->numChars!=src->numChars){
		if(dst->numChars>0 && dst->charArray!=0){
			free(dst->charArray);
			dst->charArray=0;
		}
		dst->numChars=src->numChars;
		if(dst->numChars>0){
			dst->charArray=(char *)calloc(dst->numChars,sizeof(char));
		}
	}
	if(src->numChars>0){
		memcpy(dst->charArray, src->charArray,dst->numChars*sizeof(char));
	}	
}

/**
Created by Brian Tanner on Sept 27, 2008.
I thought this might be handy for people
*/
void clearRLStruct(rl_abstract_type_t *dst){
	if(dst==0)return;
	if(dst->intArray!=0){
		free(dst->intArray);
	}
	dst->intArray=0;

	if(dst->doubleArray!=0){
		free(dst->doubleArray);
	}
	dst->doubleArray=0;
	if(dst->charArray!=0){
		free(dst->charArray);
	}
	dst->charArray=0;

	dst->numInts=0;
	dst->numDoubles=0;
	dst->numChars=0;
	
}

void freeRLStructPointer(rl_abstract_type_t *dst){
	if(dst!=0){
		clearRLStruct(dst);
		free(dst);
	}
}

void reallocateRLStruct(rl_abstract_type_t *dst, const unsigned int numInts, const unsigned int numDoubles, const unsigned int numChars){
	assert(dst!=0);
	/* We could be clever here if we had wanted, and re-use the arrays if they were the same size
	   as the new arrays.  We can still implement that optimization sometime in the future. Instead, for now
	   we just free the old arrays and create new ones.
	*/
	clearRLStruct(dst);
	allocateRLStruct(dst,numInts,numDoubles,numChars);
}
void allocateRLStruct(rl_abstract_type_t *dst, const unsigned int numInts, const unsigned int numDoubles, const unsigned int numChars){
	assert(dst!=0);
	dst->numInts=numInts;
	dst->numDoubles=numDoubles;
	dst->numChars=numChars;
	
	dst->intArray=0;
	dst->doubleArray=0;
	dst->charArray=0;
	
	if(dst->numInts!=0)
		dst->intArray=(int *)calloc(dst->numInts,sizeof(int));

	if(dst->numDoubles!=0)
		dst->doubleArray=(double *)calloc(dst->numDoubles,sizeof(double));

	if(dst->numChars!=0)
		dst->charArray=(char *)calloc(dst->numChars,sizeof(char));
}

rl_abstract_type_t *allocateRLStructPointer(const unsigned int numInts, const unsigned int numDoubles, const unsigned int numChars){
	rl_abstract_type_t *dst=(rl_abstract_type_t *)calloc(1,sizeof(rl_abstract_type_t));
	allocateRLStruct(dst,numInts,numDoubles,numChars);
	return dst;
}

rl_abstract_type_t *duplicateRLStructToPointer(const rl_abstract_type_t *src){
	assert(src!=0);
	rl_abstract_type_t *dst=(rl_abstract_type_t *)calloc(1,sizeof(rl_abstract_type_t));
	replaceRLStruct(src,dst);
	return dst;
}


int __rlglue_check_abstract_type(const rl_abstract_type_t *theStruct){
	if(theStruct==0)return 0;
	
	if(theStruct->numInts>1000000){printf("abstract type integrity error: numInts = %d\n",theStruct->numInts);return 1;}
	if(theStruct->numDoubles>1000000)return 2;
	if(theStruct->numChars>1000000)return 3;

	if(theStruct->numInts>0 && theStruct->intArray==0)return 4;
	if(theStruct->numDoubles>0 && theStruct->doubleArray==0)return 5;
	if(theStruct->numChars>0 && theStruct->charArray==0)return 6;

	if(theStruct->numInts==0 && theStruct->intArray!=0)return 7;
	if(theStruct->numDoubles==0 && theStruct->doubleArray!=0)return 8;
	if(theStruct->numChars==0 && theStruct->charArray!=0)return 9;
	
	return 0;
}

/* Take ths out later */
void __rlglue_print_abstract_type(const rl_abstract_type_t *theStruct){
	int i;
	if(theStruct==0)return;
	printf("Printing Abstract Type\n-----------------\n");
	printf("\t Ints: %d \t Doubles: %d\t Chars: %d\n",theStruct->numInts, theStruct->numDoubles, theStruct->numChars);
	if(theStruct->numInts>0)assert(theStruct->intArray!=0);
	if(theStruct->numDoubles>0)assert(theStruct->doubleArray!=0);
	if(theStruct->numChars>0)assert(theStruct->charArray!=0);
	
	if(theStruct->numInts<100){
		printf("Ints: ");
		for(i=0;i<theStruct->numInts;i++)
			printf("\t%d",theStruct->intArray[i]);
	}
	printf("\n");
	if(theStruct->numDoubles<100){
		printf("Doubles: ");
		for(i=0;i<theStruct->numDoubles;i++)
			printf("\t%f",theStruct->doubleArray[i]);
	}
	printf("\n");
	if(theStruct->numChars<100){
		printf("Chars: ");
		for(i=0;i<theStruct->numChars;i++)
			printf("\t%c",theStruct->charArray[i]);
	}
	printf("\n");
}
/*This is an easier trick to get the version */
char svnVersionString[1024];
char* __rlglue_get_svn_version(){
	int howMuchToCopy=0;
	char *theVersion="$Revision: 909 $";
	howMuchToCopy=strlen(theVersion+11) - 2;
	assert(howMuchToCopy>0);
	memcpy(svnVersionString,  theVersion+11, howMuchToCopy);
    svnVersionString[howMuchToCopy] = '\0';
	return svnVersionString;
}
