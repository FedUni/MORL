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


#ifdef __cplusplus
extern "C" {
#endif


#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#ifdef _WIN32
#pragma warning( disable : 4996 )
#define strncasecmp _strnicmp
#else
#include <strings.h>
#endif
#include <ctype.h>
#include <errno.h> /* used to detect errors in string-to-number conversions */

#include <rlglue/utils/C/TaskSpec_Parser.h>


/* for buffering during range processing. */
#define VECT_BUF_LEN 5

/* Some utility functions (not intended for use outside this file): */

/* Each "get dims" function returns a pointer to a dynamically
   allocated array and stores the array length to num_dims; If array
   length is zero, num_dims is set to 0 and NULL is returned. If an
   error occurs during parsing, NULL is returned and num_dims is set
   to -1.
*/
int_range_t *get_int_dims( const char *range_str, int *num_dims );
double_range_t *get_double_dims( const char *range_str, int *num_dims );

/* Return pointer to first non-space character in s;
   if no non-space character is found, returns NULL. */
char const *find_nonspace( const char *s );

/* Return pointer to space (white space character) immediately before
   first character of any of the possible task spec string entries
   that would indicate the end of a list of ranges: end_delim, INTS,
   DOUBLES, CHARCOUNT . Note that comparisons are (approximately) not
   case sensitive, e.g., either "INTS" or "ints" is valid. However,
   the end_delim string is searched for as is (i.e., comparison IS
   case sensitive).

   If no such location is found, returns NULL. */
char *find_end_ranges_list( const char *s, const char *end_delim );


int decode_taskspec( taskspec_t *tspec, const char *tspec_string )
{
	char const *cp, *cp_next; /* character pointers (for string parsing) */
	char *cp_candidate_next; /* when deciding between two possible
								"next" character pointers. */
	char *tmpstr; /* temporary strings for passing substrings of the 
					 task spec string to other functions. */

	char *endptr; /* used for detecting errors in string-to-double
					 conversion with strtod */

	/* set all pointers to NULL in the taskspec struct; this is
	   necessary for clean calls to free_taskspec_struct. */
	tspec->version = NULL;
	tspec->int_observations = NULL;
	tspec->double_observations = NULL;
	tspec->int_actions = NULL;
	tspec->double_actions = NULL;
	tspec->extra_spec = NULL;

	/* initialize all counts to zero */
	tspec->num_int_observations = 0;
	tspec->num_double_observations = 0;
	tspec->charcount_observations = 0;
	tspec->num_int_actions = 0;
	tspec->num_double_actions = 0;
	tspec->charcount_actions = 0;

	/*
	 * Determine version:
	 *
	 */
	cp = find_nonspace( tspec_string );
	if (cp == NULL
		|| strncasecmp( cp, "VERSION", strlen("VERSION") ))
		return -1;
	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp );
	if (cp == NULL)
		return -1;
	if (strncmp( cp, CURRENT_VERSION, strlen(CURRENT_VERSION) )) {
		/* store copy of unrecognized version name to task spec structure */
		cp_next = strchr( cp, ' ' );
		if (cp_next == NULL)
			return -1;
		tspec->version = (char *)malloc( cp_next-cp+1 );
		if (tspec->version == NULL) /* insufficient memory!? */
			return -1;
		strncpy( tspec->version, cp, cp_next-cp );
		*(tspec->version+(cp_next-cp)) = '\0';
		return 1; /* unrecognized or unsupported version */
	}

	/* prepare to save copy of version string */
	cp_next = strchr( cp, ' ' );
	if (cp_next == NULL)
		return -1;

	/* save version string to taskspec struct */
	tspec->version = (char *)malloc( cp_next-cp+1 );
	if (tspec->version == NULL) /* insufficient memory!? */
		return -1;
	strncpy( tspec->version, cp, cp_next-cp );
	*(tspec->version+(cp_next-cp)) = '\0';
	cp = cp_next+1;

	/*
	 * Determine problem type:
	 *
	 */
	cp = find_nonspace( cp );
	if (cp == NULL
		|| strncasecmp( cp, "PROBLEMTYPE", strlen("PROBLEMTYPE") )) {
		free_taskspec_struct( tspec );
		return -1;
	}
	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp );
	if (cp == NULL) {
		free_taskspec_struct( tspec );
		return -1;
	}
	if (!strncmp( cp, "episodic", strlen("episodic") )) {
		tspec->problem_type = TSPEC_EPISODIC;
	} else if (!strncmp( cp, "continuing", strlen("continuing") )) {
		tspec->problem_type = TSPEC_CONTINUING;
	} else {
		tspec->problem_type = TSPEC_OTHER;
	}

	/*
	 * Determine discount factor:
	 *
	 */
	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp ); 
	if (cp == NULL
		|| strncasecmp( cp, "DISCOUNTFACTOR", strlen("DISCOUNTFACTOR") )) {
		free_taskspec_struct( tspec );
		return -1;
	}
	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp );
	if (cp == NULL || isalpha( *cp )) {
		free_taskspec_struct( tspec );
		return -1;
	}
	cp_next = strchr( cp, ' ' );
	if (cp_next == NULL) {
		free_taskspec_struct( tspec );
		return -1;
	}
	tmpstr = (char *)malloc( cp_next-cp+1 );
	if (tmpstr == NULL) { /* insufficient memory!? */
		free_taskspec_struct( tspec );
		return -1;
	}
	strncpy( tmpstr, cp, cp_next-cp );
	*(tmpstr+(cp_next-cp)) = '\0';
	errno = 0;
	tspec->discount_factor = strtod( tmpstr, &endptr );
	if (tspec->discount_factor == 0
		&& (endptr == tmpstr || errno == ERANGE)) {
		free( tmpstr );
		free_taskspec_struct( tspec );
		return -1; /* given discount factor is invalid */
	}
	free( tmpstr );
	if (tspec->discount_factor < 0. || tspec->discount_factor > 1.) {
		free_taskspec_struct( tspec );
		return -1; /* discount factor out of bounds */
	}
	cp = cp_next+1;

	/*
	 * Observation space specifications:
	 *
	 */
	cp = find_nonspace( cp );
	if (cp == NULL
		|| strncasecmp( cp, "OBSERVATIONS", strlen("OBSERVATIONS") )) {
		free_taskspec_struct( tspec );
		return -1;
	}
	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp );
	while (cp != NULL
		   && strncasecmp( cp, "ACTIONS", strlen("ACTIONS") )) {

		/* determine dimension type */
		if (!strncasecmp( cp, "INTS", strlen("INTS") )) {
			
			cp = strchr( cp, ' ' );
			cp = find_nonspace( cp );

			cp_next = find_end_ranges_list( cp, "ACTIONS" );
			cp_candidate_next = find_end_ranges_list( cp, "actions" );
			if (cp_next == NULL && cp_candidate_next == NULL) {
				free_taskspec_struct( tspec );
				return -1; /* early task spec string termination */
			}
			if (cp_next == NULL
				|| (cp_candidate_next != NULL && cp_candidate_next < cp_next))
				cp_next = cp_candidate_next;

			if (cp == cp_next)
				continue;

			tmpstr = (char *)malloc( cp_next-cp );
			if (tmpstr == NULL) { /* insufficient memory!? */
				free_taskspec_struct( tspec );
				return -1;
			}
			strncpy( tmpstr, cp, cp_next-cp-1 );
			*(tmpstr+(cp_next-cp-1)) = '\0';
			tspec->int_observations = get_int_dims( tmpstr, &(tspec->num_int_observations) );
			free( tmpstr );
			if (tspec->num_int_observations == -1) {
				free_taskspec_struct( tspec );
				return -1;
			}

		} else if (!strncasecmp( cp, "DOUBLES", strlen("DOUBLES") )) {
			
			cp = strchr( cp, ' ' );
			cp = find_nonspace( cp );

			cp_next = find_end_ranges_list( cp, "ACTIONS" );
			cp_candidate_next = find_end_ranges_list( cp, "actions" );
			if (cp_next == NULL && cp_candidate_next == NULL) {
				free_taskspec_struct( tspec );
				return -1; /* early task spec string termination */
			}
			if (cp_next == NULL
				|| (cp_candidate_next != NULL && cp_candidate_next < cp_next))
				cp_next = cp_candidate_next;

			if (cp == cp_next)
				continue;

			tmpstr = (char *)malloc( cp_next-cp );
			if (tmpstr == NULL) { /* insufficient memory!? */
				free_taskspec_struct( tspec );
				return -1;
			}
			strncpy( tmpstr, cp, cp_next-cp-1 );
			*(tmpstr+(cp_next-cp-1)) = '\0';
			tspec->double_observations = get_double_dims( tmpstr, &(tspec->num_double_observations) );
			free( tmpstr );
			if (tspec->num_double_observations == -1) {
				free_taskspec_struct( tspec );
				return -1;
			}

		} else if (!strncasecmp( cp, "CHARCOUNT", strlen("CHARCOUNT") )) {
			
			cp = strchr( cp, ' ' );
			cp = find_nonspace( cp );
			cp_next = find_end_ranges_list( cp, "ACTIONS" );
			cp_candidate_next = find_end_ranges_list( cp, "actions" );
			if (cp_next == NULL && cp_candidate_next == NULL) {
				free_taskspec_struct( tspec );
				return -1; /* early task spec string termination */
			}
			if (cp_next == NULL
				|| (cp_candidate_next != NULL && cp_candidate_next < cp_next))
				cp_next = cp_candidate_next;

			if (cp == cp_next)
				continue;

			/* read the character count integer */
			errno = 0;
			tspec->charcount_observations = strtol( cp, &endptr, 0 );
			if (endptr == cp || errno == ERANGE
				|| tspec->charcount_observations < 0) {
				free_taskspec_struct( tspec );
				return -1; /* count read failed or count is negative */
			}

		} else { 

			free_taskspec_struct( tspec );
			return -1; /* unrecognized dimension type */

		}

		cp = cp_next;

	}
	if (cp == NULL) {
		free_taskspec_struct( tspec );
		return -1; /* early task spec string termination */
	}

	/*
	 * Action space specifications:
	 *
	 */
	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp );
	while (cp != NULL
		   && strncasecmp( cp, "REWARDS", strlen("REWARDS") )) {

		/* determine dimension type */
		if (!strncasecmp( cp, "INTS", strlen("INTS") )) {
			
			cp = strchr( cp, ' ' );
			cp = find_nonspace( cp );

			cp_next = find_end_ranges_list( cp, "REWARDS" );
			cp_candidate_next = find_end_ranges_list( cp, "rewards" );
			if (cp_next == NULL && cp_candidate_next == NULL) {
				free_taskspec_struct( tspec );
				return -1; /* early task spec string termination */
			}
			if (cp_next == NULL
				|| (cp_candidate_next != NULL && cp_candidate_next < cp_next))
				cp_next = cp_candidate_next;

			if (cp == cp_next)
				continue;

			tmpstr = (char *)malloc( cp_next-cp );
			if (tmpstr == NULL) { /* insufficient memory!? */
				free_taskspec_struct( tspec );
				return -1;
			}
			strncpy( tmpstr, cp, cp_next-cp-1 );
			*(tmpstr+(cp_next-cp-1)) = '\0';
			tspec->int_actions = get_int_dims( tmpstr, &(tspec->num_int_actions) );
			free( tmpstr );
			if (tspec->num_int_actions == -1) {
				free_taskspec_struct( tspec );
				return -1;
			}

		} else if (!strncasecmp( cp, "DOUBLES", strlen("DOUBLES") )) {
			
			cp = strchr( cp, ' ' );
			cp = find_nonspace( cp );

			cp_next = find_end_ranges_list( cp, "REWARDS" );
			cp_candidate_next = find_end_ranges_list( cp, "rewards" );
			if (cp_next == NULL && cp_candidate_next == NULL) {
				free_taskspec_struct( tspec );
				return -1; /* early task spec string termination */
			}
			if (cp_next == NULL
				|| (cp_candidate_next != NULL && cp_candidate_next < cp_next))
				cp_next = cp_candidate_next;

			if (cp == cp_next)
				continue;

			tmpstr = (char *)malloc( cp_next-cp );
			if (tmpstr == NULL) { /* insufficient memory!? */
				free_taskspec_struct( tspec );
				return -1;
			}
			strncpy( tmpstr, cp, cp_next-cp-1 );
			*(tmpstr+(cp_next-cp-1)) = '\0';
			tspec->double_actions = get_double_dims( tmpstr, &(tspec->num_double_actions) );
			free( tmpstr );
			if (tspec->num_double_actions == -1) {
				free_taskspec_struct( tspec );
				return -1;
			}

		} else if (!strncasecmp( cp, "CHARCOUNT", strlen("CHARCOUNT") )) {
			
			cp = strchr( cp, ' ' );
			cp = find_nonspace( cp );
			cp_next = find_end_ranges_list( cp, "REWARDS" );
			cp_candidate_next = find_end_ranges_list( cp, "rewards" );
			if (cp_next == NULL && cp_candidate_next == NULL) {
				free_taskspec_struct( tspec );
				return -1; /* early task spec string termination */
			}
			if (cp_next == NULL
				|| (cp_candidate_next != NULL && cp_candidate_next < cp_next))
				cp_next = cp_candidate_next;

			if (cp == cp_next)
				continue;

			/* read the character count integer */
			errno = 0;
			tspec->charcount_actions = strtol( cp, &endptr, 0 );
			if (endptr == cp || errno == ERANGE
				|| tspec->charcount_actions < 0) {
				free_taskspec_struct( tspec );
				return -1; /* count read failed or count is negative */
			}

		} else { 

			free_taskspec_struct( tspec );
			return -1; /* unrecognized dimension type */

		}

		cp = cp_next;

	}
	if (cp == NULL) {
		free_taskspec_struct( tspec );
		return -1; /* early task spec string termination */
	}

	/*
	 * Reward range:
	 *
	 */
	/* initialize the reward range structure */
	tspec->reward.special_min = tspec->reward.special_max = RVAL_NOTSPECIAL;

	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp );
	if (*cp != '(') {
		free_taskspec_struct( tspec );
		return -1; /* malformed reward range */
	}
	cp = find_nonspace( cp+1 );
	if (cp == NULL || *cp == '\0') {
		free_taskspec_struct( tspec );
		return -1; /* early task spec string termination */
	}
	
	if (isalpha( *cp )) { /* a special value? */

		if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
			tspec->reward.special_min = RVAL_UNSPEC;
		} else if (!strncasecmp( cp, "NEGINF", strlen("NEGINF") )) {
			tspec->reward.special_min = RVAL_NEGINF;
		} else {
			free_taskspec_struct( tspec );
			return -1; /* malformed expression: unrecognized special type */
		}

	} else { /* attempt to extract the double */

		errno = 0;
		tspec->reward.min = strtod( cp, &endptr );
		if (endptr == cp || errno == ERANGE) {
			free_taskspec_struct( tspec );
			return -1; /* malformed expression: invalid double string */
		}

	}

	cp = strchr( cp, ' ' );
	cp = find_nonspace( cp );
	if (cp == NULL) {
		free_taskspec_struct( tspec ); 
		return -1; /* early task spec string termination */
	}

	if (isalpha( *cp )) { /* a special value? */

		if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
			tspec->reward.special_max = RVAL_UNSPEC;
		} else if (!strncasecmp( cp, "POSINF", strlen("POSINF") )) {
			tspec->reward.special_max = RVAL_POSINF;
		} else {
			free_taskspec_struct( tspec );
			return -1; /* malformed expression: unrecognized special type */
		}

	} else { /* attempt to extract the double */

		errno = 0;
		tspec->reward.max = strtod( cp, &endptr );
		if (endptr == cp || errno == ERANGE) {
			free_taskspec_struct( tspec );
			return -1; /* malformed expression: invalid double string */
		}

	}
	
	cp_next = strchr( cp, ')' );
	cp = strchr( cp, ' ' );
	if (cp == NULL || cp_next == NULL || *(cp_next+1) == '\0' ) {
		free_taskspec_struct( tspec );
		return -1; /* early task spec string termination */
	}
	if (cp < cp_next) {
		cp = find_nonspace( cp );
		if (*cp != ')') {
			free_taskspec_struct( tspec );
			return -1; /* invalid tuple size */
		}
	}
	cp = cp_next+1;

	/*
	 * Extra spec string:
	 *
	 */
	cp = find_nonspace( cp );
	if (cp == NULL
		|| strncasecmp( cp, "EXTRA", strlen("EXTRA") )) {
		free_taskspec_struct( tspec );
		return -1; /* early task spec string termination */
	}
	cp = strchr( cp, ' ' );
	if (cp != NULL
		&& (cp = find_nonspace( cp )) != NULL) {
		tspec->extra_spec = (char *)malloc( strlen(cp)+1 );
		if (tspec->extra_spec == NULL) {
			free_taskspec_struct( tspec );
			return -1; /* insufficient memory!? */
		}
		strcpy( tspec->extra_spec, cp );
	}

	return 0;
}


