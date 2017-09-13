/* 
 Copyright (C) 2009, Scott Livingston

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

*/

#ifndef TASKSPEC_PARSER_H
#define TASKSPEC_PARSER_H

#ifdef __cplusplus
extern "C" {
#endif


/* current task spec version string */
#define CURRENT_VERSION "RL-Glue-3.0"

/* problem types */
#define TSPEC_EPISODIC   1
#define TSPEC_CONTINUING 2
#define TSPEC_OTHER      0

/* special range values */
#define RVAL_NOTSPECIAL 0
#define RVAL_NEGINF     1 /* negative infinity */
#define RVAL_POSINF     2 /* positive infinity */
#define RVAL_UNSPEC     3 /* unspecified */


typedef struct {
	unsigned int repeat_count; /* number of times this range tuple repeats */
	int min, max;
	char special_min, special_max;
} int_range_t;

typedef struct {
	unsigned int repeat_count;
	double min, max;
	char special_min, special_max;
} double_range_t;


typedef struct {
	char *version;              /* task spec version string */
	char problem_type;
	double discount_factor;
	int num_int_observations;            /* length of int_observations array */
	int_range_t *int_observations;       /* array of integral observation dimensions */
	int num_double_observations;         /* length of double_observations array */
	double_range_t *double_observations; /* array of real observation dimensions */
	int charcount_observations;          /* number of characters in observation */
	int num_int_actions;            /* length of int_actions array */
	int_range_t *int_actions;       /* array of integral action dimensions */
	int num_double_actions;         /* length of double_actions array */
	double_range_t *double_actions; /* array of real action dimensions */
	int charcount_actions;          /* number of characters in action */
	double_range_t reward;      /* range of (environmentally determined) reward */
	char *extra_spec;           /* string of extra specifications (not parsed) */
} taskspec_t;


/* decode_taskspec
 *
 * Decode (i.e., parse) a given task specification string.
 *
 * Returns 0 on success,
 *         1 if task spec version is unsupported or not recognized, or
 *        -1 on failure.
 */
int decode_taskspec( taskspec_t *tspec, const char *tspec_string );

/* encode_taskspec
 * 
 * Encode (i.e., generate) a task specification string given a task
 * spec structure. The given string buffer (in which to write the
 * result) is assumed to have length buf_len.
 *
 * Returns 0 on success, -1 on failure.
 */
int encode_taskspec( const taskspec_t *tspec, char *tspec_string, size_t buf_len );

/* free_taskspec_struct
 * 
 * Free any dynamically allocated arrays in the given task spec
 * structure. This is available merely for convenience, but be careful
 * not to give it a bad task spec struct.
 *
 * Returns 0 on success, -1 on failure.
 */
int free_taskspec_struct( taskspec_t *tspec );


/*
 * Accessor methods:
 *
 * NOTE, indices are 0-based, i.e., numbering starts at 0.
 *
 * Requests for numerical min and max values that are of a special
 * type, such as positive infinity, result in an undefined return
 * value. That is, call the corresponding _special function before
 * attempting to read numerical (int or double) values.
 *
 * Access functions (prefix of "get") have undefined return values if
 * the given index is out of range or the taskspec_t pointer is NULL,
 * unless specified otherwise. NULL taskspec_t pointers are ignored in
 * all functions.
 *
 * All is... type functions return nonzero if the condition is true,
 * zero otherwise. For example, isEpisodic will return 1 if the
 * problem is episodic, 0 otherwise. -1 is returned upon error.
 *
 * Finally, these routines are somewhat awkward due to the lack of
 * exception handling and classes in C.
 */

int isEpisodic( taskspec_t *tspec );
int isContinuing( taskspec_t *tspec );
int isOtherType( taskspec_t *tspec );

/* get copies of range structures; the returned structure contains no
   pointers and is a copy (pass by value) of the original entry in the
   taskspec_t structure; hence, it can be manipulated at will by the
   caller. */
int_range_t getIntObs( taskspec_t *tspec, int index );
double_range_t getDoubleObs( taskspec_t *tspec, int index );
int_range_t getIntAct( taskspec_t *tspec, int index );
double_range_t getDoubleAct( taskspec_t *tspec, int index );

/* Observation space */
int getNumIntObs( taskspec_t *tspec );
int getIntObsMax( taskspec_t *tspec, int index );
int getIntObsMin( taskspec_t *tspec, int index );
int isIntObsMax_special( taskspec_t *tspec, int index );
int isIntObsMax_posInf( taskspec_t *tspec, int index );
int isIntObsMax_unspec( taskspec_t *tspec, int index );
int isIntObsMin_special( taskspec_t *tspec, int index );
int isIntObsMin_negInf( taskspec_t *tspec, int index );
int isIntObsMin_unspec( taskspec_t *tspec, int index );

int getNumDoubleObs( taskspec_t *tspec );
double getDoubleObsMax( taskspec_t *tspec, int index );
double getDoubleObsMin( taskspec_t *tspec, int index );
int isDoubleObsMax_special( taskspec_t *tspec, int index );
int isDoubleObsMax_posInf( taskspec_t *tspec, int index );
int isDoubleObsMax_unspec( taskspec_t *tspec, int index );
int isDoubleObsMin_special( taskspec_t *tspec, int index );
int isDoubleObsMin_negInf( taskspec_t *tspec, int index );
int isDoubleObsMin_unspec( taskspec_t *tspec, int index );

int getCharcountObs( taskspec_t *tspec );

/* Action space */
int getNumIntAct( taskspec_t *tspec );
int getIntActMax( taskspec_t *tspec, int index );
int getIntActMin( taskspec_t *tspec, int index );
int isIntActMax_special( taskspec_t *tspec, int index );
int isIntActMax_posInf( taskspec_t *tspec, int index );
int isIntActMax_unspec( taskspec_t *tspec, int index );
int isIntActMin_special( taskspec_t *tspec, int index );
int isIntActMin_negInf( taskspec_t *tspec, int index );
int isIntActMin_unspec( taskspec_t *tspec, int index );

int getNumDoubleAct( taskspec_t *tspec );
double getDoubleActMax( taskspec_t *tspec, int index );
double getDoubleActMin( taskspec_t *tspec, int index );
int isDoubleActMax_special( taskspec_t *tspec, int index );
int isDoubleActMax_posInf( taskspec_t *tspec, int index );
int isDoubleActMax_unspec( taskspec_t *tspec, int index );
int isDoubleActMin_special( taskspec_t *tspec, int index );
int isDoubleActMin_negInf( taskspec_t *tspec, int index );
int isDoubleActMin_unspec( taskspec_t *tspec, int index );

int getCharcountAct( taskspec_t *tspec );

/* Reward range */
double getRewardMax( taskspec_t *tspec );
double getRewardMin( taskspec_t *tspec );
int isRewardMax_special( taskspec_t *tspec );
int isRewardMax_posInf( taskspec_t *tspec );
int isRewardMax_unspec( taskspec_t *tspec ); 
int isRewardMin_special( taskspec_t *tspec );
int isRewardMin_negInf( taskspec_t *tspec );
int isRewardMin_unspec( taskspec_t *tspec );


#ifdef __cplusplus
}
#endif

#endif