int encode_taskspec( const taskspec_t *tspec, char *tspec_string, size_t buf_len )
{
	int i;
	char *cp, *cp_end;
	int nb; /* number of characters printed */

	if (buf_len < 2) /* ignore impractical buffer sizes */
		return -1;

	cp = tspec_string; /* step through the buffer as the task spec string is built */
	cp_end = tspec_string+buf_len-1; /* point to end of available string buffer */

	if (tspec->version != NULL) {
		nb = snprintf( cp, cp_end-cp, "VERSION %s ", tspec->version );
		if (nb < strlen("VERSION  ")
			|| cp+nb > tspec_string+buf_len-2)
			return -1;
	} else {
		return -1;
	}
	cp += nb;

	switch (tspec->problem_type) {
	case TSPEC_EPISODIC:
		nb = snprintf( cp, cp_end-cp, "PROBLEMTYPE episodic " );
		if (nb < strlen("PROBLEMTYPE episodic ")
			|| cp+nb >= cp_end)
			return -1;
		break;
	case TSPEC_CONTINUING:
		nb = snprintf( cp, cp_end-cp, "PROBLEMTYPE continuing " );
		if (nb < strlen("PROBLEMTYPE continuing ")
			|| cp+nb >= cp_end)
			return -1;
		break;
	case TSPEC_OTHER:
		nb = snprintf( cp, cp_end-cp, "PROBLEMTYPE other " );
		if (nb < strlen("PROBLEMTYPE other ")
			|| cp+nb >= cp_end)
			return -1;
		break;
	default:
		return -1;
	}
	cp += nb;

	nb = snprintf( cp, cp_end-cp, "DISCOUNTFACTOR %g ", tspec->discount_factor );
	if (nb < strlen("DISCOUNTFACTOR ") || cp+nb >= cp_end)
		return -1;
	cp += nb;

	/*
	 * Print observations structure:
	 *
	 */
	nb = snprintf( cp, cp_end-cp, "OBSERVATIONS " );
	if (nb < strlen("OBSERVATIONS ") || cp+nb >= cp_end)
		return -1;
	cp += nb;
	
	/* print observation INTS */
	if (tspec->num_int_observations > 0) {
		nb = snprintf( cp, cp_end-cp, "INTS " );
		if (nb < strlen("INTS ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
		for (i = 0; i < tspec->num_int_observations; i++) {
			nb = snprintf( cp, cp_end-cp, "(" );
			if (nb < 1 || cp+nb >= cp_end)
				return -1;
			cp += nb;

			/* print tuple repeat count if greater than 1 */
			if ((tspec->int_observations+i)->repeat_count > 1) {
				nb = snprintf( cp, cp_end-cp, "%d ", (tspec->int_observations+i)->repeat_count );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}
			
			/* print infimum */
			if ((tspec->int_observations+i)->special_min == RVAL_NEGINF) {
				nb = snprintf( cp, cp_end-cp, "NEGINF " );
				if (nb < strlen("NEGINF ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->int_observations+i)->special_min == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC " );
				if (nb < strlen("UNSPEC ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%d ", (tspec->int_observations+i)->min );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}

			/* print supremum */
			if ((tspec->int_observations+i)->special_max == RVAL_POSINF) {
				nb = snprintf( cp, cp_end-cp, "POSINF) " );
				if (nb < strlen("POSINF) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->int_observations+i)->special_max == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC) " );
				if (nb < strlen("UNSPEC) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%d) ", (tspec->int_observations+i)->max );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}
		}
	}

	/* print observation DOUBLES */
	if (tspec->num_double_observations > 0) {
		nb = snprintf( cp, cp_end-cp, "DOUBLES " );
		if (nb < strlen("DOUBLES ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
		for (i = 0; i < tspec->num_double_observations; i++) {
			nb = snprintf( cp, cp_end-cp, "(" );
			if (nb < 1 || cp+nb >= cp_end)
				return -1;
			cp += nb;

			/* print tuple repeat count if greater than 1 */
			if ((tspec->double_observations+i)->repeat_count > 1) {
				nb = snprintf( cp, cp_end-cp, "%d ", (tspec->double_observations+i)->repeat_count );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}

			/* print infimum */
			if ((tspec->double_observations+i)->special_min == RVAL_NEGINF) {
				nb = snprintf( cp, cp_end-cp, "NEGINF " );
				if (nb < strlen("NEGINF ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->double_observations+i)->special_min == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC " );
				if (nb < strlen("UNSPEC ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%g ", (tspec->double_observations+i)->min );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}

			/* print supremum */
			if ((tspec->double_observations+i)->special_max == RVAL_POSINF) {
				nb = snprintf( cp, cp_end-cp, "POSINF) " );
				if (nb < strlen("POSINF) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->double_observations+i)->special_max == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC) " );
				if (nb < strlen("UNSPEC) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%g) ", (tspec->double_observations+i)->max );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}
		}
	}

	if (tspec->charcount_observations > 0) {
		nb = snprintf( cp, cp_end-cp, "CHARCOUNT %d ", tspec->charcount_observations );
		if (nb < strlen("CHARCOUNT ")+1 || cp+nb >= cp_end)
			return -1;
		cp += nb;
	}

	/*
	 * Print actions structure:
	 *
	 */
	nb = snprintf( cp, cp_end-cp, "ACTIONS " );
	if (nb < strlen("ACTIONS ") || cp+nb >= cp_end)
		return -1;
	cp += nb;
	
	/* print action INTS */
	if (tspec->num_int_actions > 0) {
		nb = snprintf( cp, cp_end-cp, "INTS " );
		if (nb < strlen("INTS ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
		for (i = 0; i < tspec->num_int_actions; i++) {
			nb = snprintf( cp, cp_end-cp, "(" );
			if (nb < 1 || cp+nb >= cp_end)
				return -1;
			cp += nb;

			/* print tuple repeat count if greater than 1 */
			if ((tspec->int_actions+i)->repeat_count > 1) {
				nb = snprintf( cp, cp_end-cp, "%d ", (tspec->int_actions+i)->repeat_count );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}

			/* print infimum */
			if ((tspec->int_actions+i)->special_min == RVAL_NEGINF) {
				nb = snprintf( cp, cp_end-cp, "NEGINF " );
				if (nb < strlen("NEGINF ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->int_actions+i)->special_min == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC " );
				if (nb < strlen("UNSPEC ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%d ", (tspec->int_actions+i)->min );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}

			/* print supremum */
			if ((tspec->int_actions+i)->special_max == RVAL_POSINF) {
				nb = snprintf( cp, cp_end-cp, "POSINF) " );
				if (nb < strlen("POSINF) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->int_actions+i)->special_max == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC) " );
				if (nb < strlen("UNSPEC) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%d) ", (tspec->int_actions+i)->max );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}
		}
	}

	/* print action DOUBLES */
	if (tspec->num_double_actions > 0) {
		nb = snprintf( cp, cp_end-cp, "DOUBLES " );
		if (nb < strlen("DOUBLES ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
		for (i = 0; i < tspec->num_double_actions; i++) {
			nb = snprintf( cp, cp_end-cp, "(" );
			if (nb < 1 || cp+nb >= cp_end)
				return -1;
			cp += nb;

			/* print tuple repeat count if greater than 1 */
			if ((tspec->double_actions+i)->repeat_count > 1) {
				nb = snprintf( cp, cp_end-cp, "%d ", (tspec->double_actions+i)->repeat_count );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}

			/* print infimum */
			if ((tspec->double_actions+i)->special_min == RVAL_NEGINF) {
				nb = snprintf( cp, cp_end-cp, "NEGINF " );
				if (nb < strlen("NEGINF ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->double_actions+i)->special_min == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC " );
				if (nb < strlen("UNSPEC ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%g ", (tspec->double_actions+i)->min );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}

			/* print supremum */
			if ((tspec->double_actions+i)->special_max == RVAL_POSINF) {
				nb = snprintf( cp, cp_end-cp, "POSINF) " );
				if (nb < strlen("POSINF) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else if ((tspec->double_actions+i)->special_max == RVAL_UNSPEC) {
				nb = snprintf( cp, cp_end-cp, "UNSPEC) " );
				if (nb < strlen("UNSPEC) ") || cp+nb >= cp_end)
					return -1;
				cp += nb;
			} else {
				nb = snprintf( cp, cp_end-cp, "%g) ", (tspec->double_actions+i)->max );
				if (nb < 2 || cp+nb >= cp_end)
					return -1;
				cp += nb;
			}
		}
	}

	if (tspec->charcount_actions > 0) {
		nb = snprintf( cp, cp_end-cp, "CHARCOUNT %d ", tspec->charcount_actions );
		if (nb < strlen("CHARCOUNT ")+1 || cp+nb >= cp_end)
			return -1;
		cp += nb;
	}

	/*
	 * Print the reward range:
	 *
	 */
	nb = snprintf( cp, cp_end-cp, "REWARDS (" );
	if (nb < strlen("REWARDS (") || cp+nb >= cp_end)
		return -1;
	cp += nb;
	if ((tspec->reward).special_min == RVAL_NEGINF) {
		nb = snprintf( cp, cp_end-cp, "NEGINF " );
		if (nb < strlen("NEGINF ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
	} else if ((tspec->reward).special_min == RVAL_UNSPEC) {
		nb = snprintf( cp, cp_end-cp, "UNSPEC " );
		if (nb < strlen("UNSPEC ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
	} else {
		nb = snprintf( cp, cp_end-cp, "%g ", (tspec->reward).min );
		if (nb < 2 || cp+nb >= cp_end)
			return -1;
		cp += nb;
	}
	if ((tspec->reward).special_max == RVAL_POSINF) {
		nb = snprintf( cp, cp_end-cp, "POSINF) " );
		if (nb < strlen("POSINF) ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
	} else if ((tspec->reward).special_max == RVAL_UNSPEC) {
		nb = snprintf( cp, cp_end-cp, "UNSPEC) " );
		if (nb < strlen("UNSPEC) ") || cp+nb >= cp_end)
			return -1;
		cp += nb;
	} else {
		nb = snprintf( cp, cp_end-cp, "%g) ", (tspec->reward).max );
		if (nb < 2 || cp+nb >= cp_end)
			return -1;
		cp += nb;
	}

	/* Extra spec string */
	if (tspec->extra_spec != NULL) {
		nb = snprintf( cp, cp_end-cp, "EXTRA %s", tspec->extra_spec );
		if (nb < strlen("EXTRA ")+strlen(tspec->extra_spec)
			|| cp+nb >= cp_end)
			return -1;
		cp += nb;
	}
	*cp = '\0';

	return 0;
}


int free_taskspec_struct( taskspec_t *tspec )
{
	/* There is probably a better, fault-tolerant way to do this... */

	/* free allocated arrays; note that free ignores NULL pointers */
	free( tspec->version );
	free( tspec->int_observations );
	free( tspec->double_observations );
	free( tspec->int_actions );
	free( tspec->double_actions );
	free( tspec->extra_spec );

	/* set all pointers to NULL in the taskspec struct */
	tspec->version = NULL;
	tspec->int_observations = NULL;
	tspec->double_observations = NULL;
	tspec->int_actions = NULL;
	tspec->double_actions = NULL;
	tspec->extra_spec = NULL;

	/* initialize all counts to zero */
	tspec->num_int_observations = 0;
	tspec->num_double_observations = 0;
	tspec->charcount_observations = 0;
	tspec->num_int_actions = 0;
	tspec->num_double_actions = 0;
	tspec->charcount_actions = 0;

	return 0;
}


int_range_t *get_int_dims( const char *range_str, int *num_dims )
{
	char const *cp,
		*cp_close; /* point to the closing paren of the current pair */

	int_range_t current_ran; /* range struct for processing each pair of parens */
	
	int_range_t *ran_vect;
	int buf_len;
	int_range_t *tmp_ir_ptr; /* used when growing the int_range_t array buffer */
	
	char *endptr; /* used for detecting errors in string-to-integer
					 conversion with strtol */

	*num_dims = 0;

	/* ignore empty parameter strings */
	if (range_str == NULL)
		return NULL;

	cp = range_str;
	
	/* allocate array of range structs; this will grow in length as more
	   ranges are read. */
	buf_len = VECT_BUF_LEN;
	ran_vect = (int_range_t *)malloc( VECT_BUF_LEN*sizeof(int_range_t) );

	/* step through each pair of parens */
	while ((cp = strchr( cp, '(' )) != NULL) {

		cp_close = strchr( cp, ')' );
		if (cp_close == NULL) {
			free( ran_vect );
			*num_dims = -1; /* malformed expression: missing closing paren */
			return NULL;
		}

		/* clear previous current_ran field values */
		current_ran.min = current_ran.max = 0;
		current_ran.special_min = current_ran.special_max = RVAL_NOTSPECIAL;
		current_ran.repeat_count = 1;

		/* 
		 * Extract the integers, checking for special values:
		 *
		 */

		/* first entry: either repeat-count or min-value */
		cp = find_nonspace( cp+1 );
		if (cp == cp_close) {
			free( ran_vect );
			*num_dims = -1; /* malformed expression: an empty tuple! */
			return NULL;
		}

		if (isalpha( *cp )) { /* a special value? */

			if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
				current_ran.special_min = RVAL_UNSPEC;
			} else if (!strncasecmp( cp, "NEGINF", strlen("NEGINF") )) {
				current_ran.special_min = RVAL_NEGINF;
			} else {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: unrecognized special type */
				return NULL;
			}

		} else { /* attempt to extract the integer */

			errno = 0;
			current_ran.min = strtol( cp, &endptr, 0 );
			if (endptr == cp || errno == ERANGE) {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: invalid integer string */
				return NULL;
			}

		}

		/* second entry: either min-value (if previous was a
		   repeat-count) or max-value.

           NOTE: for now, I assume this is the max-value and deal with
		   the special case of a 3-tuple (in which a repeat-count is
		   specified) only if a third entry is found before the
		   closing paren. */
		cp = strchr( cp, ' ' );
		cp = find_nonspace( cp );
		if (cp == cp_close || cp == NULL) {
			free( ran_vect );
			*num_dims = -1; /* malformed expression: a one tuple! */
			return NULL;
		}

		if (isalpha( *cp )) { /* a special value? */

			if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
				current_ran.special_max = RVAL_UNSPEC;
			} else if (!strncasecmp( cp, "POSINF", strlen("POSINF") )) {
				current_ran.special_max = RVAL_POSINF;
			} else {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: unrecognized special type */
				return NULL;
			}

		} else { /* attempt to extract the integer */

			errno = 0;
			current_ran.max = strtol( cp, &endptr, 0 );
			if (endptr == cp || errno == ERANGE) {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: invalid integer string */
				return NULL;
			}

		}

		/* move forward to last (third) entry or closing paren */
		cp = strchr( cp, ' ' );
		if (cp == NULL)
			cp = cp_close;
		else
			cp = find_nonspace( cp );

		if (cp != NULL && cp < cp_close) { /* Third entry is present,
											  thus a repeat-count was given. */

			if (current_ran.special_min != RVAL_NOTSPECIAL
				|| current_ran.special_max == RVAL_POSINF) {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: repeat-count
								   cannot be a special value, or min
								   special value cannot be positive
								   infinity. */
				return NULL;
			}

			/* adjust values in int_range struct to handle third entry */
			current_ran.repeat_count = current_ran.min;
			current_ran.min = current_ran.max;
			current_ran.special_min = current_ran.special_max;

			if (current_ran.repeat_count < 1) {
				free( ran_vect );
				*num_dims = -1; /* invalid repeat-count */
				return NULL;
			}
			
			if (isalpha( *cp )) { /* a special value? */

				if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
					current_ran.special_max = RVAL_UNSPEC;
				} else if (!strncasecmp( cp, "POSINF", strlen("POSINF") )) {
					current_ran.special_max = RVAL_POSINF;
				} else {
					free( ran_vect );
					*num_dims = -1; /* malformed expression: unrecognized special type */
					return NULL;
				}

			} else { /* attempt to extract the integer */

				errno = 0;
				current_ran.max = strtol( cp, &endptr, 0 );
				if (endptr == cp || errno == ERANGE) {
					free( ran_vect );
					*num_dims = -1; /* malformed expression: invalid integer string */
					return NULL;
				}

			}

			cp = strchr( cp, ' ' );
			if (cp != NULL && cp < cp_close) {
				cp = find_nonspace( cp );
				if (cp != cp_close) {
					free( ran_vect );
					*num_dims = -1; /* malformed expression: invalid tuple size */
					return NULL;
				}
			}

		}
		cp = cp_close+1;		

		(*num_dims)++;
		if (*num_dims > buf_len) { /* grow buffer if necessary */
			buf_len = (*num_dims)+VECT_BUF_LEN;
			tmp_ir_ptr = (int_range_t *)realloc( ran_vect, buf_len*sizeof(int_range_t) );
			if (tmp_ir_ptr == NULL) {
				free( ran_vect );
				*num_dims = -1; /* realloc call failed */
				return NULL;
			}
			ran_vect = tmp_ir_ptr;
		}
		
		/* finally, add this int range struct to the array */
		(ran_vect+(*num_dims)-1)->min = current_ran.min;
		(ran_vect+(*num_dims)-1)->max = current_ran.max;
		(ran_vect+(*num_dims)-1)->special_min = current_ran.special_min;
		(ran_vect+(*num_dims)-1)->special_max = current_ran.special_max;
		(ran_vect+(*num_dims)-1)->repeat_count = current_ran.repeat_count;

	}

	/* free unused buffer space */
	if (buf_len > *num_dims) {
		tmp_ir_ptr = realloc( ran_vect, (*num_dims)*sizeof(int_range_t) );
		if (tmp_ir_ptr == NULL) {
			free( ran_vect );
			*num_dims = -1; /* memory error! */
			return NULL;
		}
		ran_vect = tmp_ir_ptr;
	}

	return ran_vect;
}


double_range_t *get_double_dims( const char *range_str, int *num_dims )
{
	char const *cp,
		*cp_close; /* point to the closing paren of the current pair */

	double_range_t current_ran; /* range struct for processing each pair of parens */
	
	double_range_t *ran_vect;
	int buf_len;
	double_range_t *tmp_ir_ptr; /* used when growing the double_range_t array buffer */
	
	char *endptr; /* used for detecting errors in string-to-number
					 conversions with strtol and strtod */

	/* ignore empty parameter strings */
	if (range_str == NULL) {
		*num_dims = 0;
		return NULL;
	}

	cp = range_str;
	
	/* allocate array of range structs; this will grow in length as more
	   ranges are read. */
	buf_len = VECT_BUF_LEN;
	*num_dims = 0;
	ran_vect = (double_range_t *)malloc( VECT_BUF_LEN*sizeof(double_range_t) );

	/* step through each pair of parens */
	while ((cp = strchr( cp, '(' )) != NULL) {

		cp_close = strchr( cp, ')' );
		if (cp_close == NULL) {
			free( ran_vect );
			*num_dims = -1; /* malformed expression: missing closing paren */
			return NULL;
		}

		/* clear previous current_ran field values */
		current_ran.min = current_ran.max = 0.;
		current_ran.special_min = current_ran.special_max = RVAL_NOTSPECIAL;
		current_ran.repeat_count = 1;

		/* 
		 * Extract the numbers, checking for special values:
		 *
		 */

		/* first entry: either repeat-count or min-value */
		cp = find_nonspace( cp+1 );
		if (cp == cp_close) {
			free( ran_vect );
			*num_dims = -1; /* malformed expression: an empty tuple! */
			return NULL;
		}

		if (isalpha( *cp )) { /* a special value? */

			if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
				current_ran.special_min = RVAL_UNSPEC;
			} else if (!strncasecmp( cp, "NEGINF", strlen("NEGINF") )) {
				current_ran.special_min = RVAL_NEGINF;
			} else {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: unrecognized special type */
				return NULL;
			}

		} else { /* attempt to extract the double */

			errno = 0;
			current_ran.min = strtod( cp, &endptr );
			if (endptr == cp || errno == ERANGE) {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: invalid double string */
				return NULL;
			}

		}

		/* second entry: either min-value (if previous was a
		   repeat-count) or max-value.

           NOTE: for now, I assume this is the max-value and deal with
		   the special case of a 3-tuple (in which a repeat-count is
		   specified) only if a third entry is found before the
		   closing paren. */
		cp = strchr( cp, ' ' );
		cp = find_nonspace( cp );
		if (cp == cp_close || cp == NULL) {
			free( ran_vect );
			*num_dims = -1; /* malformed expression: a one tuple! */
			return NULL;
		}

		if (isalpha( *cp )) { /* a special value? */

			if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
				current_ran.special_max = RVAL_UNSPEC;
			} else if (!strncasecmp( cp, "POSINF", strlen("POSINF") )) {
				current_ran.special_max = RVAL_POSINF;
			} else {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: unrecognized special type */
				return NULL;
			}

		} else { /* attempt to extract the double */

			errno = 0;
			current_ran.max = strtod( cp, &endptr );
			if (endptr == cp || errno == ERANGE) {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: invalid double string */
				return NULL;
			}

		}

		/* move forward to last (third) entry or closing paren */
		cp = strchr( cp, ' ' );
		if (cp == NULL)
			cp = cp_close;
		else
			cp = find_nonspace( cp );

		if (cp != NULL && cp < cp_close) { /* Third entry is present, thus a
											  repeat-count was given. */

			if (current_ran.special_min != RVAL_NOTSPECIAL
				|| current_ran.special_max == RVAL_POSINF) {
				free( ran_vect );
				*num_dims = -1; /* malformed expression: repeat-count
								   cannot be a special value, or min
								   special value cannot be positive
								   infinity. */
				return NULL;
			}

			/* adjust values in double_range struct to handle third entry */
			current_ran.repeat_count = (int)current_ran.min;
			current_ran.min = current_ran.max;
			current_ran.special_min = current_ran.special_max;

			if (current_ran.repeat_count < 1) {
				free( ran_vect );
				*num_dims = -1; /* invalid repeat-count */
				return NULL;
			}
			
			if (isalpha( *cp )) { /* a special value? */

				if (!strncasecmp( cp, "UNSPEC", strlen("UNSPEC") )) {
					current_ran.special_max = RVAL_UNSPEC;
				} else if (!strncasecmp( cp, "POSINF", strlen("POSINF") )) {
					current_ran.special_max = RVAL_POSINF;
				} else {
					free( ran_vect );
					*num_dims = -1; /* malformed expression: unrecognized special type */
					return NULL;
				}

			} else { /* attempt to extract the double */

				errno = 0;
				current_ran.max = strtod( cp, &endptr );
				if (endptr == cp || errno == ERANGE) {
					free( ran_vect );
					*num_dims = -1; /* malformed expression: invalid double string */
					return NULL;
				}

			}

			cp = strchr( cp, ' ' );
			if (cp != NULL && cp < cp_close) {
				cp = find_nonspace( cp );
				if (cp != cp_close) {
					free( ran_vect );
					*num_dims = -1; /* malformed expression: invalid tuple size */
					return NULL;
				}
			}

		}
		cp = cp_close+1;		

		(*num_dims)++;
		if (*num_dims > buf_len) { /* grow buffer if necessary */
			buf_len = (*num_dims)+VECT_BUF_LEN;
			tmp_ir_ptr = (double_range_t *)realloc( ran_vect, buf_len*sizeof(double_range_t) );
			if (tmp_ir_ptr == NULL) {
				free( ran_vect );
				*num_dims = -1; /* realloc call failed */
				return NULL;
			}
			ran_vect = tmp_ir_ptr;
		}
		
		/* finally, add this double range struct to the array */
		(ran_vect+(*num_dims)-1)->min = current_ran.min;
		(ran_vect+(*num_dims)-1)->max = current_ran.max;
		(ran_vect+(*num_dims)-1)->special_min = current_ran.special_min;
		(ran_vect+(*num_dims)-1)->special_max = current_ran.special_max;
		(ran_vect+(*num_dims)-1)->repeat_count = current_ran.repeat_count;

	}

	/* free unused buffer space */
	if (buf_len > *num_dims) {
		tmp_ir_ptr = realloc( ran_vect, (*num_dims)*sizeof(double_range_t) );
		if (tmp_ir_ptr == NULL) {
			free( ran_vect );
			*num_dims = -1; /* memory error! */
			return NULL;
		}
		ran_vect = tmp_ir_ptr;
	}

	return ran_vect;
}


char *find_end_ranges_list( const char *s, const char *end_delim )
{
	char *cp;
	char *cp_ints, *cp_doubles, *cp_charcount, *cp_delim;
	
	if (s == NULL || end_delim == NULL)
		return NULL; /* ignore NULL string pointers */

	cp = strstr( s, "INTS" );
	cp_ints = strstr( s, "ints" );
	if (cp_ints == NULL
		|| (cp != NULL && cp < cp_ints)) {
		cp_ints = cp;
	}

	cp = strstr( s, "DOUBLES" );
	cp_doubles = strstr( s, "doubles" );
	if (cp_doubles == NULL
		|| (cp != NULL && cp < cp_doubles)) {
		cp_doubles = cp;
	}

	cp = strstr( s, "CHARCOUNT" );
	cp_charcount = strstr( s, "charcount" );
	if (cp_charcount == NULL
		|| (cp != NULL && cp < cp_charcount)) {
		cp_charcount = cp;
	}

	cp_delim = strstr( s, end_delim );

	/* find the earliest occurring pointer */
	cp = cp_ints;
	if (cp == NULL || (cp_doubles != NULL && cp_doubles < cp))
		cp = cp_doubles;
	if (cp == NULL || (cp_charcount != NULL && cp_charcount < cp))
		cp = cp_charcount;
	if (cp == NULL || (cp_delim != NULL && cp_delim < cp))
		cp = cp_delim;

	return cp;
}


char const *find_nonspace( const char *s )
{
	char const *cp = s;

	if (s == NULL)
		return NULL; /* ignore NULL string pointers */

	while (*cp != '\0' && *cp == ' ')
		cp++;

	if (*cp == '\0')
		return NULL;
	else
		return cp;
}


/*
 * Definitions for accessor methods:
 *
 */

int isEpisodic( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1;

	if (tspec->problem_type == TSPEC_EPISODIC)
		return 1;
	else
		return 0;
}

int isContinuing( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1;

	if (tspec->problem_type == TSPEC_CONTINUING)
		return 1;
	else
		return 0;
}

int isOtherType( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1;

	if (tspec->problem_type == TSPEC_OTHER)
		return 1;
	else
		return 0;
}

int_range_t getIntObs( taskspec_t *tspec, int index )
{
	int_range_t ir;
	int i;
	int ind_accumulator; /* for stepping through the observation
							ranges array */

	ir.repeat_count = 0;
	ir.min = ir.max = 0;
	ir.special_min = ir.special_max = RVAL_NOTSPECIAL;

	if (tspec == NULL || tspec->num_int_observations == 0 || index < 0)
		return ir; /* invalid request */

	ir.repeat_count = 1;
	
	ind_accumulator = 0;
	for (i = 0; i < tspec->num_int_observations; i++) {
		ind_accumulator += (tspec->int_observations+i)->repeat_count;
		if (index < ind_accumulator) {
			/* desired observation range found */
			ir.min = (tspec->int_observations+i)->min;
			ir.max = (tspec->int_observations+i)->max;
			ir.special_min = (tspec->int_observations+i)->special_min;
			ir.special_max = (tspec->int_observations+i)->special_max;

			return ir;
		}
	}

	/* given index is out of bounds; return empty range */
	return ir;
}

double_range_t getDoubleObs( taskspec_t *tspec, int index )
{
	double_range_t dr;
	int i;
	int ind_accumulator; /* for stepping through the observation
							ranges array */

	dr.repeat_count = 0;
	dr.min = dr.max = 0.;
	dr.special_min = dr.special_max = RVAL_NOTSPECIAL;

	if (tspec == NULL || tspec->num_double_observations == 0 || index < 0)
		return dr; /* invalid request */

	dr.repeat_count = 1;
	
	ind_accumulator = 0;
	for (i = 0; i < tspec->num_double_observations; i++) {
		ind_accumulator += (tspec->double_observations+i)->repeat_count;
		if (index < ind_accumulator) {
			/* desired observation range found */
			dr.min = (tspec->double_observations+i)->min;
			dr.max = (tspec->double_observations+i)->max;
			dr.special_min = (tspec->double_observations+i)->special_min;
			dr.special_max = (tspec->double_observations+i)->special_max;

			return dr;
		}
	}

	/* given index is out of bounds; return empty range */
	return dr;
}

int_range_t getIntAct( taskspec_t *tspec, int index )
{
	int_range_t ir;
	int i;
	int ind_accumulator; /* for stepping through the action ranges array */

	ir.repeat_count = 0;
	ir.min = ir.max = 0;
	ir.special_min = ir.special_max = RVAL_NOTSPECIAL;

	if (tspec == NULL || tspec->num_int_actions == 0 || index < 0)
		return ir; /* invalid request */

	ir.repeat_count = 1;
	
	ind_accumulator = 0;
	for (i = 0; i < tspec->num_int_actions; i++) {
		ind_accumulator += (tspec->int_actions+i)->repeat_count;
		if (index < ind_accumulator) {
			/* desired action range found */
			ir.min = (tspec->int_actions+i)->min;
			ir.max = (tspec->int_actions+i)->max;
			ir.special_min = (tspec->int_actions+i)->special_min;
			ir.special_max = (tspec->int_actions+i)->special_max;

			return ir;
		}
	}

	/* given index is out of bounds; return empty range */
	return ir;
}

double_range_t getDoubleAct( taskspec_t *tspec, int index )
{
	double_range_t dr;
	int i;
	int ind_accumulator; /* for stepping through the action ranges array */

	dr.repeat_count = 0;
	dr.min = dr.max = 0.;
	dr.special_min = dr.special_max = RVAL_NOTSPECIAL;

	if (tspec == NULL || tspec->num_double_actions == 0 || index < 0)
		return dr; /* invalid request */

	dr.repeat_count = 1;
	
	ind_accumulator = 0;
	for (i = 0; i < tspec->num_double_actions; i++) {
		ind_accumulator += (tspec->double_actions+i)->repeat_count;
		if (index < ind_accumulator) {
			/* desired action range found */
			dr.min = (tspec->double_actions+i)->min;
			dr.max = (tspec->double_actions+i)->max;
			dr.special_min = (tspec->double_actions+i)->special_min;
			dr.special_max = (tspec->double_actions+i)->special_max;

			return dr;
		}
	}

	/* given index is out of bounds; return empty range */
	return dr;
}

/* Observation space */
int getNumIntObs( taskspec_t *tspec )
{
	int i;
	int ind_accumulator;

	if (tspec == NULL)
		return -1;

	ind_accumulator = 0;
	for (i = 0; i < tspec->num_int_observations; i++)
		ind_accumulator += (tspec->int_observations+i)->repeat_count;

	return ind_accumulator;
}

int getIntObsMax( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	return ir.max;
}

int getIntObsMin( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	return ir.min;
}

int isIntObsMax_special( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	
	if (ir.special_max != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isIntObsMax_posInf( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	
	if (ir.special_max == RVAL_POSINF)
		return 1;
	else
		return 0;
}

int isIntObsMax_unspec( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	
	if (ir.special_max == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int isIntObsMin_special( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	
	if (ir.special_min != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isIntObsMin_negInf( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	
	if (ir.special_min == RVAL_NEGINF)
		return 1;
	else
		return 0;
}

int isIntObsMin_unspec( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_observations == 0
		|| index < 0 || index > getNumIntObs( tspec )-1)
		return -1; /* invalid request */

	ir = getIntObs( tspec, index );
	
	if (ir.special_min == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int getNumDoubleObs( taskspec_t *tspec )
{
	int i;
	int ind_accumulator;

	if (tspec == NULL)
		return -1;

	ind_accumulator = 0;
	for (i = 0; i < tspec->num_double_observations; i++)
		ind_accumulator += (tspec->double_observations+i)->repeat_count;

	return ind_accumulator;
}

double getDoubleObsMax( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );
	return dr.max;
}

double getDoubleObsMin( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );
	return dr.min;
}

int isDoubleObsMax_special( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );

	if (dr.special_max != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isDoubleObsMax_posInf( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );

	if (dr.special_max == RVAL_POSINF)
		return 1;
	else
		return 0;
}

int isDoubleObsMax_unspec( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );

	if (dr.special_max == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int isDoubleObsMin_special( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );

	if (dr.special_min != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isDoubleObsMin_negInf( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );

	if (dr.special_min == RVAL_NEGINF)
		return 1;
	else
		return 0;
}

int isDoubleObsMin_unspec( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_observations == 0
		|| index < 0 || index > getNumDoubleObs( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleObs( tspec, index );

	if (dr.special_min == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int getCharcountObs( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	return tspec->charcount_observations;
}

/* Action space */
int getNumIntAct( taskspec_t *tspec )
{
	int i;
	int ind_accumulator;

	if (tspec == NULL)
		return -1;

	ind_accumulator = 0;
	for (i = 0; i < tspec->num_int_actions; i++)
		ind_accumulator += (tspec->int_actions+i)->repeat_count;

	return ind_accumulator;
}

int getIntActMax( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );
	return ir.max;
}

int getIntActMin( taskspec_t *tspec, int index )
{
	int_range_t ir;
	
	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );
	return ir.min;
}

int isIntActMax_special( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );

	if (ir.special_max != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isIntActMax_posInf( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );

	if (ir.special_max == RVAL_POSINF)
		return 1;
	else
		return 0;
}

int isIntActMax_unspec( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );

	if (ir.special_max == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int isIntActMin_special( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );

	if (ir.special_min != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isIntActMin_negInf( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );

	if (ir.special_max == RVAL_NEGINF)
		return 1;
	else
		return 0;
}

int isIntActMin_unspec( taskspec_t *tspec, int index )
{
	int_range_t ir;

	if (tspec == NULL || tspec->num_int_actions == 0
		|| index < 0 || index > getNumIntAct( tspec )-1)
		return -1; /* invalid request */

	ir = getIntAct( tspec, index );

	if (ir.special_max == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int getNumDoubleAct( taskspec_t *tspec )
{
	int i;
	int ind_accumulator;

	if (tspec == NULL)
		return -1;

	ind_accumulator = 0;
	for (i = 0; i < tspec->num_double_actions; i++)
		ind_accumulator += (tspec->double_actions+i)->repeat_count;

	return ind_accumulator;
}

double getDoubleActMax( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	return dr.max;
}

double getDoubleActMin( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	return dr.min;
}

int isDoubleActMax_special( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	
	if (dr.special_max != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isDoubleActMax_posInf( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	
	if (dr.special_max == RVAL_POSINF)
		return 1;
	else
		return 0;
}

int isDoubleActMax_unspec( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	
	if (dr.special_max == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int isDoubleActMin_special( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	
	if (dr.special_min != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isDoubleActMin_negInf( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	
	if (dr.special_min == RVAL_NEGINF)
		return 1;
	else
		return 0;
}

int isDoubleActMin_unspec( taskspec_t *tspec, int index )
{
	double_range_t dr;

	if (tspec == NULL || tspec->num_double_actions == 0
		|| index < 0 || index > getNumDoubleAct( tspec )-1)
		return -1; /* invalid request */

	dr = getDoubleAct( tspec, index );
	
	if (dr.special_min == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int getCharcountAct( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	return tspec->charcount_actions;
}

/* Reward range */
double getRewardMax( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	return (tspec->reward).max;
}

double getRewardMin( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	return (tspec->reward).min;
}

int isRewardMax_special( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	if ((tspec->reward).special_max != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isRewardMax_posInf( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	if ((tspec->reward).special_max == RVAL_POSINF)
		return 1;
	else
		return 0;
}

int isRewardMax_unspec( taskspec_t *tspec ) 
{
	if (tspec == NULL)
		return -1; /* invalid request */

	if ((tspec->reward).special_max == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}

int isRewardMin_special( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	if ((tspec->reward).special_min != RVAL_NOTSPECIAL)
		return 1;
	else
		return 0;
}

int isRewardMin_negInf( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	if ((tspec->reward).special_min == RVAL_NEGINF)
		return 1;
	else
		return 0;
}

int isRewardMin_unspec( taskspec_t *tspec )
{
	if (tspec == NULL)
		return -1; /* invalid request */

	if ((tspec->reward).special_min == RVAL_UNSPEC)
		return 1;
	else
		return 0;
}


#ifdef __cplusplus
}
#endif
